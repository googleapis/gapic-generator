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
package com.google.api.codegen.metadatagen;

import com.google.api.codegen.GeneratedResult;
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
import java.util.List;
import java.util.Map;

/**
 * ToolDriver for PackageMetadataGenerator; creates and sets the ToolOptions and builds the Model
 */
public class PackageMetadataGenerator extends ToolDriverBase {

  private List<String> snippetFilenames;

  private PackageCopier packageCopier;

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
  // TODO (jgeiger): Support this input to configure Python common protos namespace packages.
  public static final Option<String> PYTHON_PACKAGE_FILE =
      ToolOptions.createOption(
          String.class,
          "python_package_file",
          "The name of the yaml file that configures Python-specific package information.",
          "");
  public static final Option<String> API_DEFAULTS_FILE =
      ToolOptions.createOption(
          String.class,
          "api_defaults_file",
          "The name of the yaml file that configures Python-specific package information.",
          "");
  // TODO (jgeiger): This eventually will come from the service config/Model and will not be
  // configurable. See https://github.com/googleapis/toolkit/issues/270
  public static final Option<String> LONG_API_NAME =
      ToolOptions.createOption(
          String.class,
          "long_name",
          "The API full name, including any prefixed branding, e.g., 'Stackdriver Logging'.",
          "");
  public static final Option<String> SHORT_API_NAME =
      ToolOptions.createOption(
          String.class, "short_name", "The a single-word name for the API, e.g., 'Logging'.", "");
  public static final Option<String> API_PATH =
      ToolOptions.createOption(
          String.class,
          "api_path",
          "The path to the API directory under googleapis, e.g., 'google/cloud/logging'",
          "");
  public static final Option<String> API_VERSION =
      ToolOptions.createOption(
          String.class, "api_version", "The major version of the API, e.g., 'v1'", "");

  protected PackageMetadataGenerator(
      ToolOptions options, List<String> snippetFilenames, PackageCopier packageCopier) {
    super(options);
    this.snippetFilenames = snippetFilenames;
    this.packageCopier = packageCopier;
  }

  @SuppressWarnings("unchecked")
  protected Map<String, Doc> generateDocs(Model model) throws IOException {
    // Used to create a "special" Doc that can then be fed into the main generation to propagate
    // the results of the copy phase.
    String copierMetadataKey = "_PACKAGE_COPIER_RESULTS_METADATA";

    // Copy gRPC package and add non-top-level files
    Map<String, Doc> copierResults = packageCopier.run(options, copierMetadataKey);

    PackageMetadataSnippetSetRunner runner = new PackageMetadataSnippetSetRunner();
    ApiNameInfo apiNameInfo =
        ApiNameInfo.create(
            options.get(LONG_API_NAME),
            options.get(SHORT_API_NAME),
            options.get(API_VERSION),
            options.get(API_PATH));

    PackageMetadataContext context =
        new PackageMetadataContext(apiNameInfo, copierResults.get(copierMetadataKey));
    context.loadConfigurationFromFile(
        options.get(DEPENDENCIES_FILE), options.get(API_DEFAULTS_FILE));

    // Add top-level doc files
    ImmutableMap.Builder<String, Doc> docs = new ImmutableMap.Builder<String, Doc>();
    for (String key : copierResults.keySet()) {
      if (!key.equals(copierMetadataKey)) {
        docs.put(key, copierResults.get(key));
      }
    }
    for (String snippetFilename : snippetFilenames) {
      GeneratedResult result = runner.generate(model, snippetFilename, context);
      if (!result.getDoc().isWhitespace()) {
        docs.put(result.getFilename(), result.getDoc());
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
