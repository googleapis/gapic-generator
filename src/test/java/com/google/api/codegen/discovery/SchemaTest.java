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
import java.util.Map;
import org.junit.Test;

public class SchemaTest {

  @Test
  public void testSchema() throws IOException {
    String file = "src/test/java/com/google/api/codegen/discoverytestdata/schema.json";
    Reader reader = new InputStreamReader(new FileInputStream(new File(file)));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);

    Schema schema = Schema.from(new DiscoveryNode(root));

    Truth.assertThat(schema.description().equals("My Foo."));
    Truth.assertThat(schema.id().equals("Foo"));
    Truth.assertThat(schema.location().equals("bar"));
    Truth.assertThat(schema.pattern().equals(".*"));
    Truth.assertThat(schema.reference().equals("Baz"));
    Truth.assertThat(schema.repeated());
    Truth.assertThat(schema.required());
    Truth.assertThat(schema.type() == Schema.Type.OBJECT);

    Map<String, Schema> properties = schema.properties();

    Truth.assertThat(properties.get("any").type() == Schema.Type.ANY);
    Truth.assertThat(properties.get("array").type() == Schema.Type.ARRAY);
    Truth.assertThat(properties.get("boolean").type() == Schema.Type.BOOLEAN);

    Truth.assertThat(properties.get("byte").type() == Schema.Type.STRING);
    Truth.assertThat(properties.get("byte").format() == Schema.Format.BYTE);

    Truth.assertThat(properties.get("date").type() == Schema.Type.STRING);
    Truth.assertThat(properties.get("date").format() == Schema.Format.DATE);

    Truth.assertThat(properties.get("date-time").type() == Schema.Type.STRING);
    Truth.assertThat(properties.get("date-time").format() == Schema.Format.DATETIME);

    Truth.assertThat(properties.get("double").type() == Schema.Type.NUMBER);
    Truth.assertThat(properties.get("double").format() == Schema.Format.DOUBLE);

    Truth.assertThat(properties.get("empty").reference().equals("Foo"));
    Truth.assertThat(properties.get("empty").type() == Schema.Type.EMPTY);
    Truth.assertThat(properties.get("empty").format() == Schema.Format.EMPTY);

    Truth.assertThat(properties.get("float").type() == Schema.Type.NUMBER);
    Truth.assertThat(properties.get("float").format() == Schema.Format.FLOAT);

    Truth.assertThat(properties.get("int32").type() == Schema.Type.INTEGER);
    Truth.assertThat(properties.get("int32").format() == Schema.Format.INT32);

    Truth.assertThat(properties.get("int64").type() == Schema.Type.STRING);
    Truth.assertThat(properties.get("int64").format() == Schema.Format.INT64);

    Truth.assertThat(properties.get("object").additionalProperties().type() == Schema.Type.STRING);
    Truth.assertThat(properties.get("object").type() == Schema.Type.STRING);

    Truth.assertThat(properties.get("uint32").type() == Schema.Type.INTEGER);
    Truth.assertThat(properties.get("uint64").format() == Schema.Format.UINT32);

    Truth.assertThat(properties.get("uint64").type() == Schema.Type.STRING);
    Truth.assertThat(properties.get("uint64").format() == Schema.Format.UINT64);
  }
}
