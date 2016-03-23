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

    @Override
    protected Object run() {
      // Should generate three files -- one for the class, one for the other for the configuration
      // yaml, and one for the messages class
      GeneratedResult codeResult = generateForSnippet(0);
      GeneratedResult configResult = generateForSnippet(1);
      GeneratedResult messagesResult = generateForSnippet(2);
      Truth.assertThat(codeResult).isNotNull();
      Truth.assertThat(configResult).isNotNull();
      Truth.assertThat(messagesResult).isNotNull();
      return ImmutableMap.of(
          codeResult.getFilename(), codeResult.getDoc(),
          configResult.getFilename(), configResult.getDoc(),
          messagesResult.getFilename(), messagesResult.getDoc());
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

    @Override
    protected Object run() {
      GeneratedResult codeResult = generateForSnippet(0);
      GeneratedResult configResult = generateForSnippet(1);
      GeneratedResult messagesResult = generateForSnippet(2);
      Truth.assertThat(codeResult).isNotNull();
      Truth.assertThat(configResult).isNotNull();
      Truth.assertThat(messagesResult).isNotNull();
      return ImmutableMap.of(
          codeResult.getFilename(), codeResult.getDoc(),
          configResult.getFilename(), configResult.getDoc(),
          messagesResult.getFilename(), messagesResult.getDoc());
    }

    // Tests
    // =====


    @Test
    public void no_path_templates() throws Exception {
      test("no_path_templates");
    }

  }

}
