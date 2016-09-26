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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import java.io.File;

public class CSharpGapicSurfaceSnippetsTransformer extends CSharpGapicSurfaceCommonTransformer {

  private final GapicCodePathMapper pathMapper;

  public CSharpGapicSurfaceSnippetsTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  protected String getTemplateFileName() {
    return "csharp/gapic_snippets.snip";
  }

  @Override
  protected String getOutputPath(SurfaceTransformerContext context) {
    String name = context.getNamer().getApiWrapperClassName(context.getInterface());
    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    return outputPath + File.separator + name + "Snippets.g.cs";
  }

  @Override
  protected String getPackageName(ApiConfig apiConfig) {
    return apiConfig.getPackageName() + ".Snippets";
  }
}
