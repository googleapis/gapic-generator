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
import java.util.Map;
import org.junit.Test;

public class SchemaTest {
  @Test
  public void testSchemaFromJson() throws IOException {
    String file = "src/test/java/com/google/api/codegen/discoverytestdata/schema_.json";
    Reader reader = new InputStreamReader(new FileInputStream(new File(file)));

    ObjectMapper mapper = new ObjectMapper();
    JsonNode root = mapper.readTree(reader);

    Schema schema = Schema.from(new DiscoveryNode(root), "", null);

    Truth.assertThat(schema.description()).isEqualTo("My Foo.");
    Truth.assertThat(schema.id()).isEqualTo("Foo");
    Truth.assertThat(schema.location()).isEqualTo("bar");
    Truth.assertThat(schema.pattern()).isEqualTo(".*");
    Truth.assertThat(schema.reference()).isEqualTo("Baz");
    Truth.assertThat(schema.required()).isTrue();
    Truth.assertThat(schema.type()).isEqualTo(Schema.Type.OBJECT);

    Map<String, Schema> properties = schema.properties();

    Truth.assertThat(properties.get("any").type()).isEqualTo(Schema.Type.ANY);

    Truth.assertThat(properties.get("array").type()).isEqualTo(Schema.Type.ARRAY);
    Truth.assertThat(properties.get("array").items().type()).isEqualTo(Schema.Type.STRING);

    Truth.assertThat(properties.get("boolean").type()).isEqualTo(Schema.Type.BOOLEAN);

    Truth.assertThat(properties.get("byte").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("byte").format()).isEqualTo(Schema.Format.BYTE);

    Truth.assertThat(properties.get("date").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("date").format()).isEqualTo(Schema.Format.DATE);

    Truth.assertThat(properties.get("date-time").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("date-time").format()).isEqualTo(Schema.Format.DATETIME);

    Truth.assertThat(properties.get("double").type()).isEqualTo(Schema.Type.NUMBER);
    Truth.assertThat(properties.get("double").format()).isEqualTo(Schema.Format.DOUBLE);

    Truth.assertThat(properties.get("empty").reference()).isEqualTo("Foo");
    Truth.assertThat(properties.get("empty").type()).isEqualTo(Schema.Type.EMPTY);
    Truth.assertThat(properties.get("empty").format()).isEqualTo(Schema.Format.EMPTY);

    Truth.assertThat(properties.get("enum").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("enum").isEnum()).isTrue();

    Truth.assertThat(properties.get("float").type()).isEqualTo(Schema.Type.NUMBER);
    Truth.assertThat(properties.get("float").format()).isEqualTo(Schema.Format.FLOAT);

    Truth.assertThat(properties.get("int32").type()).isEqualTo(Schema.Type.INTEGER);
    Truth.assertThat(properties.get("int32").format()).isEqualTo(Schema.Format.INT32);

    Truth.assertThat(properties.get("int64").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("int64").format()).isEqualTo(Schema.Format.INT64);

    Truth.assertThat(properties.get("object").additionalProperties().type())
        .isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("object").type()).isEqualTo(Schema.Type.OBJECT);

    Truth.assertThat(properties.get("repeated").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("repeated").repeated()).isTrue();

    Truth.assertThat(properties.get("uint32").type()).isEqualTo(Schema.Type.INTEGER);
    Truth.assertThat(properties.get("uint32").format()).isEqualTo(Schema.Format.UINT32);

    Truth.assertThat(properties.get("uint64").type()).isEqualTo(Schema.Type.STRING);
    Truth.assertThat(properties.get("uint64").format()).isEqualTo(Schema.Format.UINT64);

    // Test path.
    Truth.assertThat(schema.id()).isEqualTo("Foo");
    Truth.assertThat(schema.properties().get("array").parent()).isEqualTo(schema);
    Truth.assertThat(schema.properties().get("array").items().parent())
        .isEqualTo(schema.properties().get("array"));

    Truth.assertThat(schema.properties().get("object").additionalProperties().parent())
        .isEqualTo(schema.properties().get("object"));
  }

  @Test
  public void testSchemaFromEmptyNode() {
    Truth.assertThat(Schema.from(new DiscoveryNode(null), "", null).type())
        .isEqualTo(Schema.Type.EMPTY);
  }
}
