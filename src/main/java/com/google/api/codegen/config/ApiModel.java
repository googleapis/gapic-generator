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

import com.google.api.tools.framework.model.DiagCollector;
import java.util.List;

/**
 * Wrapper class around the API models (e.g. protobuf Model, or Discovery Document).
 *
 * <p>This class abstracts the format (protobuf, discovery, etc) of the source from a resource type
 * definition.
 */
public interface ApiModel {

  String getServiceName();

  DiagCollector getDiagCollector();

  String getTitle();

  /** Return a list of scopes for authentication. */
  List<String> getAuthScopes(GapicProductConfig gapicProductConfig);

  List<? extends InterfaceModel> getInterfaces();

  List<? extends TypeModel> getAdditionalTypes();

  boolean hasMultipleServices();

  InterfaceModel getInterface(String interfaceName);

  String getDocumentationSummary();
}
