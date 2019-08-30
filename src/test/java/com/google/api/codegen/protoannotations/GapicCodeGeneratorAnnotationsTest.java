/* Copyright 2018 Google LLC
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
package com.google.api.codegen.protoannotations;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.GapicCodegenTestConfig;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.gapic.GapicTestBase2;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Code generator baseline tests. Tests generation using proto annotations, without config files.
 */
@RunWith(Parameterized.class)
public class GapicCodeGeneratorAnnotationsTest extends GapicTestBase2 {

  private final String testName;

  private final String[] baseNames;

  public GapicCodeGeneratorAnnotationsTest(GapicCodegenTestConfig testConfig, String baseline) {
    super(testConfig);

    String apiName = testConfig.baseNames().get(0);

    String gapicConfigStatus = "_gapic_config";
    if (testConfig.gapicConfigFileNames() == null
        || testConfig.gapicConfigFileNames().size() == 0) {
      gapicConfigStatus = "_no" + gapicConfigStatus;
    }
    this.baseNames = testConfig.baseNames().stream().toArray(String[]::new);

    this.testName = apiName + gapicConfigStatus;

    getTestDataLocator().addTestDataSource(getClass(), "testdata");

    // Use the common yaml files from the codegen test package's testsrc/common.
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc");
    // TODO(andrealin): Remove dependency on yaml files when proto annotations fully supported.
  }

  @Parameters(name = "{1}")
  public static List<Object[]> testedConfigs() {
    return Arrays.<Object[]>asList(
        // Only Proto Annotations, no GAPIC config
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("com.google.example.library.v1")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.RUBY)
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("Library::V1")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("google.cloud.example.library_v1.gapic")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("google.cloud.example.library_v1.gapic")
            .grpcServiceConfigFileName("library_grpc_service_config.json")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PYTHON)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("google.cloud.example.library_v1.gapic")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.CSHARP)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("Google.Example.Library.V1")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.GO)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("cloud.google.com/go/library/apiv1")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.RUBY)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("Library::V1")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PHP)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.PHP)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .grpcServiceConfigFileName("library_grpc_service_config.json")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput(),
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.NODEJS)
            .gapicConfigFileNames(ImmutableList.of("library_v2_gapic.yaml"))
            .packageConfigFileName("library_pkg2.yaml")
            .apiName("library")
            .protoPackage("google.example.library.v1")
            .clientPackage("library.v1")
            .baseNames(ImmutableList.of("another_service"))
            .build()
            .toParameterizedTestInput());
  }

  @Test
  public void test() throws Exception {
    test(baseNames);
  }
}
