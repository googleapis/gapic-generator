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

import com.google.api.codegen.MultiYamlReader;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Message;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

/**
 * Base class for code generator baseline tests.
 */
public abstract class GeneratorTestBase extends ConfigBaselineTestCase {

  private static final Pattern BASELINE_PATTERN = Pattern.compile("(\\w+)\\[(\\w+)\\]");

  // Wiring
  // ======

  private final String name;
  private final String[] gapicConfigFileNames;
  private final String gapicProviderName;
  private final String snippetName;
  private String viewName;
  protected ConfigProto config;

  public GeneratorTestBase(
      String name,
      String[] gapicConfigFileNames,
      String gapicProviderName,
      String viewName,
      String snippetName) {
    this.name = name;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.gapicProviderName = gapicProviderName;
    this.viewName = viewName;
    this.snippetName = snippetName;
  }

  public GeneratorTestBase(String name, String[] gapicConfigFileNames) {
    this(name, gapicConfigFileNames, null, null, null);
  }

  @Override
  protected void setupModel() {
    super.setupModel();

    config = readConfig(model);
    // TODO (garrettjones) depend on the framework to take care of this
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
  @Nullable
  protected abstract Object run() throws Exception;

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

    if (gapicProviderName != null && snippetName != null && viewName != null) {
      // Filtering can be made more sophisticated later if required
      TemplateProto template =
          TemplateProto.newBuilder()
              .setCodegenProvider(gapicProviderName)
              .setInputElementView(viewName)
              .addSnippetFiles(snippetName)
              .build();
      configProto = configProto.toBuilder().clearTemplates().addTemplates(template).build();
    }

    return configProto;
  }
}
