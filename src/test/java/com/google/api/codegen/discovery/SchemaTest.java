/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.discovery;

import static com.google.common.truth.Truth.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import org.junit.Test;

public class SchemaTest {
  @Test
  public void testSchemaFromJson() throws IOException {
    String file = "src/test/java/com/google/api/codegen/discovery/testdata/graph.json";
    Reader reader = new InputStreamReader(new FileInputStream(new File(file)));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);

    Document document = Document.from(new DiscoveryNode(root));

    Schema apple = document.schemas().get("Apple");
    Schema banana = document.schemas().get("Banana");
    Schema cat = document.schemas().get("Cat");
    Schema dog = document.schemas().get("Dog");
    List<Schema> appleToDogPath = apple.findChild("Dog");

    // The path fro
    assertThat(appleToDogPath.get(0)).isEqualTo(apple);
    assertThat(appleToDogPath.get(1).getIdentifier()).isEqualTo("items");
    assertThat(appleToDogPath.get(2)).isEqualTo(banana);
    assertThat(appleToDogPath.get(3)).isEqualTo(cat);
    assertThat(appleToDogPath.get(4).getIdentifier()).isEqualTo("items");
    assertThat(appleToDogPath.get(5)).isEqualTo(dog);
  }
}
