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
package com.google.api.codegen;

import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * NodeJS code generator baseline tests.
 */
@RunWith(Parameterized.class)
public class NodeJSCodeGeneratorTest extends CodeGeneratorTestBase {

  public NodeJSCodeGeneratorTest(String name, String[] gapicConfigFileNames) {
    super(name, gapicConfigFileNames);
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with the first
   * element a name, the second a config of this name.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    return ImmutableList.of(
        new Object[] {
          "nodejs",
          new String[] {
            "com/google/api/codegen/nodejs/nodejs_gapic.yaml", "library_gapic.yaml",
          }
        });
  }

  @Override
  protected Object run() {
    // Should generate one file for the class, a list of files for the protos, and a client
    // config.
    List<GeneratedResult> codeResult = generateForTemplate(0, 0);
    List<GeneratedResult> configResult = generateForTemplate(1, 0);
    Truth.assertThat(codeResult).isNotNull();
    Truth.assertThat(configResult).isNotNull();

    ImmutableMap.Builder<String, Doc> builder = new ImmutableMap.Builder<String, Doc>();
    for (GeneratedResult result : codeResult) {
      builder.put(result.getFilename(), result.getDoc());
    }
    for (GeneratedResult result : configResult) {
      builder.put(result.getFilename(), result.getDoc());
    }
    return builder.build();
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test("library");
  }
}
