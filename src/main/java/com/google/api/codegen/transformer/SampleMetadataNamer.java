/* Copyright 2019 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.config.SampleContext;
import java.util.List;

/**
 * A SampleMetadataNamer provides language-specific names for specific components of a sample
 * metadata view.
 */
public interface SampleMetadataNamer {

  String getEnvironment();

  String getBasePath(ProductConfig config);

  String getBin();

  String getInvocation();

  String getSamplePath(String uniqueSampleId);

  default String getSampleClassName(String uniqueSampleId) {
    return "";
  }

  default String getPackageName(GapicProductConfig productConfig) {
    return "";
  }

  List<SampleContext> getSampleContexts(
      List<InterfaceContext> interfaceContexts, GapicProductConfig productConfig);
}
