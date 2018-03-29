/* Copyright 2017 Google LLC
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
package com.google.api.codegen.config;

import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES;
import static com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING;

import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.ModelTypeNameConverter;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.Nullable;

/** A field declaration wrapper around a protobuf Field. */
public class ProtoField implements FieldModel {
  private final Field protoField;
  private final ProtoTypeRef protoTypeRef;

  /* Create a FieldModel object from a non-null Field object. */
  public ProtoField(Field protoField) {
    Preconditions.checkNotNull(protoField);
    this.protoField = protoField;
    this.protoTypeRef = new ProtoTypeRef(protoField.getType());
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
  public Name getNameAsParameterName() {
    return Name.from(getSimpleName());
  }

  @Override
  public String getNameAsParameter() {
    return getNameAsParameterName().toLowerUnderscore();
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
  public boolean isMessage() {
    return protoField.getType().isMessage();
  }

  @Override
  public boolean isRequired() {
    return protoField.getType().getCardinality().equals(Cardinality.REQUIRED);
  }

  @Override
  public boolean isRepeated() {
    return protoField.isRepeated();
  }

  @Override
  public boolean mayBeInResourceName() {
    return true;
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

  @Override
  public List<String> getPagedResponseResourceMethods(
      FeatureConfig featureConfig, FieldConfig startingFieldConfig, SurfaceNamer namer) {
    String resourceFieldGetFunctionName =
        namer.getFieldGetFunctionName(featureConfig, startingFieldConfig);
    return ImmutableList.of(resourceFieldGetFunctionName);
  }

  @Override
  public ProtoTypeRef getType() {
    return protoTypeRef;
  }

  @Override
  public int hashCode() {
    return 5 + 31 * protoField.hashCode();
  }

  @Override
  public String toString() {
    return String.format("Protobuf FieldModel: {%s}", protoField.toString());
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof ProtoField
        && ((ProtoField) o).protoField.equals(this.protoField);
  }
}
