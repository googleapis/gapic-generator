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
package io.gapi.vgen;

import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.model.testing.SimpleDiag;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.protobuf.Message;
import com.google.protobuf.MessageOrBuilder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Base class for code generator baseline tests.
 */
public abstract class DiscoveryGeneratorTestBase extends ConfigBaselineTestCase {

  private static final Pattern BASELINE_PATTERN = Pattern.compile("(\\w+)\\[(\\w+)\\]");

  // Wiring
  // ======

  private final String name;
  private final String discoveryDocFileName;
  private final String[] veneerConfigFileNames;
  private final String snippetName;
  protected ConfigProto config;
  protected DiscoveryImporter discoveryImporter;

  public DiscoveryGeneratorTestBase(String name, String discoveryDocFileName,
      String[] veneerConfigFileNames, String snippetName) {
    this.name = name;
    this.discoveryDocFileName = discoveryDocFileName;
    this.veneerConfigFileNames = veneerConfigFileNames;
    this.snippetName = snippetName;
  }

  public DiscoveryGeneratorTestBase(String name, String discoveryDocFileName,
      String[] veneerConfigFileNames) {
    this(name, discoveryDocFileName, veneerConfigFileNames, null);
  }

  protected void setupDiscovery() {
    try {
      discoveryImporter = DiscoveryImporter.parse(
          Files.newReader(
              new File(getTestDataLocator().getTestDataAsFile(discoveryDocFileName).toString()),
              Charset.forName("UTF8")));
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem creating Generator", e);
    }

    config = readConfig();
    if (config == null) {
      return;
    }
  }

  @Override
  protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  protected void test() throws Exception {
    // Setup
    setupDiscovery();

    // Run test specific logic.
    Object result = run();

    testOutput().println(displayValue(result));
  }

  private String displayValue(Object value) throws IOException {
    if (value instanceof Doc) {
      return ((Doc) value).prettyPrint(100);
    } else if (value instanceof File) {
      return Files.toString((File) value, StandardCharsets.UTF_8);
    } else if (value instanceof MessageOrBuilder) {
      // Convert proto to text format, considering any instances.
      return formatter.printToString((MessageOrBuilder) value);
    } else {
      return value.toString();
    }
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

  private ConfigProto readConfig() {
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
    // Use DiagCollector to collect errors from config read since user errors may arise here
    DiagCollector diagCollector = new SimpleDiag();
    ConfigProto configProto =
        (ConfigProto) MultiYamlReader.read(diagCollector,
            inputNames, inputs, supportedConfigTypes);
    if (diagCollector.getErrorCount() > 0) {
      System.err.println(diagCollector.toString());
      return null;
    }

    if (snippetName != null) {
      // Filtering can be made more sophisticated later if required
      configProto = configProto.toBuilder()
          .clearSnippetFiles().addSnippetFiles(snippetName)
          .build();
    }

    return configProto;
  }
}
