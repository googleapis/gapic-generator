/* Copyright 2019 Google LLC
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/**
 * Code generator baseline tests. Tests generation using proto annotations and sample configs, but
 * not gapic configs.
 */
@RunWith(Parameterized.class)
public class GapicCodeGeneratorShowcaseSamplesTest extends GapicTestBase2 {

  private String[] baseNames;

  public GapicCodeGeneratorShowcaseSamplesTest(GapicCodegenTestConfig testConfig, String baseline) {
    super(testConfig);
    this.baseNames = testConfig.baseNames().stream().toArray(String[]::new);
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/showcase");
    getTestDataLocator().addTestDataSource(CodegenTestUtil.class, "testsrc/showcase/samples");
  }

  @Parameters(name = "{1}")
  public static List<Object[]> testedConfigs() {
    return Arrays.<Object[]>asList(
        GapicCodegenTestConfig.newBuilder()
            .targetLanguage(TargetLanguage.JAVA)
            .packageConfigFileName("showcase_pkg2.yaml")
            .gapicConfigFileNames(ImmutableList.of("showcase_gapic.yaml"))
            .sampleConfigFileNames(ImmutableList.of("echo.sample.yaml"))
            .apiName("showcase")
            .protoPackage("google.showcase.v1beta1")
            .clientPackage("com.google.showcase.v1beta1")
            .baseline("java_showcase_samples.baseline")
            .baseNames(ImmutableList.of("echo", "identity", "messaging", "testing"))
            .build()
            .toParameterizedTestInput());
  }

  @Override
  public Map<String, ?> run() throws IOException {
    return runWithArtifacts(new ArrayList<>(Arrays.asList("samples")));
  }

  @Test
  public void test() throws Exception {
    test(baseNames);
  }
}
