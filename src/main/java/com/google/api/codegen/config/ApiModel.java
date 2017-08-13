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

/**
 * Wrapper class around the API models (e.g. protobuf Model, or Discovery Document).
 *
 * <p>This class abstracts the format (protobuf, discovery, etc) of the source from a resource type
 * definition.
 */
public interface ApiModel {
  /* @return the type of source that this FieldModel is based on. */
  ApiSource getApiSource();

  String getServiceName();

  /** Return the service address. */
  String getServiceAddress();

  /** Return the service port. TODO(cbao): Read the port from config. */
  Integer getServicePort();

  String getTitle();

  /** Return a list of scopes for authentication. */
  List<String> getAuthScopes();

  Iterable<? extends InterfaceModel> getInterfaces(GapicProductConfig productConfig);

  boolean hasMultipleServices(GapicProductConfig productConfig);

  InterfaceModel getInterface(String interfaceName);

  String getDocumentationSummary();
}
