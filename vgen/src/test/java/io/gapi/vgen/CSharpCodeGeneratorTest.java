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

  public CSharpCodeGeneratorTest(String name, String[] veneerConfigFileNames) {
    super(name, veneerConfigFileNames);
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
          "csharp_extensions",
          new String[] { "library_veneer.yaml", "csharp_extensions_library.yaml" }
      },
      new Object[] {
          "csharp_factory",
          new String[] { "library_veneer.yaml", "csharp_factory_library.yaml" }
      });
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test("library");
  }
}
