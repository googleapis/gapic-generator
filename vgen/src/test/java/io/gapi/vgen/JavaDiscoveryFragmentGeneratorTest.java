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
package io.gapi.vgen;

import com.google.common.collect.ImmutableList;

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
public class JavaDiscoveryFragmentGeneratorTest extends DiscoveryFragmentGeneratorTestBase {

  public JavaDiscoveryFragmentGeneratorTest(
      String name, String discoveryDocFileName, String[] veneerConfigFileNames) {
    super(name, discoveryDocFileName, veneerConfigFileNames);
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with
   * the first element a name, the second a discovery doc, and the third a partial veneer config.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    File dir =
        new File(System.getProperty("user.dir"), "src/test/java/io/gapi/vgen/testdata/discoveries");
    ImmutableList.Builder<Object[]> builder = ImmutableList.<Object[]>builder();
    for (String fileName : dir.list()) {
      builder.add(
          new Object[] {
            "java_discovery_fragments_" + fileName,
            "discoveries/" + fileName,
            new String[] {"io/gapi/vgen/java/java_discovery_veneer.yaml"}
          });
    }
    return builder.build();
  }

  // Tests
  // =====

  @Test
  public void fragments() throws Exception {
    test();
  }
}
