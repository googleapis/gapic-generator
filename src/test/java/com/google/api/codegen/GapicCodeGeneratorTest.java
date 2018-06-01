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

import com.google.api.codegen.gapic.GapicProviderFactory;
import com.google.common.collect.ImmutableMultimap;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Go code generator baseline tests. */
@RunWith(Parameterized.class)
public class GapicCodeGeneratorTest extends GapicTestBase2 {

  private static final ImmutableMultimap<String, String> TEST_DIR =
      ImmutableMultimap.<String, String>builder()
          .put(GapicProviderFactory.GO, GapicProviderFactory.GO)
          .put(GapicProviderFactory.PHP, GapicProviderFactory.PHP)
          .put(GapicProviderFactory.JAVA, GapicProviderFactory.JAVA)
          .put(GapicProviderFactory.RUBY, GapicProviderFactory.RUBY)
          .put(GapicProviderFactory.RUBY_DOC, GapicProviderFactory.RUBY)
          .put(GapicProviderFactory.PYTHON, GapicProviderFactory.PYTHON)
          .put(GapicProviderFactory.NODEJS, GapicProviderFactory.NODEJS)
          .put(GapicProviderFactory.NODEJS_DOC, GapicProviderFactory.NODEJS)
          .put(GapicProviderFactory.CSHARP, GapicProviderFactory.CSHARP)
          .put(GapicProviderFactory.CLIENT_CONFIG, "clientconfig")
          .build();

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
    for (String dir : TEST_DIR.get(idForFactory)) {
      getTestDataLocator().addTestDataSource(getClass(), dir);
      getTestDataLocator().addTestDataSource(getClass(), "testdata/" + dir);
    }
  }

  @Parameters(name = "{5}")
  public static List<Object[]> testedConfigs() {
    return Arrays.asList(
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.GO,
            new String[] {"go_gapic.yaml", "library_gapic.yaml"},
            null,
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.PHP,
            new String[] {"php_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.PHP,
            new String[] {"php_gapic.yaml", "longrunning_gapic.yaml"},
            "longrunning_pkg2.yaml",
            "longrunning"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.PHP,
            new String[] {"php_gapic.yaml", "no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.JAVA,
            new String[] {"java_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.JAVA,
            new String[] {"java_gapic.yaml", "no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.RUBY,
            new String[] {"ruby_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.RUBY_DOC,
            new String[] {"ruby_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.RUBY,
            new String[] {"ruby_gapic.yaml", "multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.RUBY,
            new String[] {"ruby_gapic.yaml", "longrunning_gapic.yaml"},
            "longrunning_pkg2.yaml",
            "longrunning"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.PYTHON,
            new String[] {"python_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.PYTHON,
            new String[] {"python_gapic.yaml", "no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.PYTHON,
            new String[] {"python_gapic.yaml", "multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.NODEJS,
            new String[] {"nodejs_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.NODEJS_DOC,
            new String[] {"nodejs_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.NODEJS,
            new String[] {"nodejs_gapic.yaml", "no_path_templates_gapic.yaml"},
            "library_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.NODEJS,
            new String[] {"nodejs_gapic.yaml", "multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.CSHARP,
            new String[] {"csharp_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            GapicProviderFactory.CLIENT_CONFIG,
            new String[] {"client_gapic.yaml", "library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"));
  }

  @Test
  public void test() throws Exception {
    test(apiName);
  }
}
