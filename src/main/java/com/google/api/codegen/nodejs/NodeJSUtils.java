/* Copyright 2016 Google LLC
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
package com.google.api.codegen.nodejs;

import com.google.api.codegen.config.GapicProductConfig;

public class NodeJSUtils {
  /**
   * Returns true if the current API is a part of gcloud (i.e. cloud API). This can be known if the
   * domain_layer_location is "google-cloud".
   */
  public static boolean isGcloud(GapicProductConfig config) {
    return "google-cloud".equals(config.getDomainLayerLocation());
  }
}
