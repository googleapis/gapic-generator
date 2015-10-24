package io.gapi.vgen;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * Java code generator baseline tests.
 */
@RunWith(Parameterized.class)
public class PythonCodeGeneratorTest extends CodeGeneratorTestBase {

  public PythonCodeGeneratorTest(String name, Config config) {
    super(name, config);
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
          Config.newBuilder()
            .setLanguageProvider("io.gapi.vgen.py.PythonLanguageProvider")
            .addSnippetFiles("main.snip")
            .build()
      });
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test("library");
  }
}