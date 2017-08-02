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
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** Created by andrealin on 7/31/17. */
public class ProtoField implements FieldType {
  private final Field protoField;

  @Override
  /* @return the type of the underlying model resource. */
  public ApiSource getApiSource() {
    return PROTO;
  }

  /* Create a FieldType object from a non-null Field object. */
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
  public boolean isRepeated() {
    return protoField.isRepeated();
  }

  @Override
  public String getParentFullName() {
    return protoField.getParent().getFullName();
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
    return String.format("Protobuf FieldType (%s): {%s}", getApiSource(), protoField.toString());
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

  @Override
  public List<String> getOneofFieldsNames() {
    if (protoField.getOneof() != null) {
      ImmutableList.Builder<String> fieldNames = ImmutableList.builder();
      for (Field field : protoField.getOneof().getFields()) {
        fieldNames.add(field.getSimpleName());
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
}
