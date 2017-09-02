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
import com.google.api.codegen.transformer.TypeNameConverter;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import java.util.List;
import java.util.Map;

/** Input-agnostic model of a method. */
public interface MethodModel {

  /* @return the type of source that this FieldModel is based on. */
  ApiSource getApiSource();

  /* @return find a nested field in the method's input type by the nested field's name. */
  FieldModel getInputField(String fieldName);

  /* @return find a nested field in the method's output type by the nested field's name. */
  FieldModel getOutputField(String fieldName);

  /* @return the full name of this method. */
  String getFullName();

  /* @return the full name of the method's input. */
  String getInputFullName();

  /* @return the full name of the method's output. */
  String getOutputFullName();

  /* @return a short name for this method. */
  String getSimpleName();

  /* @return a short name for this method. */
  String getRawName();

  /* @return a short name for the parent. */
  String getParentSimpleName();

  /* @return the nickname for the parent. */
  String getParentNickname(TypeNameConverter typeNameConverter);

  /* @return the description of this method. */
  String getDescription();

  /* @return theTypeName for the output type. Save it in the table. */
  TypeName getOutputTypeName(ImportTypeTable typeTable);

  /* @return the TypeName for the input type. Save it in the table. */
  TypeName getInputTypeName(ImportTypeTable typeTable);

  /* @return a short name for the output type. */
  String getOutputTypeSimpleName();

  String getScopedDescription();

  GenericFieldSelector getInputFieldSelector(String fieldName);

  boolean getRequestStreaming();

  boolean getResponseStreaming();

  String getAndSaveRequestTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer);

  String getAndSaveResponseTypeName(ImportTypeTable typeTable, SurfaceNamer surfaceNamer);

  Name asName();

  boolean isOutputTypeEmpty();

  boolean hasReturnValue();

  Iterable<FieldModel> getInputFields();

  List<FieldModel> getInputFieldsForResourceNameMethod();

  Iterable<FieldModel> getOutputFields();

  Iterable<FieldModel> getResourceNameInputFields();

  boolean isIdempotent();

  Map<String, String> getResourcePatternNameMap(Map<String, String> nameMap);
}
