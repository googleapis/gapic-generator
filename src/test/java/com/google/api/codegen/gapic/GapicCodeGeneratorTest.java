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
import com.google.api.codegen.GapicCodegenTestConfig;
import com.google.api.codegen.common.TargetLanguage;
import com.google.common.collect.ImmutableList;
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

  public GapicCodeGeneratorTest(GapicCodegenTestConfig testConfig, String baselineFileName) {
    super(testConfig);
    this.baseNames = testConfig.baseNames().stream().toArray(String[]::new);
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common/samples");
  }

  @Parameters(name = "{1}")
  public static List<Object[]> testedConfigs() {
    return Arrays.asList(
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.GO)
            .gapicConfigFileNames(ImmutableList.of("library_gapic.yaml"))
            .apiName("library")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PHP)
            .gapicConfigFileNames(ImmutableList.of("library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PHP)
            .gapicConfigFileNames(ImmutableList.of("longrunning_gapic.yaml"))
            .packageConfigFileName("longrunning_pkg2.yaml")
            .apiName("longrunning")
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PHP)
            .gapicConfigFileNames(ImmutableList.of("no_path_templates_gapic.yaml"))
            .packageConfigFileName("no_path_templates_pkg2.yaml")
            .apiName("no_path_templates")
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PHP)
            .gapicConfigFileNames(ImmutableList.of("samplegen_config_migration_library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .sampleConfigFileNames(sampleConfigFileNames())
            .baseline("php_samplegen_config_migration_library.baseline")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .gapicConfigFileNames(ImmutableList.of("library_gapic.yaml"))
            .apiName("library")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .gapicConfigFileNames(ImmutableList.of("multiple_services_gapic.yaml"))
            .packageConfigFileName("multiple_services_pkg2.yaml")
            .apiName("multiple_services")
            .baseNames(ImmutableList.of("multiple_services_v2"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .gapicConfigFileNames(ImmutableList.of("no_path_templates_gapic.yaml"))
            .packageConfigFileName("no_path_templates_pkg2.yaml")
            .apiName("no_path_templates")
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .gapicConfigFileNames(ImmutableList.of("my_streaming_proto_gapic.yaml"))
            .packageConfigFileName("my_streaming_proto_pkg2.yaml")
            .apiName("my_streaming_proto")
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .gapicConfigFileNames(ImmutableList.of("samplegen_config_migration_library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .sampleConfigFileNames(sampleConfigFileNames())
            .baseline("java_samplegen_config_migration_library.baseline")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.RUBY)
            .gapicConfigFileNames(ImmutableList.of("library_gapic.yaml"))
            .apiName("library")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.RUBY)
            .gapicConfigFileNames(ImmutableList.of("multiple_services_gapic.yaml"))
            .packageConfigFileName("multiple_services_pkg2.yaml")
            .apiName("multiple_services")
            .baseNames(ImmutableList.of("multiple_services_v2"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.RUBY)
            .gapicConfigFileNames(ImmutableList.of("longrunning_gapic.yaml"))
            .packageConfigFileName("longrunning_pkg2.yaml")
            .apiName("longrunning")
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.RUBY)
            .gapicConfigFileNames(ImmutableList.of("samplegen_config_migration_library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .sampleConfigFileNames(sampleConfigFileNames())
            .baseline("ruby_samplegen_config_migration_library.baseline")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PYTHON)
            .gapicConfigFileNames(ImmutableList.of("library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PYTHON)
            .gapicConfigFileNames(ImmutableList.of("no_path_templates_gapic.yaml"))
            .packageConfigFileName("no_path_templates_pkg2.yaml")
            .apiName("no_path_templates")
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PYTHON)
            .gapicConfigFileNames(ImmutableList.of("multiple_services_gapic.yaml"))
            .packageConfigFileName("multiple_services_pkg2.yaml")
            .apiName("multiple_services")
            .baseNames(ImmutableList.of("multiple_services_v2"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PYTHON)
            .gapicConfigFileNames(ImmutableList.of("samplegen_config_migration_library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .sampleConfigFileNames(sampleConfigFileNames())
            .baseline("python_samplegen_config_migration_library.baseline")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.NODEJS)
            .gapicConfigFileNames(ImmutableList.of("library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.NODEJS)
            .gapicConfigFileNames(ImmutableList.of("no_path_templates_gapic.yaml"))
            .packageConfigFileName("no_path_templates_pkg2.yaml")
            .apiName("no_path_templates")
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.NODEJS)
            .gapicConfigFileNames(ImmutableList.of("multiple_services_gapic.yaml"))
            .packageConfigFileName("multiple_services_pkg2.yaml")
            .apiName("multiple_services")
            .baseNames(ImmutableList.of("multiple_services_v2"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.NODEJS)
            .gapicConfigFileNames(ImmutableList.of("samplegen_config_migration_library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .sampleConfigFileNames(sampleConfigFileNames())
            .baseline("nodejs_samplegen_config_migration_library.baseline")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.CSHARP)
            .gapicConfigFileNames(ImmutableList.of("library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.CSHARP)
            .gapicConfigFileNames(ImmutableList.of("samplegen_config_migration_library_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .sampleConfigFileNames(sampleConfigFileNames())
            .baseline("csharp_samplegen_config_migration_library.baseline")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput());
  }

  @Test
  public void test() throws Exception {
    test(baseNames);
  }

  private static ImmutableList<String> sampleConfigFileNames() {
    return ImmutableList.of(
        "fake.sample.yaml",
        "another_fake.sample.yaml",
        "babble_about_book.sample.yaml",
        "create_book.sample.yaml",
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
        "test_optional_required_flattening_params.sample.yaml");
  }
}
