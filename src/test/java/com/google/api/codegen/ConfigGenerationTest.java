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

import com.google.api.codegen.configgen.GapicConfigGeneratorApp;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;

public class ConfigGenerationTest extends ConfigBaselineTestCase {

  private final TestDataLocator testDataLocator =
      new MixedPathTestDataLocator(this.getClass(), Paths.get("src", "test", "java").toString());

  @Override
  protected TestDataLocator getTestDataLocator() {
    return this.testDataLocator;
  }

  @Override
  protected String baselineFileName() {
    return testName.getMethodName() + "_config.baseline";
  }

  @Override
  protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  @Override
  public Object run() throws Exception {
    String outFile = tempDir.getRoot().getPath() + File.separator + baselineFileName();
    String serviceConfigPath =
        getTestDataLocator().findTestData(testName.getMethodName() + ".yaml").getPath();

    ToolOptions options = ToolOptions.create();
    options.set(GapicConfigGeneratorApp.OUTPUT_FILE, outFile);
    options.set(ToolOptions.DESCRIPTOR_SET, testConfig.getDescriptorFile().toString());
    options.set(ToolOptions.CONFIG_FILES, Lists.newArrayList(serviceConfigPath));
    new GapicConfigGeneratorApp(options).run();

    return new String(Files.readAllBytes(Paths.get(outFile)), StandardCharsets.UTF_8);
  }

  @Before
  public void setup() {
    getTestDataLocator().addTestDataSource(getClass(), "testsrc/common");
    getTestDataLocator().addTestDataSource(getClass(), "testsrc/libraryproto/config_not_annotated");
  }

  @Test
  public void library() throws Exception {
    test("library");
  }

  @Test
  public void no_path_templates() throws Exception {
    test("no_path_templates");
  }

  @Test
  public void longrunning() throws Exception {
    test("longrunning");
  }

  @Test
  public void multiple_services() throws Exception {
    test("multiple_services");
  }
}
