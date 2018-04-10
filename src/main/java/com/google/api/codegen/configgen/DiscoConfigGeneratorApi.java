/* Copyright 2017 Google LLC
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

import static com.google.api.codegen.DiscoGapicGeneratorApi.DISCOVERY_DOC_OPTION_NAME;

import com.google.api.codegen.DiscoGapicGeneratorApi;
import com.google.api.codegen.DocumentGenerator;
import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.configgen.transformer.DiscoConfigTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.GenericToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.api.tools.framework.tools.ToolUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/** Main class for the config generator. */
public class DiscoConfigGeneratorApi extends GenericToolDriverBase {

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
    Document document = setupDocument();
    ViewModel viewModel =
        new DiscoConfigTransformer().generateConfig(new DiscoApiModel(document, ""), outputPath);
    Map<String, GeneratedResult<Doc>> generatedConfig =
        new CommonSnippetSetRunner(new CommonRenderingUtil(), true).generate(viewModel);
    return GeneratedResult.extractBodies(generatedConfig);
  }

  /** Initializes the Discovery document document. */
  private Document setupDocument() {
    // Prevent INFO messages from polluting the log.
    Logger.getLogger("").setLevel(Level.WARNING);
    String discoveryDocPath = options.get(DiscoGapicGeneratorApi.DISCOVERY_DOC);

    Document document = null;
    try {
      document = DocumentGenerator.createDocumentAndLog(discoveryDocPath, getDiagCollector());
    } catch (FileNotFoundException e) {
      getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "File not found: " + discoveryDocPath));
    } catch (IOException e) {
      getDiagCollector()
          .addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL, "Failed to read Discovery Doc: " + discoveryDocPath));
    }
    return document;
  }
}
