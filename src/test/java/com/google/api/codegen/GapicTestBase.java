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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

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
  @Nullable private final String packageConfigFileName;
  private final String snippetName;
  protected ConfigProto gapicConfig;
  protected PackageMetadataConfig packageConfig;

  public GapicTestBase(
      String name, String idForFactory, String[] gapicConfigFileNames, String snippetName) {
    this(name, idForFactory, gapicConfigFileNames, null, snippetName);
  }

  public GapicTestBase(
      String name,
      String idForFactory,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      String snippetName) {
    this.name = name;
    this.idForFactory = idForFactory;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.packageConfigFileName = packageConfigFileName;
    this.snippetName = snippetName;

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
      String idForFactory, String[] gapicConfigFileNames, String packageConfigFileName) {
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
    List<Object[]> testArgs = new ArrayList<>();
    for (GapicProvider<? extends Object> provider : providers) {
      for (String snippetFileName : provider.getSnippetFileNames()) {
        String fileNamePath = snippetFileName.split("\\.")[0];
        String fileName =
            fileNamePath.indexOf("/") > 0
                ? fileNamePath.split("/", 2)[1].replace("/", "_")
                : fileNamePath;
        String id = idForFactory + "_" + fileName;
        testArgs.add(
            new Object[] {
              id, idForFactory, gapicConfigFileNames, packageConfigFileName, snippetFileName
            });
      }
    }
    return testArgs;
  }

  public static List<Object[]> createTestedConfigs(
      String idForFactory, String[] gapicConfigFileNames) {
    return createTestedConfigs(idForFactory, gapicConfigFileNames, null);
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

    List<String> enabledArtifacts = Arrays.asList();
    if (hasSmokeTestConfig(productConfig)) {
      enabledArtifacts = Arrays.asList("surface", "test", "sample_app");
    }

    GapicGeneratorConfig generatorConfig =
        GapicGeneratorConfig.newBuilder()
            .id(idForFactory)
            .enabledArtifacts(enabledArtifacts)
            .build();
    List<GapicProvider<? extends Object>> providers =
        MainGapicProviderFactory.defaultCreate(
            model, productConfig, generatorConfig, packageConfig, "");
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

    if (testedProvider != null) {
      Map<String, Doc> output = testedProvider.generate(snippetName);
      if (output == null) {
        // Report diagnosis to baseline file.
        for (Diag diag : model.getDiagCollector().getDiags()) {
          testOutput().println(diag.toString());
        }
      }

      return output;
    } else {
      testOutput().printf("%s generation is not enabled for this test case.\n", snippetName);
    }

    return null;
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
