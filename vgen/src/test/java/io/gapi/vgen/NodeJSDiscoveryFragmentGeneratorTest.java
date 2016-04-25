package io.gapi.vgen;

import com.google.common.collect.ImmutableList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.io.File;
import java.util.List;

/**
 * Java discovery doc fragment generator baseline tests.
 */
@RunWith(Parameterized.class)
public class NodeJSDiscoveryFragmentGeneratorTest extends DiscoveryFragmentGeneratorTestBase {

  public NodeJSDiscoveryFragmentGeneratorTest(
      String name, String discoveryDocFileName, String[] gapicConfigFileNames) {
    super(name, discoveryDocFileName, gapicConfigFileNames);
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with
   * the first element a name, the second a discovery doc, and the third a partial gapic config.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    File dir =
        new File(System.getProperty("user.dir"), "src/test/java/io/gapi/vgen/testdata/discoveries");
    ImmutableList.Builder<Object[]> builder = ImmutableList.<Object[]>builder();
    for (File file : dir.listFiles(new DiscoveryFile())) {
      String fileName = file.getName();
      builder.add(
          new Object[] {
            "nodejs_" + fileName,
            "discoveries/" + fileName,
            new String[] {"io/gapi/vgen/nodejs/nodejs_discovery_gapic.yaml"}
          });
    }
    return builder.build();
  }

  @Before
  public void putTestDirectory() {
    getTestDataLocator().addTestDataSource(this.getClass(), "testdata/discoveries/nodejs");
  }

  // Tests
  // =====

  @Test
  public void fragments() throws Exception {
    test();
  }
}
