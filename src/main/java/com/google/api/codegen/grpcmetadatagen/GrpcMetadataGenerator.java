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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.api.tools.framework.tools.ToolUtil;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

/** ToolDriver for gRPC meta-data generation. */
public class GrpcMetadataGenerator extends ToolDriverBase {
  public static final Option<String> OUTPUT_DIR =
      ToolOptions.createOption(
          String.class, "output_file", "The name of the output folder to put generated code.", "");
  public static final Option<String> INPUT_DIR =
      ToolOptions.createOption(
          String.class,
          "input_file",
          "The name of the folder containing the gRPC package to generate metadata for.",
          "");
  public static final Option<String> METADATA_CONFIG_FILE =
      ToolOptions.createOption(
          String.class,
          "metadata_config_file",
          "The name of the yaml file that configures package metadata.",
          "");
  public static final Option<String> LANGUAGE =
      ToolOptions.createOption(String.class, "language", "The package's language.", "");

  protected GrpcMetadataGenerator(ToolOptions options) {
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
    Map<String, Doc> docs = generate(model);
    ToolUtil.writeFiles(docs, options.get(OUTPUT_DIR));
  }

  protected Map<String, Doc> generate(Model model) throws IOException {
    TargetLanguage language = TargetLanguage.fromString(options.get(LANGUAGE));
    String configContent =
        new String(
            Files.readAllBytes(Paths.get(options.get(METADATA_CONFIG_FILE))),
            StandardCharsets.UTF_8);
    PackageMetadataConfig config = PackageMetadataConfig.createFromString(configContent);
    GrpcMetadataProvider provider = GrpcMetadataProviderFactory.create(language, config, options);
    return provider.generate(model, config);
  }
}
