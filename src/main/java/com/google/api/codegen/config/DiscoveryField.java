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

import static com.google.api.codegen.config.FieldType.ApiSource.DISCOVERY;

import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;

/** Created by andrealin on 7/31/17. */
public class DiscoveryField implements FieldType {
  private final Schema schema;
  private final ApiSource apiSource = DISCOVERY;

  /* Create a FieldType object from a non-null Schema object. */
  public DiscoveryField(Schema schema) {
    Preconditions.checkNotNull(schema);
    this.schema = schema;
  }

  @Override
  /* @return the type of the underlying model resource. */
  public ApiSource getApiSource() {
    return apiSource;
  }

  @Override
  public String getSimpleName() {
    return schema.getIdentifier();
  }

  @Override
  public String getFullName() {
    return schema.getIdentifier();
  }

  @Override
  /* @return if the underlying resource is a map type. */
  public boolean isMap() {
    return false;
  }

  @Override
  /* @return the resource type of the map key. */
  public FieldType getMapKeyField() {
    throw new IllegalArgumentException("Discovery model types have no map keys.");
  }

  @Override
  /* @return the resource type of the map value. */
  public FieldType getMapValueField() {
    throw new IllegalArgumentException("Discovery model types have no map values.");
  }

  @Override
  /* @return if the underlying resource is a proto Messsage. */
  public boolean isMessage() {
    return false;
  }

  @Override
  /* @return if the underlying resource can be repeated in the parent resource. */
  public boolean isRepeated() {
    return schema.type() == Type.ARRAY;
  }

  @Override
  /* @return the full name of the parent. */
  public String getParentFullName() {
    return schema.parent().id();
  }

  @Override
  /* @return the cardinality of the resource. */
  public Cardinality getCardinality() {
    throw new IllegalArgumentException("Discovery model types have no defined Cardinality.");
  }

  @Override
  /* @return if this resource is an enum. */
  public boolean isEnum() {
    return schema.isEnum();
  }

  @Override
  /* @return if this is a primitive type. */
  public boolean isPrimitive() {
    return schema.reference().isEmpty() && schema.items() == null;
  }

  @Override
  public String toString() {
    return String.format("Discovery FieldType (%s): {%s}", apiSource, schema.toString());
  }

  @Override
  public TypeRef getProtoTypeRef() {
    throw new IllegalArgumentException("Discovery model types have no TypeRefs.");
  }

  @Override
  public Field getProtoField() {
    throw new IllegalArgumentException("Discovery model types have no protobuf Field types.");
  }

  @Override
  public Schema getDiscoveryField() {
    return schema;
  }

  @Override
  public Oneof getProtoOneof() {
    throw new IllegalArgumentException("Discovery model types have no protobuf Oneofs.");
  }

  @Override
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoveryField
        && ((DiscoveryField) o).schema.equals(this.schema);
  }
}
