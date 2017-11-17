/* Copyright 2016 Google LLC
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

import com.google.api.tools.framework.model.testing.TestConfig;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.api.tools.framework.model.testing.TestModelGenerator;
import java.util.List;
import org.junit.rules.TemporaryFolder;

public class GapicTestModelGenerator extends TestModelGenerator {

  public GapicTestModelGenerator(TestDataLocator testDataLocator, TemporaryFolder tempDir) {
    super(testDataLocator, tempDir);
  }

  @Override
  public TestConfig createTestConfig(String tempDir, List<String> protoFiles) {
    return new GapicTestConfig(getTestDataLocator(), tempDir, protoFiles);
  }
}
