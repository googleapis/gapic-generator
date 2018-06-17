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
package com.google.api.codegen.configgen;

import com.google.api.codegen.configgen.mergers.ProtoConfigMerger;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.tools.ToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import java.util.Map;

/** Main class for the config generator. */
public class GapicConfigGeneratorApp extends ToolDriverBase {

  public static final Option<String> OUTPUT_FILE =
      ToolOptions.createOption(
          String.class, "output_file", "The path of the output file to put generated config.", "");

  /** Constructs a config generator api based on given options. */
  public GapicConfigGeneratorApp(ToolOptions options) {
    super(options);
  }

  @Override
  protected void process() throws Exception {
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return;
    }

    String outputPath = options.get(OUTPUT_FILE);
    Map<String, String> outputFiles = generateConfig(outputPath);
    ToolUtil.writeFiles(outputFiles, "");
  }

  private Map<String, String> generateConfig(String outputPath) {
    ConfigNode node = new ProtoConfigMerger().mergeConfig(model, outputPath);
    ConfigGenerator configGenerator = new ConfigGenerator(0);
    configGenerator.visit(node);
    return ImmutableMap.of(outputPath, configGenerator.toString());
  }
}
