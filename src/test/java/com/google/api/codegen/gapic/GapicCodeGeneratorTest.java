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
package com.google.api.codegen.gapic;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.common.TargetLanguage;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Code generator baseline tests. Tests generation using config files. */
@RunWith(Parameterized.class)
public class GapicCodeGeneratorTest extends GapicTestBase2 {

  private final String[] baseNames;

  public GapicCodeGeneratorTest(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      List<String> snippetName,
      String baseline,
      String protoPackage,
      String clientPackage,
      String[] baseNames) {
    super(
        language,
        gapicConfigFileNames,
        packageConfigFileName,
        snippetName,
        baseline,
        protoPackage,
        clientPackage);
    this.baseNames = baseNames;
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
  }

  @Parameters(name = "{4}")
  public static List<Object[]> testedConfigs() {
    return Arrays.asList(
        GapicTestBase2.createTestConfig(
            TargetLanguage.GO, new String[] {"library_gapic.yaml"}, null, "library"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"), // Test passing in a proto_package flag.
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"longrunning_gapic.yaml"},
            "longrunning_pkg2.yaml",
            "longrunning"), // Test passing in a proto_package flag.
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services",
            "multiple_services_v2"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"longrunning_gapic.yaml"},
            "longrunning_pkg2.yaml",
            "longrunning"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services",
            "multiple_services_v2"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"no_path_templates_gapic.yaml"},
            "library_pkg2.yaml",
            "no_path_templates"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services",
            "multiple_services_v2"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.CSHARP,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library"));
  }

  @Test
  public void test() throws Exception {
    test(baseNames);
  }
}
