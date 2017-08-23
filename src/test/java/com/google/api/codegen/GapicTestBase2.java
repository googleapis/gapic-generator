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
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.gapic.GapicGeneratorConfig;
import com.google.api.codegen.gapic.GapicProvider;
import com.google.api.codegen.gapic.MainGapicProviderFactory;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;

/** Base class for code generator baseline tests. */
public abstract class GapicTestBase2 extends ConfigBaselineTestCase {
  // Wiring
  // ======

  private final String idForFactory;
  private final String[] gapicConfigFileNames;
  @Nullable private final String packageConfigFileName;
  private final ImmutableList<String> snippetNames;
  protected ConfigProto gapicConfig;
  protected PackageMetadataConfig packageConfig;
  private final String baselineFile;

  public GapicTestBase2(
      String idForFactory,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      List<String> snippetNames,
      String baselineFile) {
    this.idForFactory = idForFactory;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.packageConfigFileName = packageConfigFileName;
    this.snippetNames = ImmutableList.copyOf(snippetNames);
    this.baselineFile = baselineFile;
  }

  @Override
  protected void test(String... baseNames) throws Exception {
    super.test(new GapicTestModelGenerator(getTestDataLocator(), tempDir), baseNames);
  }

  @Override
  protected void setupModel() {
    super.setupModel();
    gapicConfig =
        CodegenTestUtil.readConfig(
            model.getDiagCollector(), getTestDataLocator(), gapicConfigFileNames);
    if (!Strings.isNullOrEmpty(packageConfigFileName)) {
      try {
        URI packageConfigUrl = getTestDataLocator().findTestData(packageConfigFileName).toURI();
        String contents =
            new String(Files.readAllBytes(Paths.get(packageConfigUrl)), StandardCharsets.UTF_8);
        packageConfig = PackageMetadataConfig.createFromString(contents);
      } catch (IOException | URISyntaxException e) {
        throw new IllegalArgumentException("Problem creating packageConfig");
      }
    }
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

  /**
   * Creates the constructor arguments to be passed onto this class (GapicTestBase2) to create test
   * methods. The idForFactory String is passed to GapicProviderFactory to get the GapicProviders
   * provided by that id, and then the snippet file names are scraped from those providers, and a
   * set of arguments is created for each combination of GapicProvider x snippet that
   * GapicProviderFactory returns.
   */
  public static Object[] createTestConfig(
      String idForFactory,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      String apiName) {
    Model model = Model.create(Service.getDefaultInstance());
    GapicProductConfig productConfig = GapicProductConfig.createDummyInstance();
    PackageMetadataConfig packageConfig = PackageMetadataConfig.createDummyPackageMetadataConfig();

    GapicGeneratorConfig generatorConfig =
        GapicGeneratorConfig.newBuilder()
            .id(idForFactory)
            .enabledArtifacts(Arrays.asList("surface", "test", "sample_app"))
            .build();
    List<GapicProvider<? extends Object>> providers =
        MainGapicProviderFactory.defaultCreate(
            model, productConfig, generatorConfig, packageConfig, "");

    List<String> snippetNames = new ArrayList<>();
    for (GapicProvider<? extends Object> provider : providers) {
      snippetNames.addAll(provider.getSnippetFileNames());
    }

    String baseline = idForFactory + "_" + apiName + ".baseline";

    return new Object[] {
      idForFactory, gapicConfigFileNames, packageConfigFileName, snippetNames, apiName, baseline
    };
  }

  @Override
  protected String baselineFileName() {
    return baselineFile;
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

    GapicProductConfig productConfig = GapicProductConfig.create(model, gapicConfig);
    if (productConfig == null) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    List<String> enabledArtifacts = new ArrayList<>();
    if (hasSmokeTestConfig(productConfig)) {
      enabledArtifacts.addAll(Arrays.asList("surface", "test", "sample_app"));
    }

    GapicGeneratorConfig generatorConfig =
        GapicGeneratorConfig.newBuilder()
            .id(idForFactory)
            .enabledArtifacts(enabledArtifacts)
            .build();
    List<GapicProvider<? extends Object>> providers =
        MainGapicProviderFactory.defaultCreate(
            model, productConfig, generatorConfig, packageConfig, "");

    List<String> disabledGen = new ArrayList<>(snippetNames);
    for (GapicProvider<? extends Object> provider : providers) {
      disabledGen.removeAll(provider.getSnippetFileNames());
    }
    for (String gen : disabledGen) {
      testOutput().printf("%s generation is not enabled for this test case.\n", gen);
    }

    boolean reportDiag = false;
    Map<String, Doc> output = new TreeMap<>();
    for (GapicProvider<? extends Object> provider : providers) {
      Map<String, Doc> out = provider.generate();
      if (output == null) {
        reportDiag = true;
      } else {
        output.putAll(out);
      }
    }

    if (reportDiag) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        testOutput().println(diag.toString());
      }
    }

    return output;
  }

  private static boolean hasSmokeTestConfig(GapicProductConfig productConfig) {
    for (GapicInterfaceConfig config : productConfig.getInterfaceConfigMap().values()) {
      if (config.getSmokeTestConfig() != null) {
        return true;
      }
    }
    return false;
  }
}
