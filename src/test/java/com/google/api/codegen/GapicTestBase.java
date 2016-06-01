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
import com.google.api.codegen.MultiYamlReader;
import com.google.api.codegen.gapic.GapicProvider;
import com.google.api.codegen.gapic.GapicProviderFactory;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.protobuf.Message;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Base class for code generator baseline tests.
 */
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
      String name,
      String idForFactory,
      String[] gapicConfigFileNames,
      String snippetName) {
    this.name = name;
    this.idForFactory = idForFactory;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.snippetName = snippetName;
  }

  @Override
  protected Object run() {
    List<GeneratedResult> results = generate();
    Truth.assertThat(results).isNotNull();

    ImmutableMap.Builder<String, Doc> builder = new ImmutableMap.Builder<String, Doc>();
    for (GeneratedResult result : results) {
      builder.put(result.getFilename(), result.getDoc());
    }
    return builder.build();
  }

  @Override
  protected void setupModel() {
    super.setupModel();

    config = readConfig(model);
    // TODO (garrettjones) depend on the framework to take care of this.
    if (model.getErrorCount() > 0) {
      for (Diag diag : model.getDiags()) {
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

  private ConfigProto readConfig(DiagCollector diagCollector) {
    List<String> inputNames = new ArrayList<>();
    List<String> inputs = new ArrayList<>();

    for (String gapicConfigFileName : gapicConfigFileNames) {
      URL gapicConfigUrl = getTestDataLocator().findTestData(gapicConfigFileName);
      String configData = getTestDataLocator().readTestData(gapicConfigUrl);
      inputNames.add(gapicConfigFileName);
      inputs.add(configData);
    }

    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(
            ConfigProto.getDescriptor().getFullName(), ConfigProto.getDefaultInstance());
    ConfigProto configProto =
        (ConfigProto) MultiYamlReader.read(model, inputNames, inputs, supportedConfigTypes);

    return configProto;
  }

  /**
   * Creates the constructor arguments to be passed onto this class
   * (GapicTestBase) to create test methods. The idForFactory String is passed
   * to GapicProviderFactory to get the GapicProviders provided by that id, and
   * then the snippet file names are scraped from those providers, and a set of
   * arguments is created for each combination of GapicProvider x snippet that
   * GapicProviderFactory returns.
   */
  public static List<Object[]> createTestedConfigs(String idForFactory, String[] gapicConfigFileNames) {
    Model model = Model.create(Service.getDefaultInstance());
    ApiConfig apiConfig = ApiConfig.createDummyApiConfig();

    List<GapicProvider<? extends Object>> providers = GapicProviderFactory
        .defaultCreate(model, apiConfig, idForFactory);
    List<Object[]> testArgs = new ArrayList<>();
    for (GapicProvider<? extends Object> provider : providers) {
      for (String snippetFileName : provider.getSnippetFileNames()) {
        String[] fileNameParts = snippetFileName.split("\\.");
        String id = idForFactory + "_" + fileNameParts[0];
        testArgs.add(new Object[] {
            id,
            idForFactory,
            gapicConfigFileNames,
            snippetFileName
        });
      }
    }
    return testArgs;
  }


  private List<GeneratedResult> generate() {
    model.establishStage(Merged.KEY);
    if (model.getErrorCount() > 0) {
      for (Diag diag : model.getDiags()) {
        System.err.println(diag.toString());
      }
      return null;
    }

    ApiConfig apiConfig = ApiConfig.createApiConfig(model, config);
    if (apiConfig == null) {
      return null;
    }

    List<GapicProvider<? extends Object>> providers =
        GapicProviderFactory.defaultCreate(model, apiConfig, idForFactory);
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

    List<GeneratedResult> output =
        testedProvider.generateSnip(snippetName);
    if (output == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
    }

    TreeSet<GeneratedResult> results = new TreeSet<>(new GeneratedResultComparator());
    results.addAll(output);

    return new ArrayList<GeneratedResult>(results);
  }

}
