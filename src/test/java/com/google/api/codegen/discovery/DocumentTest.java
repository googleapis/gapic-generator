/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.truth.Truth;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DocumentTest {
  @Test
  public void testDocument() throws IOException {
    String file = "src/test/java/com/google/api/codegen/discoverytestdata/document.json";
    Reader reader = new InputStreamReader(new FileInputStream(new File(file)));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);

    Document document = Document.from(new DiscoveryNode(root));
    Truth.assertThat(document.description()).isEqualTo("My API!");

    List<Method> methods = document.methods();
    Collections.sort(methods);

    Truth.assertThat(methods.get(0).description()).isEqualTo("Get a baz.");
    Truth.assertThat(methods.get(0).id()).isEqualTo("myapi.bar.baz.get");
    Truth.assertThat(methods.get(0).parameterOrder()).isEqualTo(Arrays.asList("p1"));
    Truth.assertThat(methods.get(0).parameters().get("p1").type()).isEqualTo(Schema.Type.BOOLEAN);
    Truth.assertThat(methods.get(0).parameters().get("p1").required()).isTrue();
    Truth.assertThat(methods.get(0).parameters().get("p1").location()).isEqualTo("query");
    Truth.assertThat(methods.get(0).resourceHierarchy()).isEqualTo(Arrays.asList("bar", "baz"));

    Truth.assertThat(methods.get(1).description()).isEqualTo("Insert a foo.");
    Truth.assertThat(methods.get(1).id()).isEqualTo("myapi.foo.insert");
    Truth.assertThat(methods.get(1).resourceHierarchy()).isEqualTo(Arrays.asList("foo"));

    Truth.assertThat(document.name()).isEqualTo("myapi");
    Truth.assertThat(document.revision()).isEqualTo("20170419");
    Truth.assertThat(document.rootUrl()).isEqualTo("https://example.com");

    Map<String, Schema> schemas = document.schemas();

    Truth.assertThat(schemas.get("GetBazRequest").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(schemas.get("Baz").type()).isEqualTo(Schema.Type.STRING);

    Truth.assertThat(document.servicePath()).isEqualTo("/api");
    Truth.assertThat(document.title()).isEqualTo("My API!");
    Truth.assertThat(document.version()).isEqualTo("v1");
    Truth.assertThat(document.versionModule()).isTrue();
  }
}
