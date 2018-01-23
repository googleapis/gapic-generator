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
package com.google.api.codegen.configgen;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import java.util.Map;

/** Generates API source specific interface data. */
public interface InterfaceTransformer {
  /**
   * Examines all of the resource paths used by the given apiInterface, and returns a map from each
   * unique resource paths to a short name used by the collection configuration.
   */
  Map<String, String> getResourceToEntityNameMap(InterfaceModel apiInterface);

  /** Generates the resource_name_generation fields. */
  void generateResourceNameGenerations(ConfigNode parentNode, ApiModel model);
}
