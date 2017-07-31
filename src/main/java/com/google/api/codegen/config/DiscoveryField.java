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
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.List;

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
  public Schema getDiscoveryField() {
    return schema;
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
  public boolean isMap() {
    return false;
  }

  @Override
  public FieldType getMapKeyField() {
    throw new IllegalArgumentException("Discovery model types have no map keys.");
  }

  @Override
  public FieldType getMapValueField() {
    throw new IllegalArgumentException("Discovery model types have no map values.");
  }

  @Override
  public boolean isMessage() {
    return false;
  }

  @Override
  public boolean isRepeated() {
    return schema.type() == Type.ARRAY;
  }

  @Override
  public String getParentFullName() {
    return schema.parent().id();
  }

  @Override
  public Cardinality getCardinality() {
    throw new IllegalArgumentException("Discovery model types have no defined Cardinality.");
  }

  @Override
  public boolean isEnum() {
    return schema.isEnum();
  }

  @Override
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
  public boolean equals(Object o) {
    return o != null
        && o instanceof DiscoveryField
        && ((DiscoveryField) o).schema.equals(this.schema);
  }

  @Override
  /* @Get the description of the element scoped to the visibility as currently set in the model. */
  public String getScopedDocumentation() {
    return "Not yet implemented.";
  }

  @Override
  public List<String> getOneofFieldsNames() {
    return ImmutableList.of();
  }
}
