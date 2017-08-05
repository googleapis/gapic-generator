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

import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TypeFormatter;
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;

/** Input-agnostic model of a method. */
public interface MethodModel {

  /* @return the type of source that this FieldType is based on. */
  FieldType.ApiSource getApiSource();

  FieldType lookupInputField(String fieldName);

  FieldType lookupOutputField(String fieldName);

  String getFullName();

  String getInputFullName();

  String getSimpleName();

  String getParentSimpleName();

  String getParentNickname(TypeNameConverter typeNameConverter);

  String getDescription();

  String getOutputTypeFullName(TypeFormatter typeFormatter);

  String getInputTypeNickName(TypeFormatter typeFormatter);

  String getOutputTypeNickname(TypeFormatter typeFormatter);

  String getOutputTypeSimpleName();

  String getScopedDescription();

  TypeName getOutputTypeName(TypeNameConverter typeNameConverter);

  GenericFieldSelector getInputFieldSelector(String fieldName);

  boolean getRequestStreaming();

  boolean getResponseStreaming();

  String getAndSaveRequestTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer);

  String getAndSaveResponseTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer);

  Name asName();

  boolean isOutputTypeEmpty();

  boolean hasReturnValue();

  String getProtoMethodName();
}
