/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http: *www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gapi.vgen;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * C# code generator baseline tests.
 */
@RunWith(Parameterized.class)
public class CSharpCodeGeneratorTest extends CodeGeneratorTestBase {

  public CSharpCodeGeneratorTest(String name, String[] veneerConfigFileNames, String snippetName) {
    super(name, veneerConfigFileNames, snippetName);
    getTestDataLocator().addTestDataSource(io.gapi.vgen.csharp.CSharpLanguageProvider.class, "");
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with
   * the first element a name, the second a config of this name.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    // TODO(jonskeet): Remove the separate YAML files when we have a better way of overriding
    // which snippets to use. (Ideally we should use csharp_veneer.yaml and only override which
    // snippet we want to test. While additional YAML files can override single values, they
    // append to list values.)
    return ImmutableList.of(
        new Object[] {
            "csharp_wrapper",
            new String[] { "library_veneer.yaml", "csharp_veneer.yaml"},
            "wrapper.snip"
        });
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test("library");
  }
}
