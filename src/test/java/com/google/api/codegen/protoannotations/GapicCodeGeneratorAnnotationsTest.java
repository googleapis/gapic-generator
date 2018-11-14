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

  private final String apiName;

  public GapicCodeGeneratorAnnotationsTest(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      List<String> snippetName,
      String apiName,
      String baseline,
      String protoPackage) {
    super(
        language, gapicConfigFileNames, packageConfigFileName, snippetName, baseline, protoPackage);

    this.apiName = apiName;
    // Use the library.proto contained in this test package's testdata.
    getTestDataLocator().addTestDataSource(getClass(), "testdata");

    // Use the common yaml files from the codegen test package's testsrc/common.
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/common");
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc");
    // TODO(andrealin): Remove dependency on yaml files when proto annotations fully supported.
  }

  @Parameters(name = "{3}")
  public static List<Object[]> testedConfigs() {
    // return Arrays.asList();
    // TODO(andrealin): Add these back in after baselines are checked in.
    return Arrays.<Object[]>asList(
        GapicTestBase2.createTestConfig(
            TargetLanguage.PHP,
            new String[] {"libraryproto/library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.NODEJS,
            new String[] {"libraryproto/library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.CSHARP,
            new String[] {"libraryproto/library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.RUBY,
            new String[] {"libraryproto/library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.PYTHON,
            new String[] {"libraryproto/library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.GO,
            new String[] {"libraryproto/library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1"),
        GapicTestBase2.createTestConfig(
            TargetLanguage.JAVA,
            new String[] {"libraryproto/library_gapic.yaml"},
            "library_pkg2.yaml",
            "library",
            "google.example.library.v1"));
  }

  @Test
  public void test() throws Exception {
    test(apiName);
  }
}
