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

import com.google.api.codegen.configgen.DiscoConfigGeneratorApp;
import com.google.api.tools.framework.tools.ToolOptions;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.Test;

public class DiscoConfigGenerationTest extends DiscoConfigBaselineTestCase {

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
  public String run() throws Exception {
    String outFile = tempDir.getRoot().getPath() + File.separator + baselineFileName();
    String discoveryFile = getTestDataLocator().findTestData(discoveryFileName).getFile();
    ToolOptions options = ToolOptions.create();
    options.set(DiscoConfigGeneratorApp.OUTPUT_FILE, outFile);
    options.set(DiscoConfigGeneratorApp.DISCOVERY_DOC, discoveryFile);
    new DiscoConfigGeneratorApp(options).run();

    return new String(Files.readAllBytes(Paths.get(outFile)), StandardCharsets.UTF_8);
  }

  @Before
  public void setup() {
    getTestDataLocator().addTestDataSource(getClass(), "testdata");
  }

  @Test
  public void simplecompute() throws Exception {
    discoveryFileName = "simplecompute.v1.json";
    test();
  }
}
