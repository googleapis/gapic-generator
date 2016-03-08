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
public class JavaCodeGeneratorTest extends CodeGeneratorTestBase {

  public JavaCodeGeneratorTest(String name, String[] veneerConfigFileNames, String snippetName) {
    super(name, veneerConfigFileNames, snippetName);
    getTestDataLocator().addTestDataSource(io.gapi.vgen.java.JavaLanguageProvider.class, "");
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with
   * the first element a name, the second a config of this name.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    return ImmutableList.of(
      new Object[] {
          "java_main",
          new String[]{ "java_veneer.yaml", "library_veneer.yaml" },
          "main.snip"
      },
      new Object[] {
          "java_settings",
          new String[]{ "java_veneer.yaml", "library_veneer.yaml" },
          "settings.snip"
      });
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test("library");
  }
}
