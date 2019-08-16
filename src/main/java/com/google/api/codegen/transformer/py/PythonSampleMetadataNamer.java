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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.SampleMetadataNamer;
import java.util.List;

public class PythonSampleMetadataNamer implements SampleMetadataNamer {

  private final PythonGapicSamplesTransformer pythonSampleTransformer;
  private final GapicCodePathMapper pathMapper;

  public PythonSampleMetadataNamer(
      PythonGapicSamplesTransformer pythonSampleTransformer, GapicCodePathMapper pathMapper) {
    this.pythonSampleTransformer = pythonSampleTransformer;
    this.pathMapper = pathMapper;
  }

  public String getEnvironment() {
    return "python";
  }

  public String getBasePath(ProductConfig config) {
    return pathMapper.getOutputPath("", config);
  }

  public String getBin() {
    return "python3";
  }

  public String getInvocation() {
    return "{bin} {path} @args";
  }

  public String getSamplePath(String uniqueSampleId) {
    return "{base_path}/" + uniqueSampleId + ".py";
  }

  public List<SampleContext> getSampleContexts(
      List<InterfaceContext> interfaceContexts, GapicProductConfig productConfig) {
    return pythonSampleTransformer.getSampleContexts(interfaceContexts, productConfig);
  }
}
