/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GoModelTypeNameConverter extends ModelTypeNameConverter {

  /** The import path for generated pb.go files for core-proto files. */
  private static final String CORE_PROTO_BASE = "google.golang.org/genproto/";

  private static final String CORE_PROTO_PATH = CORE_PROTO_BASE + "googleapis/";
  private static final String GOOGLE_PREFIX = "google.";

  private static final ImmutableMap<Type, String> PRIMITIVE_TYPE_MAP =
      ImmutableMap.<Type, String>builder()
          .put(Type.TYPE_BOOL, "bool")
          .put(Type.TYPE_DOUBLE, "float64")
          .put(Type.TYPE_FLOAT, "float32")
          .put(Type.TYPE_INT64, "int64")
          .put(Type.TYPE_UINT64, "uint64")
          .put(Type.TYPE_SINT64, "int64")
          .put(Type.TYPE_FIXED64, "int64")
          .put(Type.TYPE_SFIXED64, "int64")
          .put(Type.TYPE_INT32, "int32")
          .put(Type.TYPE_UINT32, "uint32")
          .put(Type.TYPE_SINT32, "int32")
          .put(Type.TYPE_FIXED32, "int32")
          .put(Type.TYPE_SFIXED32, "int32")
          .put(Type.TYPE_STRING, "string")
          .put(Type.TYPE_BYTES, "[]byte")
          .build();

  @Override
  public TypeName getTypeNameInImplicitPackage(String shortName) {
    throw new UnsupportedOperationException("getTypeName(String) not supported by Go");
  }

  @Override
  public TypeName getTypeName(TypeRef type) {
    if (type.isMap()) {
      TypeName keyTypeName = getTypeNameForElementType(type.getMapKeyField().getType());
      TypeName valueTypeName = getTypeNameForElementType(type.getMapValueField().getType());
      return new TypeName("", "", "map[%i]%i", keyTypeName, valueTypeName);
    } else if (type.isRepeated()) {
      TypeName elementTypeName = getTypeNameForElementType(type);
      return new TypeName("", "", "[]%i", elementTypeName);
    } else {
      return getTypeNameForElementType(type);
    }
  }

  @Override
  public TypeName getTypeNameForElementType(TypeRef type) {
    String primitiveTypeName = PRIMITIVE_TYPE_MAP.get(type.getKind());
    if (primitiveTypeName != null) {
      return new TypeName(primitiveTypeName);
    }
    switch (type.getKind()) {
      case TYPE_MESSAGE:
        return getTypeName(type.getMessageType());
      case TYPE_ENUM:
        return getTypeName(type.getEnumType());
      default:
        throw new IllegalArgumentException("unknown type kind: " + type.getKind());
    }
  }

  /**
   * Since Go imports by package instead of by class name, we have to treat it differently than
   * other languages. Imports in Go have 4 components: - Import path, like
   * "github.com/googleapis/gax-go" - Local package name, like "gax"; this is the name we call the
   * package we import - The name of the thing in the package we want, like "CallOption" - If it's a
   * pointer, we need "*"
   *
   * <p>We need all 4 pieces in the full name and the last 3 in the nickname. For nickname, we
   * simply use: "gax.CallOption", which is how we refer to the import in the program text. For the
   * full name, we use join all 4 by semicolons: "github.com/googleapis/gax-go;gax;CallOption;"
   */
  @Override
  public TypeName getTypeName(ProtoElement elem) {
    return getTypeName(elem, elem instanceof MessageType);
  }

  TypeName getTypeName(ProtoElement elem, boolean isPointer) {
    String importPath = elem.getFile().getProto().getOptions().getGoPackage();
    String elemName = getElemName(elem);
    return getTypeName(importPath, elemName, isPointer);
  }

  private String getElemName(ProtoElement elem) {
    // Fast path if we have a top-level message
    if (elem.getParent() instanceof ProtoFile) {
      return elem.getSimpleName();
    }

    // The number of components in the same with the layers of nesting.
    // Init the list to something a little more than sensible.
    List<String> nameComponents = new ArrayList<>(5);
    while (!(elem instanceof ProtoFile)) {
      nameComponents.add(elem.getSimpleName());
      elem = elem.getParent();
    }
    Collections.reverse(nameComponents);
    return Joiner.on("_").join(nameComponents);
  }

  @VisibleForTesting
  TypeName getTypeName(String importPath, String elemName, boolean isPointer) {
    // There are two ways the import path can be formatted:
    // - "path/to/pkg"
    // - "path/to/pkg;pkgName"
    String localName = null;
    if (importPath.lastIndexOf(';') >= 0) {
      int semicolonPos = importPath.lastIndexOf(';');
      localName = importPath.substring(semicolonPos + 1) + "pb";
      importPath = importPath.substring(0, semicolonPos);
    } else {
      // The import path might be versioned:
      //   google.golang.org/genproto/googleapis/example/library/v1
      // or not:
      //   google.golang.org/genproto/googleapis/api/monitoredres
      // We heuristically get the import name by looking for the right-most element
      // that is not a version number.
      List<String> parts = Arrays.asList(importPath.split("/"));
      Collections.reverse(parts);
      for (String part : parts) {
        if (part.length() < 2 || part.charAt(0) != 'v' || !Character.isDigit(part.charAt(1))) {
          localName = part + "pb";
          break;
        }
      }
      if (localName == null) {
        throw new IllegalArgumentException("cannot find a suitable import name: " + importPath);
      }
    }

    String pointerPrefix = isPointer ? "*" : "";
    return new TypeName(
        Joiner.on(";").join(importPath, localName, elemName, pointerPrefix),
        pointerPrefix + localName + "." + elemName);
  }

  @Override
  public String renderPrimitiveValue(TypeRef type, String value) {
    Type primitiveType = type.getKind();
    if (!PRIMITIVE_TYPE_MAP.containsKey(primitiveType)) {
      throw new IllegalArgumentException(
          "Initial values are only supported for primitive types, got type "
              + type
              + ", with value "
              + value);
    }
    switch (primitiveType) {
      case TYPE_STRING:
        return "\"" + value + "\"";
      case TYPE_BYTES:
        return "[]byte(\"" + value + "\")";
      default:
        // Types that do not need to be modified (e.g. TYPE_INT32) are handled here
        return value;
    }
  }

  @Override
  public TypedValue getSnippetZeroValue(TypeRef type) {
    if (type.isRepeated() || type.isMap()) {
      return TypedValue.create(getTypeName(type), "nil");
    }
    if (type.isEnum()) {
      return getEnumValue(type, type.getEnumType().getValues().get(0));
    }
    if (type.isMessage()) {
      return TypedValue.create(
          getTypeName(type), "&" + getTypeName(type.getMessageType(), false).getNickname() + "{}");
    }
    switch (type.getKind()) {
      case TYPE_BOOL:
        return TypedValue.create(getTypeName(type), "false");

      case TYPE_STRING:
        return TypedValue.create(getTypeName(type), "\"\"");

      case TYPE_BYTES:
        return TypedValue.create(getTypeName(type), "nil");

      default:
        // Anything else -- numeric values.
        return TypedValue.create(getTypeName(type), "0");
    }
  }

  @Override
  public TypedValue getImplZeroValue(TypeRef type) {
    return getSnippetZeroValue(type);
  }

  @Override
  public TypeName getTypeNameForTypedResourceName(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException("getTypeNameForTypedResourceName not supported by Go");
  }

  @Override
  public TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName) {
    throw new UnsupportedOperationException(
        "getTypeNameForResourceNameElementType not supported by Go");
  }

  @Override
  public TypedValue getEnumValue(TypeRef type, EnumValue value) {
    // Go names enums in two different ways.
    // If the enum is nested in a message, the format is <message type>_<enum value>,
    // respecting the C++ scoping rule used by protobuf,
    // where enum values are scoped at the same level as the enum type, not as its children.
    // On the other hand, if the enum is at top-level, there is no parent message,
    // and the format is <enum type>_<enum value>
    ProtoElement parent = type.getEnumType().getParent();
    if (parent instanceof ProtoFile) {
      parent = type.getEnumType();
    }
    return TypedValue.create(getTypeName(parent, false), "%s_" + value.getSimpleName());
  }
}
