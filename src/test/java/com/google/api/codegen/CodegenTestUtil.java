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

import com.google.api.codegen.configgen.ConfigHelper;
import com.google.api.codegen.configgen.ConfigYamlReader;
import com.google.api.codegen.configgen.MessageGenerator;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.gapic.GapicTestConfig;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.TestConfig;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.api.tools.framework.setup.StandardSetup;
import java.net.URL;
import java.util.Arrays;
import java.util.Objects;
import org.junit.rules.TemporaryFolder;

public class CodegenTestUtil {

  public static Model readModel(
      TestDataLocator locator, TemporaryFolder tempDir, String[] protoFiles, String[] yamlFiles) {
    TestConfig testConfig =
        new GapicTestConfig(locator, tempDir.getRoot().getPath(), Arrays.asList(protoFiles));
    Model model = testConfig.createModel(Arrays.asList(yamlFiles));
    StandardSetup.registerStandardProcessors(model);
    StandardSetup.registerStandardConfigAspects(model);
    model.establishStage(Merged.KEY);
    return model;
  }

  public static ConfigProto readConfig(
      DiagCollector diagCollector, TestDataLocator testDataLocator, String[] gapicConfigFileNames) {
    ConfigYamlReader yamlReader = new ConfigYamlReader();
    MessageGenerator messageGenerator = new MessageGenerator(ConfigProto.newBuilder());
    for (String gapicConfigFileName : gapicConfigFileNames) {
      URL gapicConfigUrl = testDataLocator.findTestData(gapicConfigFileName);

      String gapicConfigPath = Objects.requireNonNull(gapicConfigUrl).getPath();
      String actualFileName = gapicConfigPath.substring(gapicConfigPath.lastIndexOf('/') + 1);

      ConfigHelper helper = new ConfigHelper(diagCollector, actualFileName);
      ConfigNode configNode = yamlReader.generateConfigNode(gapicConfigUrl, helper);
      if (configNode == null) {
        continue;
      }

      messageGenerator.visit(configNode.getChild());
    }

    if (diagCollector.getErrorCount() > 0) {
      System.err.println(diagCollector.toString());
      return null;
    }

    return (ConfigProto) messageGenerator.getValue();
  }
}
