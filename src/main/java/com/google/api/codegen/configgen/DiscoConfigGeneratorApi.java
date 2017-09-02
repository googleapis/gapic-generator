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
package com.google.api.codegen.configgen;

import static com.google.api.codegen.DiscoGapicGeneratorApi.DISCOVERY_DOC_OPTION_NAME;

import com.google.api.codegen.configgen.transformer.DiscoConfigTransformer;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.tools.DiscoToolDriverBase;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** Main class for the config generator. */
public class DiscoConfigGeneratorApi extends DiscoToolDriverBase {

  public static final Option<String> DISCOVERY_DOC =
      ToolOptions.createOption(
          String.class,
          DISCOVERY_DOC_OPTION_NAME,
          "The Discovery doc representing the service description.",
          "");

  public static final Option<String> OUTPUT_FILE =
      ToolOptions.createOption(
          String.class, "output_file", "The path of the output file to put generated config.", "");

  /** Constructs a config generator api based on given options. */
  public DiscoConfigGeneratorApi(ToolOptions options) {
    super(options);
  }

  @Override
  protected void process() throws Exception {
    String outputPath = options.get(OUTPUT_FILE);
    Map<String, Doc> outputFiles = generateConfig(outputPath);
    ToolUtil.writeFiles(outputFiles, "");
  }

  private Map<String, Doc> generateConfig(String outputPath) {
    ViewModel viewModel = new DiscoConfigTransformer().generateConfig(document, outputPath);
    return ImmutableMap.of(
        outputPath, new CommonSnippetSetRunner(new CommonRenderingUtil()).generate(viewModel));
  }
}
