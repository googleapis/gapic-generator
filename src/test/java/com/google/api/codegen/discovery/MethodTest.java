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
package com.google.api.codegen.discovery;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.truth.Truth;
import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class MethodTest {
  @Test
  public void testMethod() throws IOException {
    String file = "src/test/java/com/google/api/codegen/discoverytestdata/method_.json";
    Reader reader = new InputStreamReader(new FileInputStream(new File(file)));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);

    List<String> resourceHierarchy = Arrays.asList("bar");
    Method method = Method.from(new DiscoveryNode(root), resourceHierarchy);

    Truth.assertThat(method.description().equals("Get a baz!"));
    Truth.assertThat(method.httpMethod().equals("GET"));
    Truth.assertThat(method.id().equals("foo.bar.baz.get"));
    Truth.assertThat(method.parameterOrder().equals(resourceHierarchy));

    Map<String, Schema> parameters = method.parameters();

    Truth.assertThat(parameters.get("p1").type() == Schema.Type.STRING);
    Truth.assertThat(parameters.get("p1").required());
    Truth.assertThat(parameters.get("p1").location().equals("path"));

    Truth.assertThat(parameters.get("p2").type() == Schema.Type.STRING);
    Truth.assertThat(parameters.get("p2").location().equals("query"));

    Truth.assertThat(method.request().reference().equals("GetBazRequest"));
    Truth.assertThat(method.response().reference().equals("Baz"));
    Truth.assertThat(
        method
            .scopes()
            .equals(Arrays.asList("https://www.example.com/foo", "https://www.example.com/bar")));
    Truth.assertThat(method.supportsMediaDownload());
    Truth.assertThat(method.supportsMediaUpload());
  }
}
