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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class DocumentTest {

  private Document document(String filename) throws IOException {
    Reader reader = new InputStreamReader(new FileInputStream(new File(filename)));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);

    return Document.from(new DiscoveryNode(root));
  }

  @Test
  public void testDocumentFromJson() throws IOException {
    String filename = "src/test/java/com/google/api/codegen/discoverytestdata/document_.json";
    Document document = document(filename);

    Truth.assertThat(document.description()).isEqualTo("My API!");

    List<Method> methods = document.methods();
    Collections.sort(methods);

    Truth.assertThat(document.authType()).isEqualTo(Document.AuthType.OAUTH_3L);
    Truth.assertThat(methods.get(0).description()).isEqualTo("Get a baz.");
    Truth.assertThat(methods.get(0).id()).isEqualTo("myapi.bar.baz.get");
    Truth.assertThat(methods.get(0).parameterOrder()).isEqualTo(Collections.singletonList("p1"));
    Truth.assertThat(methods.get(0).parent() instanceof Document);
    Truth.assertThat(methods.get(0).parent()).isEqualTo(document);

    // Test de-referencing.
    Truth.assertThat(methods.get(0).request().dereference())
        .isEqualTo(document.schemas().get("GetBazRequest"));
    Truth.assertThat(methods.get(0).response().dereference())
        .isEqualTo(document.schemas().get("Baz"));
    // De-referencing a schema that references no other should return the same schema.
    Truth.assertThat(document.schemas().get("Baz").dereference())
        .isEqualTo(document.schemas().get("Baz"));

    Map<String, Schema> parameters = methods.get(0).parameters();
    Truth.assertThat(parameters.get("p1").type()).isEqualTo(Schema.Type.BOOLEAN);
    Truth.assertThat(parameters.get("p1").required()).isTrue();
    Truth.assertThat(parameters.get("p1").location()).isEqualTo("query");
    Truth.assertThat(parameters.get("p1").parent() instanceof Method);
    Truth.assertThat(parameters.get("p1").parent()).isEqualTo(methods.get(0));

    Truth.assertThat(methods.get(1).description()).isEqualTo("Insert a foo.");
    Truth.assertThat(methods.get(1).id()).isEqualTo("myapi.foo.insert");
    Truth.assertThat(methods.get(1).parameters().isEmpty()).isTrue();
    Truth.assertThat(methods.get(1).request()).isNull();
    Truth.assertThat(methods.get(1).response()).isNull();
    Truth.assertThat(methods.get(1).id()).isEqualTo("myapi.foo.insert");

    Truth.assertThat(document.name()).isEqualTo("myapi");
    Truth.assertThat(document.canonicalName()).isEqualTo("My API");
    Truth.assertThat(document.revision()).isEqualTo("20170419");
    Truth.assertThat(document.rootUrl()).isEqualTo("https://example.com");

    Map<String, Schema> schemas = document.schemas();

    Truth.assertThat(schemas.get("GetBazRequest").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(schemas.get("GetBazRequest").properties().get("foo").parent())
        .isEqualTo(schemas.get("GetBazRequest"));
    Truth.assertThat(schemas.get("GetBazRequest").parent() instanceof Document);
    Truth.assertThat(schemas.get("GetBazRequest").parent()).isEqualTo(document);

    Truth.assertThat(schemas.get("Baz").type()).isEqualTo(Schema.Type.STRING);

    Truth.assertThat(document.servicePath()).isEqualTo("/api");
    Truth.assertThat(document.title()).isEqualTo("My API!");
    Truth.assertThat(document.version()).isEqualTo("v1");
    Truth.assertThat(document.versionModule()).isTrue();
  }

  @Test
  public void testAuthType() throws IOException {
    String filename = "src/test/java/com/google/api/codegen/discoverytestdata/auth_adc.json";
    Document document = document(filename);

    Truth.assertThat(document.authType()).isEqualTo(Document.AuthType.ADC);

    filename = "src/test/java/com/google/api/codegen/discoverytestdata/auth_3lo.json";
    document = document(filename);

    Truth.assertThat(document.authType()).isEqualTo(Document.AuthType.OAUTH_3L);

    filename = "src/test/java/com/google/api/codegen/discoverytestdata/auth_api_key.json";
    document = document(filename);

    Truth.assertThat(document.authType()).isEqualTo(Document.AuthType.API_KEY);
  }
}
