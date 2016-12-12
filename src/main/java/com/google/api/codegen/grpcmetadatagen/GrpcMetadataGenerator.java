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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/**
 * ToolDriver for PackageMetadataGenerator; creates and sets the ToolOptions and builds the Model
 */
public class GrpcMetadataGenerator extends ToolDriverBase {

  private List<String> snippetFilenames;

  private GrpcPackageCopier packageCopier;

  public static final Option<String> OUTPUT_DIR =
      ToolOptions.createOption(
          String.class, "output_file", "The name of the output folder to put generated code.", "");
  public static final Option<String> INPUT_DIR =
      ToolOptions.createOption(
          String.class,
          "input_file",
          "The name of the folder containing the gRPC package to generate metadata for.",
          "");
  public static final Option<String> DEPENDENCIES_FILE =
      ToolOptions.createOption(
          String.class,
          "dependencies_file",
          "The name of the yaml file that configures package dependencies.",
          "");
  // TODO (jgeiger): Support python_package_file input to configure Python common protos namespace
  // packages.
  public static final Option<String> API_DEFAULTS_FILE =
      ToolOptions.createOption(
          String.class,
          "api_defaults_file",
          "The name of the yaml file that configures Python-specific package information.",
          "");
  public static final Option<String> SHORT_API_NAME =
      ToolOptions.createOption(
          String.class, "short_name", "The a single-word name for the API, e.g., 'Logging'.", "");
  public static final Option<String> PACKAGE_NAME =
      ToolOptions.createOption(
          String.class,
          "package_name",
          "The base name of the package to create, e.g., 'google-cloud-logging-v1'",
          "");
  public static final Option<String> API_PATH =
      ToolOptions.createOption(
          String.class,
          "api_path",
          "The path to the API directory under googleapis, e.g., 'google/cloud/logging'",
          "");
  public static final Option<String> API_VERSION =
      ToolOptions.createOption(
          String.class, "api_version", "The major version of the API, e.g., 'v1'", "");

  protected GrpcMetadataGenerator(
      ToolOptions options, List<String> snippetFilenames, GrpcPackageCopier packageCopier) {
    super(options);
    this.snippetFilenames = snippetFilenames;
    this.packageCopier = packageCopier;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Doc> generateDocs(Model model) throws IOException {
    // Copy gRPC package and create non-top-level files
    GrpcPackageCopierResult copierResults = packageCopier.run(options);

    Yaml yaml = new Yaml();

    String dependencies =
        new String(
            Files.readAllBytes(Paths.get(options.get(DEPENDENCIES_FILE))), StandardCharsets.UTF_8);
    Map<String, Object> dependenciesMap = (Map<String, Object>) yaml.load(dependencies);

    String defaults =
        new String(
            Files.readAllBytes(Paths.get(options.get(API_DEFAULTS_FILE))), StandardCharsets.UTF_8);
    Map<String, Object> defaultsMap = (Map<String, Object>) yaml.load(defaults);

    ApiNameInfo apiNameInfo =
        ApiNameInfo.create(
            model.getServiceConfig().getTitle(),
            options.get(SHORT_API_NAME),
            options.get(PACKAGE_NAME),
            options.get(API_VERSION),
            options.get(API_PATH));

    ImmutableMap.Builder<String, Doc> docs = new ImmutableMap.Builder<String, Doc>();
    docs.putAll(copierResults.docs());
    for (String snippetFilename : snippetFilenames) {
      GrpcMetadataContext context =
          new GrpcMetadataContext(
              snippetFilename, apiNameInfo, copierResults.metadata(), dependenciesMap, defaultsMap);
      CommonSnippetSetRunner runner = new CommonSnippetSetRunner(context);
      Doc result = runner.generate(context);
      if (!result.isWhitespace()) {
        docs.put(context.outputPath(), result);
      }
    }
    return docs.build();
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

    writeToFile(generateDocs(model));
  }

  private void writeToFile(Map<String, Doc> docs) throws Exception {
    ToolUtil.writeFiles(docs, options.get(OUTPUT_DIR));
  }
}
