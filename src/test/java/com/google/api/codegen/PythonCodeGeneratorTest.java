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

import com.google.api.codegen.gapic.MainGapicProviderFactory;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Python code generator baseline tests. */
public class PythonCodeGeneratorTest {

  @RunWith(Parameterized.class)
  public static class PythonLibraryBaseline extends GapicTestBase {

    public PythonLibraryBaseline(
        String name,
        String idForFactory,
        String[] gapicConfigFileNames,
        String packageConfigFileName,
        String snippetName) {
      super(name, idForFactory, gapicConfigFileNames, packageConfigFileName, snippetName);
      getTestDataLocator().addTestDataSource(getClass(), "py");
      getTestDataLocator().addTestDataSource(getClass(), "testdata/py");
    }

    /**
     * Declares test parameters, each one an array of values passed to the constructor, with the
     * first element a name, the second a config of this name.
     */
    @Parameters(name = "{0}")
    public static List<Object[]> testedConfigs() {
      return GapicTestBase.createTestedConfigs(
          MainGapicProviderFactory.PYTHON,
          new String[] {"python_gapic.yaml", "library_gapic.yaml"},
          "library_pkg.yaml");
    }

    // Tests
    // =====

    @Test
    public void library() throws Exception {
      test("library");
    }
  }

  @RunWith(Parameterized.class)
  public static class PythonNoPathTemplatesBaseline extends GapicTestBase {

    public PythonNoPathTemplatesBaseline(
        String name,
        String idForFactory,
        String[] gapicConfigFileNames,
        String packageConfigFileName,
        String snippetName) {
      super(name, idForFactory, gapicConfigFileNames, packageConfigFileName, snippetName);
      getTestDataLocator().addTestDataSource(getClass(), "py");
      getTestDataLocator().addTestDataSource(getClass(), "testdata/py");
    }

    /**
     * Declares test parameters, each one an array of values passed to the constructor, with the
     * first element a name, the second a config of this name.
     */
    @Parameters(name = "{0}")
    public static List<Object[]> testedConfigs() {
      return GapicTestBase.createTestedConfigs(
          "python",
          new String[] {"python_gapic.yaml", "no_path_templates_gapic.yaml"},
          "no_path_templates_pkg.yaml");
    }

    // Tests
    // =====

    @Test
    public void no_path_templates() throws Exception {
      test("no_path_templates");
    }
  }
}
