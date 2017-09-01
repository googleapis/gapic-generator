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

import com.google.api.codegen.config.LanguageStrings;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Go code generator baseline tests. */
@RunWith(Parameterized.class)
public class GapicCodeGeneratorTest extends GapicTestBase2 {

  private final String apiName;

  public GapicCodeGeneratorTest(
      String idForFactory,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      List<String> snippetName,
      String apiName,
      String baseline) {
    super(idForFactory, gapicConfigFileNames, packageConfigFileName, snippetName, baseline);
    this.apiName = apiName;
    getTestDataLocator().addTestDataSource(getClass(), idForFactory);
    getTestDataLocator().addTestDataSource(getClass(), "testdata/" + idForFactory);
  }

  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    return Arrays.asList(
        GapicTestBase2.createTestConfig(
            LanguageStrings.GO,
            new String[] {"go_gapic.yaml", "library_gapic.yaml"},
            null,
            "library"),
        GapicTestBase2.createTestConfig(
            LanguageStrings.PHP,
            new String[] {"php_gapic.yaml", "library_gapic.yaml"},
            "library_pkg.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            LanguageStrings.JAVA,
            new String[] {"java_gapic.yaml", "library_gapic.yaml"},
            "library_pkg.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            LanguageStrings.JAVA,
            new String[] {"java_gapic.yaml", "no_path_templates_gapic.yaml"},
            "no_path_templates_pkg.yaml",
            "no_path_templates"));
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test(apiName);
  }
}
