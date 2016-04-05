/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http: *www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;
import com.google.protobuf.Message;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for code generator baseline tests.
 */
@RunWith(Parameterized.class)
public abstract class CodeGeneratorTestBase extends ConfigBaselineTestCase {

  private static final Pattern BASELINE_PATTERN = Pattern.compile("(\\w+)\\[(\\w+)\\]");

  // Wiring
  // ======

  private final String name;
  private final String[] veneerConfigFileNames;
  private final String snippetName;
  private ConfigProto config;
  private CodeGenerator generator;

  public CodeGeneratorTestBase(String name, String[] veneerConfigFileNames, String snippetName) {
    this.name = name;
    this.veneerConfigFileNames = veneerConfigFileNames;
    this.snippetName = snippetName;
  }

  public CodeGeneratorTestBase(String name, String[] veneerConfigFileNames) {
    this(name, veneerConfigFileNames, null);
  }

  @Override protected void setupModel() {
    super.setupModel();

    setupModelImpl();
    // TODO (garrettjones) depend on the framework to take care of this
    if (model.getErrorCount() > 0) {
      for (Diag diag : model.getDiags()) {
        System.err.println(diag.toString());
      }
      throw new IllegalArgumentException("Problem creating CodeGenerator");
    }
  }

  private void setupModelImpl() {
    config = readConfig(model);
    if (config == null) {
      return;
    }

    generator =
        new CodeGenerator.Builder()
            .setConfigProto(config)
            .setModel(model)
            .build();
    if (generator == null) {
      return;
    }
  }

  @Override protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  protected GeneratedResult generateForSnippet(int index) {
    Map<?, GeneratedResult> result = generateForSnippet(
        config.getSnippetFilesList(), index, false);
    Truth.assertThat(result.size()).isEqualTo(1);
    return result.values().iterator().next();
  }

  protected List<GeneratedResult> generateForDocSnippet(int index) {
    TreeMap<String, GeneratedResult> result = new TreeMap(
        (Map<String, GeneratedResult>) generateForSnippet(
            config.getDocSnippetFilesList(), index, true));
    return new ArrayList(result.values());
  }

  private Map<?, GeneratedResult> generateForSnippet(List<String> snippetInputNames, int index,
      boolean doc) {
    if (index >= snippetInputNames.size()) {
      return null;
    }
    String snippetInputName = snippetInputNames.get(index);
    SnippetDescriptor resourceDescriptor =
          new SnippetDescriptor(snippetInputName);
    Map<?, GeneratedResult> result = null;
    if (doc) {
      result = generator.generateDocs(resourceDescriptor);
    } else {
      result = generator.generate(resourceDescriptor);
    }
    if (result == null) {
      // Report diagnosis to baseline file.
      for (Diag diag : model.getDiags()) {
        testOutput().println(diag.toString());
      }
      return null;
    }
    return result;
  }

  @Override
  protected Object run() {
    Truth.assertThat(this.generator).isNotNull();
    GeneratedResult result = generateForSnippet(0);
    Truth.assertThat(result).isNotNull();
    return result.getDoc();
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

    for (String veneerConfigFileName : veneerConfigFileNames) {
      URL veneerConfigUrl = getTestDataLocator().findTestData(veneerConfigFileName);
      String configData = getTestDataLocator().readTestData(veneerConfigUrl);
      inputNames.add(veneerConfigFileName);
      inputs.add(configData);
    }

    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(ConfigProto.getDescriptor().getFullName(),
            ConfigProto.getDefaultInstance());
    ConfigProto configProto =
        (ConfigProto) MultiYamlReader.read(model, inputNames, inputs, supportedConfigTypes);

    if (snippetName != null) {
      // Filtering can be made more sophisticated later if required
      configProto = configProto.toBuilder()
          .clearSnippetFiles().addSnippetFiles(snippetName)
          .build();
    }

    return configProto;
  }
}
