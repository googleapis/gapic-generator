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

import java.util.List;

/**
 * Wrapper class around the protobuf TypeRef class and the Discovery-doc Schema class.
 *
 * <p>This class abstracts the format (protobuf, discovery, etc) of the source from a type
 * definition.
 */
public interface TypeModel {

  /* @return if the underlying resource is a map type. */
  boolean isMap();

  /* @return the resource type of the map key. */
  TypeModel getMapKeyType();

  /* @return the resource type of the map value. */
  TypeModel getMapValueType();

  /* @return if the underlying resource is a proto Messsage. */
  boolean isMessage();

  /* @return if the underlying resource can be repeated in the parent resource. */
  boolean isRepeated();

  /* @return if this resource is an enum. */
  boolean isEnum();

  /* @return if this is a primitive type. */
  boolean isPrimitive();

  /* @return if this is the empty type. */
  boolean isEmptyType();

  void validateValue(String value);

  List<? extends FieldModel> getFields();

  /* @return the field with the given targetName, or null if targetName not found. */
  FieldModel getField(String targetName);

  TypeModel makeOptional();

  String getPrimitiveTypeName();

  boolean isBooleanType();

  boolean isStringType();

  boolean isFloatType();

  boolean isBytesType();

  boolean isDoubleType();

  String getTypeName();

  OneofConfig getOneOfConfig(String fieldName);
}
