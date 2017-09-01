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

import java.util.List;

/** API-source-agnostic wrapper classes for Interfaces. */
public interface InterfaceModel {
  /* @return the type of source that this FieldModel is based on. */
  ApiSource getApiSource();

  /* @return the Model from which this interface came from. */
  ApiModel getApiModel();

  String getSimpleName();

  String getFullName();

  String getParentFullName();

  String getFileSimpleName();

  String getFileFullName();

  List<MethodModel> getMethods();

  /* @return true if the element is reachable with the current scoper. */
  boolean isReachable();
}
