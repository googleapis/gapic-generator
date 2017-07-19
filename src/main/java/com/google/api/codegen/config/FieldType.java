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
import static com.google.api.codegen.config.FieldType.ApiSource.PROTO;

import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import com.google.common.base.Preconditions;
import javax.annotation.Nullable;

/**
 * Wrapper class around the protobuf Field class and the Discovery-doc Schema class.
 *
 * <p>Each instance of this class contains exactly one of {Field, Schema}. This class abstracts the
 * format (protobuf, discovery, etc) of the source from a resource type definition.
 */
public class FieldType {

  public enum ApiSource {
    DISCOVERY,
    PROTO;
  }

  @Nullable Field protoBasedField;

  @Nullable Schema schemaField;

  public final ApiSource apiSource;

  /* Create a FieldType object from a non-null Field object. */
  public FieldType(Field protoBasedField) {
    Preconditions.checkNotNull(protoBasedField);
    this.protoBasedField = protoBasedField;
    apiSource = PROTO;
  }

  /* Create a FieldType object from a non-null Schema object. */
  public FieldType(Schema schemaField) {
    Preconditions.checkNotNull(schemaField);
    this.schemaField = schemaField;
    apiSource = DISCOVERY;
  }

  /* @return the type of the underlying model resource. */
  public ApiSource getApiSource() {
    return apiSource;
  }

  public String getSimpleName() {
    switch (apiSource) {
      case PROTO:
        return protoBasedField.getSimpleName();
      case DISCOVERY:
        return schemaField.getIdentifier();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  @Nullable
  /* Return the underlying proto-based field, or null if none. */
  public Field getProtoBasedField() {
    return protoBasedField;
  }

  @Nullable
  /* Return the underlying Discovery-Doc-based schema, or null if none. */
  public Schema getSchemaField() {
    return schemaField;
  }

  public String getFullName() {
    switch (getApiSource()) {
      case PROTO:
        return protoBasedField.getFullName();
      case DISCOVERY:
        return schemaField.getIdentifier();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return if the underlying resource is a map type. */
  public boolean isMap() {
    switch (getApiSource()) {
      case PROTO:
        return protoBasedField.getType().isMap();
      case DISCOVERY:
        return false;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return the resource type of the map key. */
  public FieldType getMapKeyField() {
    switch (getApiSource()) {
      case PROTO:
        return new FieldType(protoBasedField.getType().getMapKeyField());
      case DISCOVERY:
        return null;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return the resource type of the map value. */
  public FieldType getMapValueField() {
    switch (getApiSource()) {
      case PROTO:
        return new FieldType(protoBasedField.getType().getMapValueField());
      case DISCOVERY:
        return null;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return if the underlying resource is a proto Messsage. */
  public boolean isMessage() {
    switch (getApiSource()) {
      case PROTO:
        return protoBasedField.getType().isMessage();
      case DISCOVERY:
        return false;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return if the underlying resource can be repeated in the parent resource. */
  public boolean isRepeated() {
    switch (getApiSource()) {
      case PROTO:
        return protoBasedField.isRepeated();
      case DISCOVERY:
        return schemaField.type() == Type.ARRAY;
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return the full name of the parent. */
  public String getParentFullName() {
    switch (getApiSource()) {
      case PROTO:
        return protoBasedField.getParent().getFullName();
      case DISCOVERY:
        return schemaField.parent().id();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return the cardinality of the resource. */
  public Cardinality getCardinality() {
    switch (getApiSource()) {
      case PROTO:
        return protoBasedField.getType().getCardinality();
      case DISCOVERY:
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return if this resource is an enum. */
  public boolean isEnum() {
    switch (getApiSource()) {
      case PROTO:
        return protoBasedField.getType().isEnum();
      case DISCOVERY:
        return schemaField.isEnum();
      default:
        throw new IllegalArgumentException("Unhandled model type.");
    }
  }

  /* @return if this is a primitive type. */
  public boolean isPrimitive() {
    switch (getApiSource()) {
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
    if (!this.getApiSource().equals(other.getApiSource())) {
      return false;
    }

    switch (this.getApiSource()) {
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
    switch (apiSource) {
      case DISCOVERY:
        fieldString = getSchemaField().toString();
        break;
      case PROTO:
        fieldString = getProtoBasedField().toString();
        break;
    }
    return String.format("FieldType (%s): {%s}", apiSource, fieldString);
  }

  public boolean isEmpty() {
    return protoBasedField == null && schemaField == null;
  }
}
