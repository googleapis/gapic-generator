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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.SampleMetadataNamer;
import java.util.List;

public class NodeJSSampleMetadataNamer implements SampleMetadataNamer {

  private final NodeJSGapicSamplesTransformer nodejsSampleTransformer;
  private final GapicCodePathMapper pathMapper;

  public NodeJSSampleMetadataNamer(
      NodeJSGapicSamplesTransformer nodejsSampleTransformer, GapicCodePathMapper pathMapper) {
    this.nodejsSampleTransformer = nodejsSampleTransformer;
    this.pathMapper = pathMapper;
  }

  public String getEnvironment() {
    return "nodejs";
  }

  public String getBasePath(ProductConfig config) {
    return pathMapper.getOutputPath("", config);
  }

  public String getBin() {
    return "node";
  }

  public String getInvocation() {
    return "{bin} {path} @args";
  }

  public String getSamplePath(String uniqueSampleId) {
    return "{base_path}/" + uniqueSampleId + ".js";
  }

  public List<SampleContext> getSampleContexts(
      List<InterfaceContext> interfaceContexts, GapicProductConfig productConfig) {
    return nodejsSampleTransformer.getSampleContexts(interfaceContexts, productConfig);
  }
}
