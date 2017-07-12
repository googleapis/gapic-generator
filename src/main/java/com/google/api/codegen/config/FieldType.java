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

import static com.google.api.codegen.ApiModel.ModelType.DISCOVERY;
import static com.google.api.codegen.ApiModel.ModelType.PROTO;

import com.google.api.codegen.ApiModel.ModelType;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;

/** Created by andrealin on 6/22/17. */
public class FieldType {

  @Nullable Field protoBasedField;

  @Nullable Schema schemaField;

  public final ModelType modelType;

  public FieldType(Field protoBasedField) {
    Preconditions.checkNotNull(protoBasedField);
    this.protoBasedField = protoBasedField;
    modelType = PROTO;
  }

  public FieldType(Schema schemaField) {
    Preconditions.checkNotNull(schemaField);
    this.schemaField = schemaField;
    modelType = DISCOVERY;
  }

  public ModelType getModelType() {
    return modelType;
  }

  public String getSimpleName() {
    switch (modelType) {
      case PROTO:
        return protoBasedField.getSimpleName();
      case DISCOVERY:
        return schemaField.id().isEmpty() ? schemaField.key() : schemaField.id();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public Field getProtoBasedField() {
    return protoBasedField;
  }

  public Schema getSchemaField() {
    return schemaField;
  }

  public String getFullName() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.getFullName();
      case DISCOVERY:
        return schemaField.getIdentifier();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public boolean isMap() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.getType().isMap();
      case DISCOVERY:
        return false;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public FieldType getMapKeyField() {
    switch (getModelType()) {
      case PROTO:
        return new FieldType(protoBasedField.getType().getMapKeyField());
      case DISCOVERY:
        return null;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public FieldType getMapValueField() {
    switch (getModelType()) {
      case PROTO:
        return new FieldType(protoBasedField.getType().getMapValueField());
      case DISCOVERY:
        return null;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public boolean isMessage() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.getType().isMessage();
      case DISCOVERY:
        return false;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public boolean isRepeated() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.isRepeated();
      case DISCOVERY:
        return schemaField.type() == Type.ARRAY;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public String getParentFullName() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.getParent().getFullName();
      case DISCOVERY:
        return schemaField.getIdentifier();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public Cardinality getCardinality() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.getType().getCardinality();
      case DISCOVERY:
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public boolean isEnum() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.getType().isEnum();
      case DISCOVERY:
        return schemaField.isEnum();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  public boolean isPrimitive() {
    switch (getModelType()) {
      case PROTO:
        return protoBasedField.getType().isPrimitive();
      case DISCOVERY:
        return schemaField.reference().isEmpty() && schemaField.items() == null;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  @Override
  public boolean equals(Object o) {
    if (o == null) {
      return false;
    }
    if (!(o instanceof FieldType)) {
      return false;
    }

    FieldType other = (FieldType) o;
    if (!this.getModelType().equals(other.getModelType())) {
      return false;
    }

    switch (this.getModelType()) {
      case DISCOVERY:
        return this.getSchemaField().equals(other.getSchemaField());
      case PROTO:
        return this.getProtoBasedField().equals(other.getProtoBasedField());
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  @Override
  public String toString() {
    String fieldString = "";
    switch (modelType) {
      case DISCOVERY:
        fieldString = getSchemaField().toString();
        break;
      case PROTO:
        fieldString = getProtoBasedField().toString();
        break;
    }
    return String.format("FieldType (%s): {%s}", modelType, fieldString);
  }

  public boolean isEmpty() {
    return protoBasedField == null && schemaField == null;
  }
}
