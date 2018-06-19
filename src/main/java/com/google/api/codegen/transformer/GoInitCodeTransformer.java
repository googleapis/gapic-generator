/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.OneofConfig;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.transformer.go.GoModelTypeNameConverter;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Preconditions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class GoInitCodeTransformer {

  ModelTypeTable typeTable = new ModelTypeTable(new GoTypeTable(), new GoModelTypeNameConverter());

  public String generateInitCode(MethodContext methodContext, InitCodeContext initCodeContext) {
    try {
      MessageInitWriter writer =
          new MessageInitWriter(methodContext.getMethodModel().getInputType());
      for (String spec : initCodeContext.initFieldConfigStrings()) {
        writer.addInit(spec);
      }
      StringBuilder sb = new StringBuilder();
      writer.writeInit(methodContext.getNamer(), sb);
      return sb.toString();
    } catch (Exception e) {
      return "failed to generate init code: " + e.getMessage();
    }
  }

  private interface InitWriter {
    void addInit(String spec);

    void writeInit(SurfaceNamer namer, StringBuilder sb);
  }

  private class MessageInitWriter implements InitWriter {
    private final TypeModel type;
    private final LinkedHashMap<FieldModel, InitWriter> fields = new LinkedHashMap<>();

    MessageInitWriter(TypeModel type) {
      this.type = Preconditions.checkNotNull(type);
    }

    @Override
    public void addInit(String spec) {
      if (spec.isEmpty()) {
        return;
      }
      if (spec.startsWith(".")) {
        spec = spec.substring(1);
      }
      int p = spec.length();
      for (int i = 0; i < spec.length(); i++) {
        char c = spec.charAt(i);
        if (!Character.isLetterOrDigit(c) && c != '_') {
          p = i;
          break;
        }
      }
      Preconditions.checkArgument(p > 0, "empty field name not allowed: %s", spec);
      FieldModel field = type.getField(spec.substring(0, p));
      InitWriter next = fields.computeIfAbsent(field, f -> newWriter(f.getType()));
      next.addInit(spec.substring(p));
    }

    @Override
    public void writeInit(SurfaceNamer namer, StringBuilder sb) {
      // The type name is "*T", but we need "&T" to init.
      sb.append('&').append(typeTable.getAndSaveNicknameFor(type).substring(1)).append("{");
      if (!fields.isEmpty()) {
        sb.append('\n');
      }
      for (Map.Entry<FieldModel, InitWriter> entry : fields.entrySet()) {
        FieldModel field = entry.getKey();
        OneofConfig oneof = type.getOneOfConfig(field.getSimpleName());
        if (oneof != null) {
          sb.append(namer.publicFieldName(oneof.groupName()))
              .append(": &")
              .append(namer.getOneofVariantTypeName(oneof))
              .append("{\n");
        }
        sb.append(namer.getFieldGetFunctionName(field)).append(": ");
        entry.getValue().writeInit(namer, sb);
        if (oneof != null) {
          sb.append("\n}");
        }
        sb.append(",\n");
      }
      sb.append("}");
    }
  }

  private class ValueInitWriter implements InitWriter {
    private final TypeModel type;
    private String value;

    ValueInitWriter(TypeModel type) {
      this.type = Preconditions.checkNotNull(type);
      if (type.isMessage() || type.isRepeated()) {
        value = "nil";
      } else if (type.isStringType() || type.isBytesType()) {
        value = "";
      } else {
        value = "0";
      }
    }

    @Override
    public void addInit(String spec) {
      if (spec.isEmpty()) {
        return;
      }
      Preconditions.checkArgument(spec.startsWith("="), "expected '=value': %s", spec);
      spec = spec.substring(1);

      // GAPIC YAML seems inconsistent about whether strings should be quoted.
      // Just normalize and never quote.
      if (spec.startsWith("\"")) {
        spec = spec.substring(1, spec.length() - 1);
      }
      value = spec;
    }

    @Override
    public void writeInit(SurfaceNamer namer, StringBuilder sb) {
      if (type.isStringType()) {
        sb.append('"').append(value).append('"');
      } else if (type.isBytesType()) {
        sb.append("[]byte(\"").append(value).append("\")");
      } else {
        sb.append(value);
      }
    }
  }

  private class EnumInitWriter implements InitWriter {
    private final TypeRef type;
    private String value;

    EnumInitWriter(TypeModel type) {
      if (type instanceof ProtoTypeRef) {
        this.type = ((ProtoTypeRef) type).getProtoType();
      } else {
        throw new IllegalArgumentException("can't get enum type: " + type);
      }
      value = this.type.getEnumType().getValues().get(0).getSimpleName();
    }

    @Override
    public void addInit(String spec) {
      if (spec.isEmpty()) {
        return;
      }
      Preconditions.checkArgument(spec.startsWith("="), "expected '=value': %s", spec);
      value = spec.substring(1);
    }

    @Override
    public void writeInit(SurfaceNamer namer, StringBuilder sb) {
      sb.append(typeTable.getEnumValue(type, value));
    }
  }

  private class ArrayInitWriter implements InitWriter {
    private final TypeModel type;
    private final TypeModel innerType;
    private final TreeMap<Integer, InitWriter> elements = new TreeMap<>();

    ArrayInitWriter(TypeModel type) {
      this.type = Preconditions.checkNotNull(type);
      if (type instanceof ProtoTypeRef) {
        ProtoTypeRef typeRef = (ProtoTypeRef) type;
        this.innerType = new ProtoTypeRef(typeRef.getProtoType().makeOptional());
      } else {
        throw new IllegalArgumentException("can't un-repeat type: " + type);
      }
    }

    @Override
    public void addInit(String spec) {
      if (spec.isEmpty()) {
        return;
      }
      Preconditions.checkArgument(spec.startsWith("["), "expected '[N]': %s", spec);

      int p = spec.indexOf("]");
      Preconditions.checkArgument(p > 0, "array index not closed: %s", spec);

      Integer index = Integer.parseInt(spec.substring(1, p));
      InitWriter inner = elements.computeIfAbsent(index, i -> newWriter(innerType));
      inner.addInit(spec.substring(p + 1));
    }

    @Override
    public void writeInit(SurfaceNamer namer, StringBuilder sb) {
      boolean needIndex = (elements.lastKey() != elements.size() - 1);

      sb.append(typeTable.getAndSaveNicknameFor(type)).append("{\n");
      for (Map.Entry<Integer, InitWriter> element : elements.entrySet()) {
        if (needIndex) {
          sb.append(element.getKey()).append(": ");
        }
        element.getValue().writeInit(namer, sb);
        sb.append(",\n");
      }
      sb.append("}");
    }
  }

  private InitWriter newWriter(TypeModel type) {
    if (type.isRepeated()) {
      return new ArrayInitWriter(type);
    }
    if (type.isMessage()) {
      return new MessageInitWriter(type);
    }
    if (type.isPrimitive()) {
      return new ValueInitWriter(type);
    }
    if (type.isEnum()) {
      return new EnumInitWriter(type);
    }
    throw new UnsupportedOperationException(String.format("field type not supported: %s", type));
  }
}
