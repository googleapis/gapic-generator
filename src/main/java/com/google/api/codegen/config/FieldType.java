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

import com.google.api.codegen.discovery.Schema;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import java.util.List;

/**
 * Wrapper class around the protobuf Field class and the Discovery-doc Schema class.
 *
 * <p>Each instance of this class contains exactly one of {Field, Schema}. This class abstracts the
 * format (protobuf, discovery, etc) of the source from a resource type definition.
 */
public interface FieldType {

  /* @return the type of source that this FieldType is based on. */
  ApiSource getApiSource();

  String getSimpleName();

  String getFullName();

  /* @return if the underlying resource is a map type. */
  boolean isMap();

  /* @return the resource type of the map key. */
  FieldType getMapKeyField();

  /* @return the resource type of the map value. */
  FieldType getMapValueField();

  /* @return if the underlying resource is a proto Messsage. */
  boolean isMessage();

  /* @return if the underlying resource can be repeated in the parent resource. */
  boolean isRepeated();

  /* @return the full name of the parent. */
  String getParentFullName();

  /* @return the cardinality of the resource. */
  Cardinality getCardinality();

  /* @return if this resource is an enum. */
  boolean isEnum();

  /* @return if this is a primitive type. */
  boolean isPrimitive();

  // Functions that are specific to the source type.

  /* @return the TypeRef of the underlying protobuf Field, if it exists. */
  TypeRef getProtoTypeRef();

  /* @return the underlying Schema, if it exists. */
  Schema getDiscoveryField();

  /* Get the description of the element scoped to the visibility as currently set in the model. */
  String getScopedDocumentation();

  /* @return the simple name of the Oneof (if it exists) associated with this model. */
  List<String> getOneofFieldsNames();
}
