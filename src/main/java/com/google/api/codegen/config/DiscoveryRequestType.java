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

import com.google.auto.value.AutoValue;
import java.util.List;

/** A type declaration wrapper around a Discovery request message type. */
@AutoValue
public abstract class DiscoveryRequestType implements TypeModel {

  public static DiscoveryRequestType create(DiscoveryMethodModel method) {
    return newBuilder().typeName("message").parentMethod(method).build();
  }

  // The method for which this object is the input request type.
  public abstract DiscoveryMethodModel parentMethod();

  /* @return if the underlying resource is a map type. */
  @Override
  public boolean isMap() {
    return false;
  }

  /* @return the resource type of the map key. */
  @Override
  public FieldModel getMapKeyField() {
    return null;
  }

  /* @return the resource type of the map value. */
  @Override
  public FieldModel getMapValueField() {
    return null;
  }

  /* @return if the underlying resource is a message. */
  @Override
  public boolean isMessage() {
    return true;
  }

  /* @return if the underlying resource can be repeated in the parent resource. */
  @Override
  public boolean isRepeated() {
    return false;
  }

  /* @return if this resource is an enum. */
  @Override
  public boolean isEnum() {
    return false;
  }

  /* @return if this is a primitive type. */
  @Override
  public boolean isPrimitive() {
    return false;
  }

  /* @return if this is the empty type. */
  @Override
  public abstract boolean isEmptyType();

  @Override
  public void validateValue(String value) {}

  @Override
  public List<DiscoveryField> getFields() {
    return parentMethod().getInputFields();
  }

  @Override
  public DiscoveryField getField(String key) {
    for (DiscoveryField field : getFields()) {
      // Just check each field's name without recursively searching inside each field.
      if (field.getSimpleName().equals(key)) {
        return field;
      }
    }
    return null;
  }

  @Override
  public TypeModel makeOptional() {
    return this;
  }

  @Override
  public String getPrimitiveTypeName() {
    return null;
  }

  @Override
  public boolean isBooleanType() {
    return false;
  }

  @Override
  public boolean isStringType() {
    return false;
  }

  @Override
  public boolean isFloatType() {
    return false;
  }

  @Override
  public boolean isBytesType() {
    return false;
  }

  @Override
  public boolean isDoubleType() {
    return false;
  }

  @Override
  public String getTypeName() {
    return typeName();
  }

  abstract String typeName();

  public static Builder newBuilder() {
    return new AutoValue_DiscoveryRequestType.Builder().isEmptyType(false);
  }

  @Override
  public OneofConfig getOneOfConfig(String fieldName) {
    return null;
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder isEmptyType(boolean isEmpty);

    public abstract Builder typeName(String typeName);

    public abstract Builder parentMethod(DiscoveryMethodModel method);

    public abstract DiscoveryRequestType build();
  }
}
