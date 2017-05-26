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

import static org.junit.Assert.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.DiscoGapicProvider;
import com.google.api.codegen.discogapic.MainDiscoGapicProviderFactory;
import com.google.api.codegen.discovery.DiscoveryNode;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.gapic.GapicGeneratorConfig;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

/** Java discovery doc schema generator baseline tests. */
@RunWith(Parameterized.class)
public class JavaDiscoverySchemaGeneratorTest extends DiscoveryGeneratorTestBase {
  private final Document document;
  private static final String DISCOVERIES_FILEPATH = "src/test/java/com/google/api/codegen/testdata/";

  public JavaDiscoverySchemaGeneratorTest(
      String name, String discoveryDocFileName, String[] gapicConfigFileNames) throws IOException {
    super(name, discoveryDocFileName, gapicConfigFileNames);

    Reader reader = new InputStreamReader(new FileInputStream(
        new File(DISCOVERIES_FILEPATH + discoveryDocFileName)));
    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);
    this.document = Document.from(new DiscoveryNode(root));
  }

  /**
   * Declares test parameters, each one an array of values passed to the constructor, with the first
   * element a name, the second a discovery doc, and the third a partial GAPIC config.
   */
  @Parameters(name = "{0}")
  public static List<Object[]> testedConfigs() {
    File dir =
        new File(
            System.getProperty("user.dir"),
            DISCOVERIES_FILEPATH + "discoveries");
    ImmutableList.Builder<Object[]> builder = ImmutableList.<Object[]>builder();
    for (File file : dir.listFiles(new DiscoveryFile())) {
      String fileName = file.getName();
      builder.add(
          new Object[] {
              "java_" + fileName,
              "discoveries/" + fileName,
              new String[] {"com/google/api/codegen/java/java_discovery.yaml"}
          });
    }
    return builder.build();
  }

  @Before
  public void putTestDirectory() {
    getTestDataLocator().addTestDataSource(this.getClass(), "testdata/discoveries/java");
  }

  // Tests
  // =====

  @Test
  public void fragments() throws Exception {
    test();
  }


  @Override
  protected Object run() {
    GeneratorProto generator = config.getGenerator();
    String id = generator.getId();
    GapicProductConfig productConfig = GapicProductConfig.create(document, config);
    GapicGeneratorConfig generatorConfig =
        GapicGeneratorConfig.newBuilder()
            .id(id)
            .enabledArtifacts(DiscoGapicGeneratorApi.ENABLED_ARTIFACTS.defaultValue())
            .build();
    PackageMetadataConfig packageConfig = null;
    try {
      String contents =
            new String(
                Files.readAllBytes(
                    Paths.get("src/test/java/com/google/api/codegen/testdata/library_pkg.yaml")),
                StandardCharsets.UTF_8);
        packageConfig = PackageMetadataConfig.createFromString(contents);
    } catch (IOException e) {
      fail();
    }

    List<DiscoGapicProvider> providers =
        MainDiscoGapicProviderFactory.defaultCreate(
            document, productConfig, generatorConfig, packageConfig);

    Doc output = Doc.EMPTY;
    for (DiscoGapicProvider provider : providers) {
      Map<String, Doc> docs = provider.generate();
      for (Doc doc : docs.values()) {
        output = Doc.joinWith(Doc.BREAK, output, doc);
      }
    }

    return Doc.vgroup(output);
  }
}

