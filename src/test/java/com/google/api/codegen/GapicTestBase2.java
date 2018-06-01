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
package com.google.api.codegen;

import com.google.api.Service;
import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.ApiDefaultsConfig;
import com.google.api.codegen.config.DependenciesConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.PackagingConfig;
import com.google.api.codegen.gapic.GapicProviderFactory;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;

/** Base class for code generator baseline tests. */
public abstract class GapicTestBase2 extends ConfigBaselineTestCase {
  // Wiring
  // ======

  private final TargetLanguage language;
  private final String[] gapicConfigFileNames;
  @Nullable private final String packageConfigFileName;
  private final ImmutableList<String> snippetNames;
  protected ConfigProto gapicConfig;
  protected PackageMetadataConfig packageConfig;
  private final String baselineFile;

  public GapicTestBase2(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      List<String> snippetNames,
      String baselineFile) {
    this.language = language;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.packageConfigFileName = packageConfigFileName;
    this.snippetNames = ImmutableList.copyOf(snippetNames);
    this.baselineFile = baselineFile;

    getTestDataLocator().addTestDataSource(getClass(), "testsrc");
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
        ApiDefaultsConfig apiDefaultsConfig = ApiDefaultsConfig.load();
        DependenciesConfig dependenciesConfig =
            DependenciesConfig.loadFromURL(
                getTestDataLocator().findTestData("frozen_dependencies.yaml"));
        PackagingConfig packagingConfig =
            PackagingConfig.loadFromURL(getTestDataLocator().findTestData(packageConfigFileName));
        packageConfig =
            PackageMetadataConfig.createFromPackaging(
                apiDefaultsConfig, dependenciesConfig, packagingConfig);
      } catch (IOException e) {
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
   * set of arguments is created for each combination of CodeGenerator x snippet that
   * GapicProviderFactory returns.
   */
  public static Object[] createTestConfig(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      String apiName) {
    Model model = Model.create(Service.getDefaultInstance());
    GapicProductConfig productConfig = GapicProductConfig.createDummyInstance();
    PackageMetadataConfig packageConfig = PackageMetadataConfig.createDummyPackageMetadataConfig();

    List<CodeGenerator<?>> providers =
        GapicProviderFactory.create(
            language,
            model,
            productConfig,
            packageConfig,
            Arrays.asList("surface", "test", "samples"));

    List<String> snippetNames = new ArrayList<>();
    for (CodeGenerator<?> provider : providers) {
      snippetNames.addAll(provider.getInputFileNames());
    }

    String baseline = language.toString().toLowerCase() + "_" + apiName + ".baseline";

    return new Object[] {
      language, gapicConfigFileNames, packageConfigFileName, snippetNames, apiName, baseline
    };
  }

  @Override
  protected String baselineFileName() {
    return baselineFile;
  }

  @Override
  public Map<String, ?> run() throws IOException {
    model.establishStage(Merged.KEY);
    if (model.getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    GapicProductConfig productConfig = GapicProductConfig.create(model, gapicConfig, language);
    if (productConfig == null) {
      for (Diag diag : model.getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    List<String> enabledArtifacts = new ArrayList<>();
    if (hasSmokeTestConfig(productConfig)) {
      enabledArtifacts.addAll(Arrays.asList("surface", "test", "samples"));
    }

    List<CodeGenerator<?>> providers =
        GapicProviderFactory.create(
            language, model, productConfig, packageConfig, enabledArtifacts);

    // Don't run any providers we're not testing.
    ArrayList<CodeGenerator<?>> testedProviders = new ArrayList<>();
    for (CodeGenerator<?> provider : providers) {
      if (!Collections.disjoint(provider.getInputFileNames(), snippetNames)) {
        testedProviders.add(provider);
      }
    }

    Map<String, Object> output = new TreeMap<>();
    for (CodeGenerator<?> provider : testedProviders) {
      Map<String, ? extends GeneratedResult<?>> out = provider.generate();

      if (!Collections.disjoint(out.keySet(), output.keySet())) {
        throw new IllegalStateException("file conflict");
      }
      for (Map.Entry<String, ? extends GeneratedResult<?>> entry : out.entrySet()) {
        Object value =
            (entry.getValue().getBody() instanceof byte[])
                ? "Static or binary file content is not shown."
                : entry.getValue().getBody();
        output.put(entry.getKey(), value);
      }
    }

    return output;
  }

  private static boolean hasSmokeTestConfig(GapicProductConfig productConfig) {
    return productConfig
        .getInterfaceConfigMap()
        .values()
        .stream()
        .anyMatch(config -> config.getSmokeTestConfig() != null);
  }
}
