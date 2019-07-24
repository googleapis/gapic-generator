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
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.gapic.GapicTestBase2;
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

  public GapicCodeGeneratorAnnotationsTest(
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

    String apiName = baseNames[0];

    String gapicConfigStatus = "_gapic_config";
    if (gapicConfigFileNames == null || gapicConfigFileNames.length == 0) {
      gapicConfigStatus = "_no" + gapicConfigStatus;
    }
    this.baseNames = baseNames;

    this.testName = apiName + gapicConfigStatus;

    getTestDataLocator().addTestDataSource(getClass(), "testdata");

    // Use the common yaml files from the codegen test package's testsrc/common.
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc");
    // TODO(andrealin): Remove dependency on yaml files when proto annotations fully supported.
  }

  @Parameters(name = "{5}")
  public static List<Object[]> testedConfigs() {
    return Arrays.<Object[]>asList(
        // Only Proto Annotations, no GAPIC config
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            null,
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "com.google.example.library.v1",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            null,
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "Library::V1",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "google.cloud.example.library_v1.gapic",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "google.cloud.example.library_v1.gapic",
            "library_grpc_service_config.json",
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "google.cloud.example.library_v1.gapic",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "google.cloud.example.library_v1.gapic",
            "library_grpc_service_config.json",
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.CSHARP,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "Google.Example.Library.V1",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.CSHARP,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "Google.Example.Library.V1",
            "library_grpc_service_config.json",
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.GO,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "cloud.google.com/go/library/apiv1",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.GO,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "cloud.google.com/go/library/apiv1",
            "library_grpc_service_config.json",
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "Library::V1",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "Library::V1",
            "library_grpc_service_config.json",
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            null,
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            null,
            "library_grpc_service_config.json",
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "library.v1",
            null,
            null,
            null,
            "another_service"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"library_v2_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1",
            "library.v1",
            "library_grpc_service_config.json",
            null,
            null,
            "another_service"));
  }

  @Test
  public void test() throws Exception {
    test(baseNames);
  }
}
