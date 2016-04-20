/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gapi.vgen;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * Go code generator baseline tests.
 */
@RunWith(Parameterized.class)
public class GoCodeGeneratorTest extends CodeGeneratorTestBase {

  public GoCodeGeneratorTest(String name, String[] veneerConfigFileNames) {
    super(name, veneerConfigFileNames);
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with
   * the first element a name, the second a config of this name.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    return ImmutableList.of(
      new Object[] {
          "go", new String[]{
              "io/gapi/vgen/go/go_veneer.yaml",
              "library_veneer.yaml",
          }
      });
  }

  @Override
  protected Object run() {
    // GoLanguageGenerator should generate two files -- one for the class, and
    // the other for "doc.go" which holds package doc.
    GeneratedResult codeResult = generateForSnippet(0);
    GeneratedResult docResult = generateForSnippet(1);
    Truth.assertThat(codeResult).isNotNull();
    Truth.assertThat(docResult).isNotNull();
    return ImmutableMap.of(
        codeResult.getFilename(), codeResult.getDoc(),
        docResult.getFilename(), docResult.getDoc());
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test("library");
  }
}
