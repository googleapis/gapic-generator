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
package com.google.api.codegen;

import com.google.api.Service;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.gapic.GapicGeneratorConfig;
import com.google.api.codegen.gapic.GapicProvider;
import com.google.api.codegen.gapic.MainGapicProviderFactory;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.snippet.Doc;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Base class for code generator baseline tests. */
public abstract class GapicTestBase extends ConfigBaselineTestCase {

  // example: library[ruby_message]
  // example 2: library[java_package-info]
  private static final Pattern BASELINE_PATTERN = Pattern.compile("((?:-|\\w)+)\\[((?:-|\\w)+)\\]");

  // Wiring
  // ======

  private final String name;
  private final String idForFactory;
  private final String[] gapicConfigFileNames;
  private final String snippetName;
  protected ConfigProto config;

  public GapicTestBase(
      String name, String idForFactory, String[] gapicConfigFileNames, String snippetName) {
    this.name = name;
    this.idForFactory = idForFactory;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.snippetName = snippetName;
  }

  @Override
  protected void test(String... baseNames) throws Exception {
    super.test(new GapicTestModelGenerator(getTestDataLocator(), tempDir), baseNames);
  }

  @Override
  protected void setupModel() {
    super.setupModel();
    config =
        CodegenTestUtil.readConfig(
            model.getDiagCollector(), getTestDataLocator(), gapicConfigFileNames);
    // TODO (garrettjones) depend on the framework to take care of this.
    if (model.getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      throw new IllegalArgumentException("Problem creating Generator");
    }
  }

  @Override
  protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  @Override
  protected String baselineFileName() {
    String methodName = testName.getMethodName();
    Matcher m = BASELINE_PATTERN.matcher(methodName);
    if (m.find()) {
      return m.group(2) + "_" + m.group(1) + ".baseline";
    } else {
      return name + "_" + methodName + ".baseline";
    }
  }

  /**
   * Creates the constructor arguments to be passed onto this class (GapicTestBase) to create test
   * methods. The idForFactory String is passed to GapicProviderFactory to get the GapicProviders
   * provided by that id, and then the snippet file names are scraped from those providers, and a
   * set of arguments is created for each combination of GapicProvider x snippet that
   * GapicProviderFactory returns.
   */
  public static List<Object[]> createTestedConfigs(
      String idForFactory, String[] gapicConfigFileNames) {
    Model model = Model.create(Service.getDefaultInstance());
    ApiConfig apiConfig = ApiConfig.createDummyApiConfig();

    GapicGeneratorConfig generatorConfig =
        GapicGeneratorConfig.newBuilder()
            .id(idForFactory)
            .enabledArtifacts(new ArrayList<String>())
            .build();
    List<GapicProvider<? extends Object>> providers =
        MainGapicProviderFactory.defaultCreate(model, apiConfig, generatorConfig);
    List<Object[]> testArgs = new ArrayList<>();
    for (GapicProvider<? extends Object> provider : providers) {
      for (String snippetFileName : provider.getSnippetFileNames()) {
        String fileName = snippetFileName.split("\\.")[0].split("/")[1];
        String id = idForFactory + "_" + fileName;
        testArgs.add(new Object[] {id, idForFactory, gapicConfigFileNames, snippetFileName});
      }
    }
    return testArgs;
  }

  @Override
  public Map<String, Doc> run() {
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    ApiConfig apiConfig = ApiConfig.createApiConfig(model, config);
    if (apiConfig == null) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    GapicGeneratorConfig generatorConfig =
        GapicGeneratorConfig.newBuilder()
            .id(idForFactory)
            .enabledArtifacts(new ArrayList<String>())
            .build();
    List<GapicProvider<? extends Object>> providers =
        MainGapicProviderFactory.defaultCreate(model, apiConfig, generatorConfig);
    GapicProvider<? extends Object> testedProvider = null;
    for (GapicProvider<? extends Object> provider : providers) {
      for (String snippetFileName : provider.getSnippetFileNames()) {
        if (snippetFileName.equals(snippetName)) {
          testedProvider = provider;
          break;
        }
      }
      if (testedProvider != null) {
        break;
      }
    }

    Map<String, Doc> output = testedProvider.generate(snippetName);
    if (output == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiagCollector().getDiags()) {
        testOutput().println(diag.toString());
      }
    }

    return output;
  }
}
