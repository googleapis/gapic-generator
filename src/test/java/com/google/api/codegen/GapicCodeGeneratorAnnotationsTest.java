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
package com.google.api.codegen;

import com.google.api.codegen.common.TargetLanguage;
import java.util.LinkedList;
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
    //    super(language, null, null, snippetName, baseline);
    super(language, null, null, snippetName, baseline, protoPackage);

    this.apiName = apiName;
    getTestDataLocator().addTestDataSource(getClass(), "testdata");
    getTestDataLocator().addTestDataSource(getClass(), "../../common");
  }

  @Parameters(name = "{3}")
  public static List<Object[]> testedConfigs() {
    return new LinkedList<>();
    // TODO(andrealin): Implement parsing proto-annotations, make baseline files, write tests.
  }

  @Test
  public void test() throws Exception {
    test(apiName);
  }
}
