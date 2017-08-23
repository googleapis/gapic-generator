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

import com.google.api.codegen.gapic.MainGapicProviderFactory;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Go code generator baseline tests. */
@RunWith(Parameterized.class)
public class GoCodeGeneratorTest extends GapicTestBase2 {

  private final String apiName;

  public GoCodeGeneratorTest(
      String idForFactory,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      List<String> snippetName,
      String apiName,
      String baseline) {
    super(idForFactory, gapicConfigFileNames, packageConfigFileName, snippetName, baseline);
    this.apiName = apiName;
    getTestDataLocator().addTestDataSource(getClass(), idForFactory);
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with the first
   * element a name, the second a config of this name.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    return Collections.singletonList(
        GapicTestBase2.createTestConfig(
            MainGapicProviderFactory.GO,
            new String[] {"go_gapic.yaml", "library_gapic.yaml"},
            null,
            "library"));
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test(apiName);
  }
}
