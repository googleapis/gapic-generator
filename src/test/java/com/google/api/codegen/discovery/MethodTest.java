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
import java.util.Map;
import org.junit.Test;

public class MethodTest {
  @Test
  public void testMethodFromJson() throws IOException {
    String file = "src/test/java/com/google/api/codegen/discoverytestdata/method_.json";
    Reader reader = new InputStreamReader(new FileInputStream(new File(file)));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);

    Method method = Method.from(new DiscoveryNode(root), null);

    Truth.assertThat(method.description()).isEqualTo("Get a baz!");
    Truth.assertThat(method.httpMethod()).isEqualTo("GET");
    Truth.assertThat(method.id()).isEqualTo("foo.bar.baz.get");
    Truth.assertThat(method.parameterOrder()).isEqualTo(Arrays.asList("p3", "p1"));

    Map<String, Schema> parameters = method.parameters();
    Truth.assertThat(parameters.get("p1").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(parameters.get("p1").required()).isTrue();
    Truth.assertThat(parameters.get("p1").location()).isEqualTo("path");
    Truth.assertThat(parameters.get("p1").parent().id()).isEqualTo("foo.bar.baz.get");

    Truth.assertThat(parameters.get("p2").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(parameters.get("p2").location()).isEqualTo("query");

    Truth.assertThat(parameters.get("p3").type()).isEqualTo(Schema.Type.INTEGER);
    Truth.assertThat(parameters.get("p3").required()).isTrue();

    Truth.assertThat(method.request().reference()).isEqualTo("GetBazRequest");
    Truth.assertThat(method.response().reference()).isEqualTo("Baz");

    Truth.assertThat(method.request().parent().id()).isEqualTo("foo.bar.baz.get");
    Truth.assertThat(method.response().parent().id()).isEqualTo("foo.bar.baz.get");

    Truth.assertThat(method.scopes())
        .isEqualTo(Arrays.asList("https://www.example.com/foo", "https://www.example.com/bar"));
    Truth.assertThat(method.supportsMediaDownload()).isTrue();
    Truth.assertThat(method.supportsMediaUpload()).isTrue();
  }
}
