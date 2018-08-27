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

import com.google.api.codegen.common.TargetLanguage;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Go code generator baseline tests. */
@RunWith(Parameterized.class)
public class GapicCodeGeneratorAnnotationsTest extends GapicTestBase2 {

  private final String apiName;

  public GapicCodeGeneratorAnnotationsTest(
      TargetLanguage language,
      String[] gapicConfigFileNames,
      String packageConfigFileName,
      List<String> snippetName,
      String apiName,
      String baseline) {
    super(language, null, null, snippetName, baseline);
    this.apiName = apiName;
    String dir = language.toString().toLowerCase();
    if ("python".equals(dir)) {
      dir = "py";
    }
    getTestDataLocator().addTestDataSource(getClass(), "testsrc/libraryproto/annotationsonly");
    getTestDataLocator().addTestDataSource(getClass(), dir);
    getTestDataLocator().addTestDataSource(getClass(), "testdata/" + dir);
  }

  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    return Arrays.asList(
        GapicTestBase2.createTestConfig(TargetLanguage.GO, null, null, "library"),
        GapicTestBase2.createTestConfig(TargetLanguage.JAVA, null, "library_pkg2.yaml", "library"));
  }

  @Test
  public void test() throws Exception {
    test(apiName);
  }
}
