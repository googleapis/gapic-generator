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
package com.google.api.codegen.gapic;

import com.google.api.Service;
import com.google.api.codegen.ArtifactType;
import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.MixedPathTestDataLocator;
import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.ApiDefaultsConfig;
import com.google.api.codegen.config.DependenciesConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.PackagingConfig;
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.grpc.ServiceConfig;
import com.google.api.codegen.samplegen.v1p2.SampleConfigProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
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
  private final String[] sampleConfigFileNames;
  @Nullable private final String packageConfigFileName;
  private final ImmutableList<String> snippetNames;
  private ApiDefaultsConfig apiDefaultsConfig;
  private DependenciesConfig dependenciesConfig;
  private PackagingConfig packagingConfig;
  protected ConfigProto gapicConfig;
  protected SampleConfigProto sampleConfig;
  private final String baselineFile;
  private final String protoPackage;
  private final String clientPackage;
  private final TestDataLocator testDataLocator = MixedPathTestDataLocator.create(this.getClass());
  private final String grpcServiceConfigFileName;
  private ServiceConfig grpcServiceConfig;

  public GapicTestBase2(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String[] sampleConfigFileNames,
      String packageConfigFileName,
      List<String> snippetNames,
      String baselineFile,
      String protoPackage,
      String clientPackage,
      String grpcServiceConfigFileName) {
    this.language = language;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.sampleConfigFileNames = sampleConfigFileNames;
    this.packageConfigFileName = packageConfigFileName;
    this.snippetNames = ImmutableList.copyOf(snippetNames);
    this.baselineFile = baselineFile;
    this.clientPackage = clientPackage;
    this.grpcServiceConfigFileName = grpcServiceConfigFileName;

    // Represents the test value for the --package flag.
    this.protoPackage = protoPackage;

    String dir = language.toString().toLowerCase();
    if ("python".equals(dir)) {
      dir = "py";
    }
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, dir);
    getTestDataLocator().addTestDataSource(getClass(), "testdata/" + dir);
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
  }

  @Override
  protected TestDataLocator getTestDataLocator() {
    return this.testDataLocator;
  }

  @Override
  protected void test(String... baseNames) throws Exception {
    super.test(new GapicTestModelGenerator(getTestDataLocator(), tempDir), baseNames);
  }

  @Override
  protected void setupModel() {
    super.setupModel();
    if (gapicConfigFileNames != null) {
      gapicConfig =
          CodegenTestUtil.readConfig(
              model.getDiagReporter().getDiagCollector(),
              getTestDataLocator(),
              gapicConfigFileNames);
    }

    if (sampleConfigFileNames != null) {
      sampleConfig =
          CodegenTestUtil.readSampleConfig(
              model.getDiagReporter().getDiagCollector(),
              getTestDataLocator(),
              sampleConfigFileNames);
    }
    try {
      apiDefaultsConfig = ApiDefaultsConfig.load();
      dependenciesConfig =
          DependenciesConfig.loadFromURL(
              getTestDataLocator().findTestData("frozen_dependencies.yaml"));
      if (!Strings.isNullOrEmpty(packageConfigFileName)) {
        packagingConfig =
            PackagingConfig.loadFromURL(getTestDataLocator().findTestData(packageConfigFileName));
      } else {
        packagingConfig = null;
      }
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem creating packageConfig");
    }

    if (!Strings.isNullOrEmpty(grpcServiceConfigFileName)) {
      grpcServiceConfig =
          CodegenTestUtil.readGRPCServiceConfig(
              model.getDiagReporter().getDiagCollector(),
              testDataLocator,
              grpcServiceConfigFileName);
    }

    // TODO (garrettjones) depend on the framework to take care of this.
    if (model.getDiagReporter().getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagReporter().getDiagCollector().getDiags()) {
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
   * methods. The language is passed to GapicGeneratorFactory to get the GapicGenerators provided by
   * that language, and then the snippet file names are scraped from those generators, and a set of
   * arguments is created for each combination of CodeGenerator x snippet that GapicGeneratorFactory
   * returns.
   */
  static Object[] createTestConfig(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      String apiName,
      String grpcServiceConfigFileName,
      String... baseNames) {
    return createTestConfig(
        language,
        gapicConfigFileNames,
        packageConfigFileName,
        apiName,
        null,
        null,
        grpcServiceConfigFileName,
        null,
        null,
        baseNames);
  }

  /**
   * Creates the constructor arguments to be passed onto this class (GapicTestBase2) to create test
   * methods. The langauge String is passed to GapicGeneratorFactory to get the GapicGenerators
   * provided by that language, and then the snippet file names are scraped from those generators,
   * and a set of arguments is created for each combination of CodeGenerator x snippet that
   * GapicGeneratorFactory returns.
   */
  public static Object[] createTestConfig(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      String apiName,
      String protoPackage,
      String clientPackage,
      String grpcServiceConfigFileName,
      String[] sampleConfigFileNames,
      String baseline,
      String... baseNames) {
    Model model = Model.create(Service.getDefaultInstance());
    GapicProductConfig productConfig = GapicProductConfig.createDummyInstance();
    PackageMetadataConfig packageConfig = PackageMetadataConfig.createDummyPackageMetadataConfig();
    ArtifactFlags artifactFlags =
        new ArtifactFlags(
            Arrays.asList("surface", "test", "samples"),
            ArtifactType.LEGACY_GAPIC_AND_PACKAGE,
            true);

    List<CodeGenerator<?>> generators =
        GapicGeneratorFactory.create(language, model, productConfig, packageConfig, artifactFlags);

    List<String> snippetNames = new ArrayList<>();
    for (CodeGenerator<?> generator : generators) {
      snippetNames.addAll(generator.getInputFileNames());
    }

    // The name of the baseline file to compare the generated code with. If not given, an
    // autogenerated name will be provided.
    if (baseline == null) {
      StringBuilder suffix = new StringBuilder();
      if (gapicConfigFileNames == null || gapicConfigFileNames.length == 0) {
        suffix.append("_no_gapic_config");
      }

      if (!Strings.isNullOrEmpty(grpcServiceConfigFileName)) {
        suffix.append("_with_grpc_service_config");
      }

      baseline = language.toString().toLowerCase() + "_" + apiName + suffix + ".baseline";
    }
    baseNames = Lists.asList(apiName, baseNames).toArray(new String[0]);

    return new Object[] {
      language,
      gapicConfigFileNames,
      sampleConfigFileNames,
      packageConfigFileName,
      snippetNames,
      baseline,
      protoPackage,
      clientPackage,
      grpcServiceConfigFileName,
      baseNames
    };
  }

  @Override
  protected String baselineFileName() {
    return baselineFile;
  }

  @Override
  public Map<String, ?> run() throws IOException {
    model.establishStage(Merged.KEY);
    if (model.getDiagReporter().getDiagCollector().getErrorCount() > 0) {
      for (Diag diag : model.getDiagReporter().getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }
    if (sampleConfig == null) {
      sampleConfig = SampleConfigProto.getDefaultInstance();
    }
    GapicProductConfig productConfig =
        GapicProductConfig.create(
            model,
            gapicConfig,
            sampleConfig,
            protoPackage,
            clientPackage,
            language,
            grpcServiceConfig,
            TransportProtocol.GRPC);

    if (productConfig == null) {
      for (Diag diag : model.getDiagReporter().getDiagCollector().getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    List<String> enabledArtifacts = new ArrayList<>();
    if (hasSmokeTestConfig(productConfig)) {
      enabledArtifacts.addAll(Arrays.asList("surface", "test", "samples"));
    }
    ArtifactFlags artifactFlags =
        new ArtifactFlags(enabledArtifacts, ArtifactType.LEGACY_GAPIC_AND_PACKAGE, true);

    PackagingConfig actualPackagingConfig = packagingConfig;
    if (actualPackagingConfig == null) {
      actualPackagingConfig =
          PackagingConfig.loadFromProductConfig(productConfig.getInterfaceConfigMap());
    }
    PackageMetadataConfig packageConfig =
        PackageMetadataConfig.createFromPackaging(
            apiDefaultsConfig, dependenciesConfig, actualPackagingConfig);

    List<CodeGenerator<?>> generators =
        GapicGeneratorFactory.create(language, model, productConfig, packageConfig, artifactFlags);

    // Don't run any generators we're not testing.
    ArrayList<CodeGenerator<?>> testedGenerators = new ArrayList<>();
    for (CodeGenerator<?> generator : generators) {
      if (!Collections.disjoint(generator.getInputFileNames(), snippetNames)) {
        testedGenerators.add(generator);
      }
    }

    Map<String, Object> output = new TreeMap<>();
    for (CodeGenerator<?> generator : testedGenerators) {
      Map<String, ? extends GeneratedResult<?>> out = generator.generate();

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
