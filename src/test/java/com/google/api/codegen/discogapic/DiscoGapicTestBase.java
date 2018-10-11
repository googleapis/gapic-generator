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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.ArtifactType;
import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.MixedPathTestDataLocator;
import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.tools.framework.model.SimpleDiagCollector;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.io.Files;
import com.google.protobuf.MessageOrBuilder;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** Base class for Discovery code generator baseline tests. */
public abstract class DiscoGapicTestBase extends ConfigBaselineTestCase {

  // Wiring
  // ======

  private final TargetLanguage language;
  private final String name;
  private final String discoveryDocFileName;
  private final List<String> gapicConfigFilePaths = new LinkedList<>();
  private final String[] gapicConfigFileNames;
  @Nullable private final String packageConfigFileName;
  protected ConfigProto config;
  private List<CodeGenerator<?>> discoGapicGenerators;
  private final TestDataLocator testDataLocator =
      new MixedPathTestDataLocator(this.getClass(), Paths.get("src", "test", "java").toString());

  public DiscoGapicTestBase(
      TargetLanguage language,
      String name,
      String discoveryDocFileName,
      String[] gapicConfigFileNames,
      String packageConfigFileName) {
    this.language = language;
    this.name = name;
    this.discoveryDocFileName = discoveryDocFileName;
    this.gapicConfigFileNames = gapicConfigFileNames;
    this.packageConfigFileName = packageConfigFileName;

    String dir = language.toString().toLowerCase();
    getTestDataLocator().addTestDataSource(getClass(), "testdata");
    getTestDataLocator().addTestDataSource(getClass(), "testdata/" + dir);
  }

  @Override
  protected TestDataLocator getTestDataLocator() {
    return this.testDataLocator;
  }

  protected void setupDiscovery() {
    for (String fileName : gapicConfigFileNames) {
      this.gapicConfigFilePaths.add(getTestDataLocator().findTestData(fileName).getFile());
    }

    try {
      discoGapicGenerators =
          DiscoGapicGeneratorApp.getGenerators(
              getTestDataLocator().findTestData(discoveryDocFileName).getPath(),
              gapicConfigFilePaths,
              getTestDataLocator().findTestData(packageConfigFileName).getPath(),
              getTestDataLocator()
                  .findTestData("com/google/api/codegen/testsrc/common/frozen_dependencies.yaml")
                  .getPath(),
              language.toString().toLowerCase(),
              Collections.emptyList(),
              ArtifactType.LEGACY_DISCOGAPIC_AND_PACKAGE);
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem creating DiscoGapic generator.", e);
    }

    config =
        CodegenTestUtil.readConfig(
            new SimpleDiagCollector(), getTestDataLocator(), gapicConfigFileNames);
    if (config == null) {
      throw new IllegalArgumentException("Problem fetching or parsing Gapic config files.");
    }
  }

  @Override
  protected Map<String, ?> run() throws IOException {
    Map<String, Object> output = new LinkedHashMap<>();

    for (CodeGenerator<?> generator : discoGapicGenerators) {
      Map<String, ? extends GeneratedResult<?>> out = generator.generate();
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

    if (result instanceof Map) {
      @SuppressWarnings("unchecked")
      Map<String, Object> map = (Map<String, Object>) result;
      for (Map.Entry<String, Object> entry : map.entrySet()) {
        testOutput().printf("============== file: %s ==============%n", entry.getKey());
        testOutput().println(displayValue(entry.getValue()));
      }
    } else {
      testOutput().println(displayValue(result));
    }
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
    return name + ".baseline";
  }

  static final class DiscoveryFile implements FileFilter {
    private String api;

    public DiscoveryFile(String api) {
      this.api = api;
    }

    @Override
    public boolean accept(File file) {
      return file.isFile() && file.getName().endsWith(api + ".v1.json");
    }
  }
}
