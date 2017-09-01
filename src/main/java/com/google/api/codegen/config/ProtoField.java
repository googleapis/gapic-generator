/* Copyright 2017 Google Inc
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
package com.google.api.codegen.config;

import static com.google.api.codegen.config.ApiSource.PROTO;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;

import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import javax.annotation.Nullable;

/** Created by andrealin on 7/31/17. */
public class ProtoField implements FieldModel {
  private final Field protoField;

  @Override
  /* @return the type of the underlying model resource. */
  public ApiSource getApiSource() {
    return PROTO;
  }

  /* Create a FieldModel object from a non-null Field object. */
  public ProtoField(Field protoField) {
    Preconditions.checkNotNull(protoField);
    this.protoField = protoField;
  }

  @Override
  public String getSimpleName() {
    return protoField.getSimpleName();
  }

  @Override
  public String getFullName() {
    return protoField.getFullName();
  }

  @Override
  public Name asName() {
    return Name.from(protoField.getSimpleName());
  }

  @Override
  public String getTypeFullName() {
    return protoField.getType().getMessageType().getFullName();
  }

  @Override
  public boolean isMap() {
    return protoField.getType().isMap();
  }

  @Override
  public ProtoField getMapKeyField() {
    return new ProtoField(protoField.getType().getMapKeyField());
  }

  @Override
  public ProtoField getMapValueField() {
    return new ProtoField(protoField.getType().getMapValueField());
  }

  @Override
  public boolean isMessage() {
    return protoField.getType().isMessage();
  }

  @Override
  public boolean isRequired() {
    // TODO(andrealin): implement.
    return false;
  }

  @Override
  public boolean isRepeated() {
    return protoField.isRepeated();
  }

  @Override
  public String getParentFullName() {
    return protoField.getParent().getFullName();
  }

  @Override
  public String getParentSimpleName() {
    return protoField.getParent().getSimpleName();
  }

  @Override
  public TypeName getParentTypeName(ImportTypeTable typeTable) {
    return ((ModelTypeNameConverter) typeTable.getTypeNameConverter())
        .getTypeName(protoField.getParent());
  }

  @Override
  public Cardinality getCardinality() {
    return protoField.getType().getCardinality();
  }

  @Override
  public boolean isEnum() {
    return protoField.getType().isEnum();
  }

  @Override
  public boolean isPrimitive() {
    return protoField.getType().isPrimitive();
  }

  @Override
  public String toString() {
    return String.format("Protobuf FieldModel (%s): {%s}", getApiSource(), protoField.toString());
  }

  @Override
  public TypeRef getProtoTypeRef() {
    return protoField.getType();
  }

  @Override
  public Schema getDiscoveryField() {
    throw new IllegalArgumentException("Protobuf model types have no Discovery Field types.");
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof ProtoField
        && ((ProtoField) o).protoField.equals(this.protoField);
  }

  @Override
  public String getScopedDocumentation() {
    return DocumentationUtil.getScopedDescription(protoField);
  }

  public static Iterable<Iterable<String>> getOneofFieldsNames(
      Iterable<FieldModel> fields, SurfaceNamer namer) {
    ImmutableSet.Builder<Oneof> oneOfsBuilder = ImmutableSet.builder();
    for (FieldModel field : fields) {
      Oneof oneof = ((ProtoField) field).protoField.getOneof();
      if (oneof == null) {
        continue;
      }
      oneOfsBuilder.add(oneof);
    }

    Iterable<Oneof> oneOfs = oneOfsBuilder.build();

    ImmutableList.Builder<Iterable<String>> fieldsNames = ImmutableList.builder();

    for (Oneof oneof : oneOfs) {
      boolean hasItems = false;
      ImmutableSet.Builder<String> fieldNames = ImmutableSet.builder();
      for (Field field : oneof.getFields()) {
        fieldNames.add(namer.getVariableName(new ProtoField(field)));
        hasItems = true;
      }
      if (hasItems) {
        fieldsNames.add(fieldNames.build());
      }
    }
    return fieldsNames.build();
  }

  @Override
  public Iterable<String> getOneofFieldsNames(SurfaceNamer surfaceNamer) {
    if (protoField.getOneof() != null) {
      ImmutableList.Builder<String> fieldNames = ImmutableList.builder();
      for (Field field : protoField.getOneof().getFields()) {
        fieldNames.add(surfaceNamer.getVariableName(new ProtoField(field)));
      }
      return fieldNames.build();
    }
    return null;
  }

  @Override
  public boolean isString() {
    return protoField.getType().equals(TYPE_STRING);
  }

  @Override
  public boolean isBytes() {
    return protoField.getType().equals(TYPE_BYTES);
  }

  @Override
  public String getKind() {
    return protoField.getType().toString();
  }

  @Nullable
  @Override
  public Oneof getOneof() {
    return protoField.getOneof();
  }
}
