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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.SampleMetadataNamer;
import com.google.api.codegen.util.Name;
import java.util.List;

public class CSharpSampleMetadataNamer implements SampleMetadataNamer {

  private final CSharpStandaloneSampleTransformer csharpSampleTransformer;
  private final GapicCodePathMapper pathMapper;

  public CSharpSampleMetadataNamer(
      CSharpStandaloneSampleTransformer csharpSampleTransformer, GapicCodePathMapper pathMapper) {
    this.csharpSampleTransformer = csharpSampleTransformer;
    this.pathMapper = pathMapper;
  }

  public String getEnvironment() {
    return "csharp";
  }

  public String getBasePath(ProductConfig config) {
    return config.getPackageName() + ".Samples";
  }

  public String getBin() {
    return "dotnet run";
  }

  public String getInvocation() {
    return "dotnet build /p:StartupObject={package}; {bin} @args";
  }

  public String getSamplePath(String uniqueSampleId) {
    return "{base_path}/" + Name.from(uniqueSampleId).toUpperCamel() + ".cs";
  }

  public String getSampleClassName(String uniqueSampleId) {
    return "{package}." + Name.from(uniqueSampleId).toUpperCamel() + "Main";
  }

  public String getPackageName(GapicProductConfig productConfig) {
    return productConfig.getPackageName() + ".Samples";
  }

  public List<SampleContext> getSampleContexts(
      List<InterfaceContext> interfaceContexts, GapicProductConfig productConfig) {
    return csharpSampleTransformer.getSampleContexts(interfaceContexts, productConfig);
  }
}
