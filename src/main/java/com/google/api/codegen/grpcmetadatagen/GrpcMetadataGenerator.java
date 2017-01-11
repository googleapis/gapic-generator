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

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.grpcmetadatagen.py.PythonPackageCopier;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * ToolDriver for PackageMetadataGenerator; creates and sets the ToolOptions and builds the Model
 */
public class GrpcMetadataGenerator extends ToolDriverBase {
  private static final Map<TargetLanguage, List<String>> SNIPPETS =
      new ImmutableMap.Builder<TargetLanguage, List<String>>()
          .put(
              TargetLanguage.PYTHON,
              Lists.newArrayList(
                  "metadatagen/LICENSE.snip",
                  "metadatagen/py/setup.py.snip",
                  "metadatagen/py/README.rst.snip",
                  "metadatagen/py/PUBLISHING.rst.snip",
                  "metadatagen/py/MANIFEST.in.snip"))
          .build();

  private static final Map<TargetLanguage, GrpcPackageCopier> COPIERS =
      new ImmutableMap.Builder<TargetLanguage, GrpcPackageCopier>()
          .put(TargetLanguage.PYTHON, new PythonPackageCopier())
          .build();

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

  @SuppressWarnings("unchecked")
  protected Map<String, Doc> generateDocs(Model model) throws IOException {
    String configContent =
        new String(
            Files.readAllBytes(Paths.get(options.get(METADATA_CONFIG_FILE))),
            StandardCharsets.UTF_8);
    PackageMetadataConfig config = PackageMetadataConfig.createFromString(configContent);
    TargetLanguage language = TargetLanguage.fromString(options.get(LANGUAGE));

    // Copy gRPC package and create non-top-level files
    GrpcPackageCopierResult copierResults = getCopier(language).run(options, config);

    ImmutableMap.Builder<String, Doc> docs = new ImmutableMap.Builder<String, Doc>();
    docs.putAll(copierResults.docs());
    PackageMetadataTransformer transformer = new PackageMetadataTransformer();

    for (String snippetFilename : getSnippets(language)) {
      PackageMetadataView view =
          transformer
              .generateMetadataView(
                  config, model, snippetFilename, outputPath(snippetFilename), language)
              // Set language-specific GrpcCopierResults here.
              .namespacePackages(copierResults.namespacePackages())
              .build();
      CommonSnippetSetRunner runner = new CommonSnippetSetRunner(view);
      Doc result = runner.generate(view);
      if (!result.isWhitespace()) {
        docs.put(view.outputPath(), result);
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

  private static String outputPath(String templateFileName) {
    String baseName = Paths.get(templateFileName).getFileName().toString();
    int extensionIndex = baseName.lastIndexOf(".");
    return baseName.substring(0, extensionIndex);
  }

  private static List<String> getSnippets(TargetLanguage language) {
    return getForLanguage(SNIPPETS, language);
  }

  private static GrpcPackageCopier getCopier(TargetLanguage language) {
    return getForLanguage(COPIERS, language);
  }

  private static <T> T getForLanguage(Map<TargetLanguage, T> map, TargetLanguage language) {
    T value = map.get(language);
    if (value == null) {
      throw new IllegalArgumentException(
          "The target language \"" + language + "\" is not supported");
    }
    return value;
  }
}
