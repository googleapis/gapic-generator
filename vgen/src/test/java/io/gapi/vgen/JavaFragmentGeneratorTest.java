package io.gapi.vgen;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * Java fragment generator baseline tests.
 */
@RunWith(Parameterized.class)
public class JavaFragmentGeneratorTest extends FragmentGeneratorTestBase {

  public JavaFragmentGeneratorTest(String name, String[] veneerConfigFileNames) {
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
          "java_fragments", new String[] {
              "io/gapi/vgen/java/java_veneer.yaml",
              "library_veneer.yaml",
          }
      });
  }

  // Tests
  // =====

  @Test
  public void library() throws Exception {
    test("library");
  }
}
