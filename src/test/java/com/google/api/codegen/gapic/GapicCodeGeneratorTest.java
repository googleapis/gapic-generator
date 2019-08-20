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
      String[] sampleConfigFileNames,
      String packageConfigFileName,
      List<String> snippetName,
      String baseline,
      String protoPackage,
      String clientPackage,
      String grpcServiceConfigFileName,
      String[] baseNames) {
    super(
        language,
        gapicConfigFileNames,
        sampleConfigFileNames,
        packageConfigFileName,
        snippetName,
        baseline,
        protoPackage,
        clientPackage,
        grpcServiceConfigFileName);
    this.baseNames = baseNames;
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common/samples");
  }

  @Parameters(name = "{5}")
  public static List<Object[]> testedConfigs() {
    return Arrays.asList(
        GapicTestBase2.createTestConfig(
            TargetLanguage.GO,
            new String[] {"library_gapic.yaml"},
            null,
            "library",
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            "another_service"), // Test passing in a proto_package flag.
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"longrunning_gapic.yaml"},
            "longrunning_pkg2.yaml",
            "longrunning",
            null), // Test passing in a proto_package flag.
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates",
            null),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"samplegen_config_migration_library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            null,
            null,
            sampleConfigFileNames(),
            "php_samplegen_config_migration_library.baseline",
            new String[] {"another_service"}),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"library_gapic.yaml"},
            null,
            "library",
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services",
            null,
            "multiple_services_v2"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates",
            null),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"my_streaming_proto_gapic.yaml"},
            "my_streaming_proto_pkg2.yaml",
            "my_streaming_proto",
            null),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"samplegen_config_migration_library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            null,
            null,
            sampleConfigFileNames(),
            "java_samplegen_config_migration_library.baseline",
            new String[] {"another_service"}),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"library_gapic.yaml"},
            null,
            "library",
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services",
            null,
            "multiple_services_v2"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"longrunning_gapic.yaml"},
            "longrunning_pkg2.yaml",
            "longrunning",
            null),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"samplegen_config_migration_library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            null,
            null,
            sampleConfigFileNames(),
            "ruby_samplegen_config_migration_library.baseline",
            new String[] {"another_service"}),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"no_path_templates_gapic.yaml"},
            "no_path_templates_pkg2.yaml",
            "no_path_templates",
            null),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services",
            null,
            "multiple_services_v2"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"samplegen_config_migration_library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            null,
            null,
            sampleConfigFileNames(),
            "python_samplegen_config_migration_library.baseline",
            new String[] {"another_service"}),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"no_path_templates_gapic.yaml"},
            "library_pkg2.yaml",
            "no_path_templates",
            null),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"multiple_services_gapic.yaml"},
            "multiple_services_pkg2.yaml",
            "multiple_services",
            null,
            "multiple_services_v2"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"samplegen_config_migration_library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            null,
            null,
            sampleConfigFileNames(),
            "nodejs_samplegen_config_migration_library.baseline",
            new String[] {"another_service"}),
        GapicTestBase2.createTestConfig(
            TargetLanguage.CSHARP,
            new String[] {"library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.CSHARP,
            new String[] {"samplegen_config_migration_library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            null,
            null,
            null,
            sampleConfigFileNames(),
            "csharp_samplegen_config_migration_library.baseline",
            new String[] {"another_service"}));
  }

  @Test
  public void test() throws Exception {
    test(baseNames);
  }

  private static String[] sampleConfigFileNames() {
    return new String[] {
      "fake.sample.yaml",
      "another_fake.sample.yaml",
      "babble_about_book.sample.yaml",
      "delete_shelf.sample.yaml",
      "discuss_book.sample.yaml",
      "find_related_books.sample.yaml",
      "get_big_book.sample.yaml",
      "get_big_nothing.sample.yaml",
      "get_book.sample.yaml",
      "get_book_from_absolutely_anywhere.sample.yaml",
      "get_shelf.sample.yaml",
      "list_shelves.sample.yaml",
      "monolog_about_book.sample.yaml",
      "publish_series.sample.yaml",
      "stream_books.sample.yaml",
      "test_optional_required_flattening_params.sample.yaml"
    };
  }
}
