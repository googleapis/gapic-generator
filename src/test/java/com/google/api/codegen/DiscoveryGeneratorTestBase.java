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

import com.google.api.codegen.discovery.DiscoveryProvider;
import com.google.api.codegen.discovery.MainDiscoveryProviderFactory;
import com.google.api.tools.framework.model.SimpleDiagCollector;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.io.Files;
import com.google.protobuf.Api;
import com.google.protobuf.MessageOrBuilder;
import com.google.protobuf.Method;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
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
  private final String[] gapicConfigFileNames;
  protected ConfigProto config;
  protected DiscoveryImporter discoveryImporter;

  public DiscoveryGeneratorTestBase(
      String name, String discoveryDocFileName, String[] gapicConfigFileNames) {
    this.name = name;
    this.discoveryDocFileName = discoveryDocFileName;
    this.gapicConfigFileNames = gapicConfigFileNames;
  }

  protected void setupDiscovery() {
    try {
      discoveryImporter =
          DiscoveryImporter.parse(
              new StringReader(
                  getTestDataLocator()
                      .readTestData(getTestDataLocator().findTestData(discoveryDocFileName))));
    } catch (IOException e) {
      throw new IllegalArgumentException("Problem creating Generator", e);
    }

    config =
        CodegenTestUtil.readConfig(
            new SimpleDiagCollector(), getTestDataLocator(), gapicConfigFileNames);
    if (config == null) {
      return;
    }
  }

  @Override
  protected Object run() {
    GeneratorProto generator = config.getGenerator();

    String factory = generator.getFactory();
    String id = generator.getId();

    DiscoveryProvider provider =
        MainDiscoveryProviderFactory.defaultCreate(
            discoveryImporter.getService(), discoveryImporter.getConfig(), id);

    Doc output = Doc.EMPTY;
    for (Api api : discoveryImporter.getService().getApisList()) {
      for (Method method : api.getMethodsList()) {
        Map<String, Doc> docs = provider.generate(method);
        for (Doc doc : docs.values()) {
          output = Doc.joinWith(Doc.BREAK, output, doc);
        }
      }
    }
    return Doc.vgroup(output);
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
    return name + ".baseline";
  }

  static final class DiscoveryFile implements FileFilter {
    @Override
    public boolean accept(File file) {
      return file.isFile() && file.getName().endsWith(".json");
    }
  }
}
