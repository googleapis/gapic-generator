package io.gapi.vgen;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * Python code generator baseline tests.
 */
public class PythonCodeGeneratorTest  {

  @RunWith(Parameterized.class)
  public static class PythonLibraryBaseline extends CodeGeneratorTestBase {

    public PythonLibraryBaseline(String name, String[] veneerConfigFileNames) {
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
            "python",
            new String[]{"io/gapi/vgen/py/python_veneer.yaml", "library_veneer.yaml"}
        });
    }

    // Tests
    // =====

    @Test
    public void library() throws Exception {
      test("library");
    }

  }

  @RunWith(Parameterized.class)
  public static class PythonNoPathTemplatesBaseline extends CodeGeneratorTestBase {

    public PythonNoPathTemplatesBaseline(String name, String[] veneerConfigFileNames) {
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
            "python",
            new String[]{"io/gapi/vgen/py/python_veneer.yaml", "no_path_templates_veneer.yaml"}
        });
    }

    // Tests
    // =====


    @Test
    public void no_path_templates() throws Exception {
      test("no_path_templates");
    }

  }

}