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
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.TypeRef;
import com.google.api.tools.framework.model.TypeRef.Cardinality;
import javax.annotation.Nullable;

/**
 * Wrapper class around the protobuf Field class and the Discovery-doc Schema class.
 *
 * <p>This class abstracts the format (protobuf, discovery, etc) of the source from a resource type
 * definition.
 */
public interface FieldModel {

  /* @return the type of source that this FieldModel is based on. */
  ApiSource getApiSource();

  String getSimpleName();

  String getFullName();

  Name asName();

  String getTypeFullName();

  /* @return if the underlying resource is a map type. */
  boolean isMap();

  /* @return the resource type of the map key. */
  FieldModel getMapKeyField();

  /* @return the resource type of the map value. */
  FieldModel getMapValueField();

  /* @return if the underlying resource is a proto Messsage. */
  boolean isMessage();

  /* @return if the underlying resource is String-typed. */
  boolean isString();

  /* @return if the underlying resource is byte-typed. */
  boolean isBytes();

  /* @return if the underlying resource can be repeated in the parent resource. */
  boolean isRepeated();

  /* @return if this field is required to be non-null in the parent object. */
  boolean isRequired();

  /* @return the full name of the parent. */
  String getParentFullName();

  /* @return the simple name of the parent. */
  String getParentSimpleName();

  /* @return the parent of this model, if the parent is a FieldModel, else return this object. */
  TypeName getParentTypeName(ImportTypeTable typeTable);

  /* @return the cardinality of the resource. */
  Cardinality getCardinality();

  /* @return if this resource is an enum. */
  boolean isEnum();

  /* @return if this is a primitive type. */
  boolean isPrimitive();

  /* Get the description of the element scoped to the visibility as currently set in the model. */
  String getScopedDocumentation();

  /* @return the simple name of the Oneof (if it exists) associated with this model. */
  Iterable<String> getOneofFieldsNames(SurfaceNamer surfaceNamer);

  // Functions that are specific to the source type.

  /* @return the TypeRef of the underlying protobuf Field, if it exists. */
  TypeRef getProtoTypeRef();

  /* @return the underlying Schema, if it exists. */
  Schema getDiscoveryField();

  /* @return the type of this object, formatted as a String. */
  String getKind();

  @Nullable
  Oneof getOneof();
}
