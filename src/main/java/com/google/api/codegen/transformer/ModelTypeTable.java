/* Copyright 2016 Google Inc
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
package com.google.api.codegen.transformer;

import com.google.api.tools.framework.model.TypeRef;

import java.util.List;

public interface ModelTypeTable {

  ModelTypeTable cloneEmpty();

  void saveNicknameFor(String fullName);

  String getFullNameFor(TypeRef type);

  String getNicknameFor(TypeRef type);

  String getAndSaveNicknameFor(String fullName);

  String getAndSaveNicknameFor(TypeRef type);

  String getAndSaveNicknameForElementType(TypeRef type);

  String renderPrimitiveValue(TypeRef type, String key);

  String getZeroValueAndSaveNicknameFor(TypeRef type);

  List<String> getImports();
}
