package io.gapi.vgen;

import com.google.common.collect.ImmutableList;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.List;

/**
 * Java discovery doc fragment generator baseline tests.
 */
@RunWith(Parameterized.class)
public class JavaDiscoveryFragmentGeneratorTest extends DiscoveryFragmentGeneratorTestBase {

  public JavaDiscoveryFragmentGeneratorTest(String name, String discoveryDocFileName,
      String[] veneerConfigFileNames) {
    super(name, discoveryDocFileName, veneerConfigFileNames);
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with
   * the first element a name, the second a discovery doc, and the third a partial veneer config.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    return ImmutableList.of(
      new Object[] {
          "java_discovery_fragments", "pubsub.json", new String[] {
              "io/gapi/vgen/java/java_discovery_veneer.yaml",
          }
      });
  }

  // Tests
  // =====

  @Test
  public void pubsub() throws Exception {
    test();
  }
}
