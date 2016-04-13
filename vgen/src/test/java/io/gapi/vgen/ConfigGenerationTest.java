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

import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.testing.TestConfig;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.api.tools.framework.tools.ToolOptions;

import io.gapi.vgen.config.ConfigGeneratorApi;

import java.io.File;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;

import org.junit.Rule;
import org.junit.rules.TemporaryFolder;
import org.junit.Test;

public class ConfigGenerationTest {
  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void config() throws Exception {
    String testTarget = "library";
    String baselineName = testTarget + "_config.baseline";

    TestDataLocator locator = TestDataLocator.create(this.getClass());
    TestConfig testConfig =
        new TestConfig(
            locator, tempDir.getRoot().getPath(), Collections.singletonList(testTarget + ".proto"));
    Model model = Model.create(testConfig.getDescriptor());

    String outFile = tempDir.getRoot().getPath() + File.separator + baselineName;

    ToolOptions options = ToolOptions.create();
    options.set(ConfigGeneratorApi.OUTPUT_FILE, outFile);
    options.set(ToolOptions.DESCRIPTOR_SET, testConfig.getDescriptorFile().toString());
    new ConfigGeneratorApi(options).run();

    URL baselineUrl = locator.findTestData(baselineName);
    if (baselineUrl == null) {
      throw new IllegalStateException(
          String.format("baseline not found: %s", baselineName));
    }
    String baselineContent = locator.readTestData(baselineUrl);
    String outputContent =
        new String(Files.readAllBytes(Paths.get(outFile)), StandardCharsets.UTF_8);

    if (!outputContent.equals(baselineContent)) {
      Path saveFile =
          Paths.get(
              System.getProperty("java.io.tmpdir"),
              String.format("%s_testdata", this.getClass().getPackage().getName()),
              baselineName);
      Files.createDirectories(saveFile.getParent());
      Files.copy(Paths.get(outFile), saveFile, StandardCopyOption.REPLACE_EXISTING);
      throw new IllegalStateException(String.format("baseline failed, output saved at %s", saveFile));
    }
  }
}
