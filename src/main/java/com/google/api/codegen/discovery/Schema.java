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

import com.google.auto.value.AutoValue;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

@AutoValue
public abstract class Schema {

  /** The set of types a schema can represent. */
  public enum Type {
    ANY("any"),
    ARRAY("array"),
    BOOLEAN("boolean"),
    EMPTY(""),
    INTEGER("integer"),
    NUMBER("number"),
    OBJECT("object"),
    STRING("string");

    private String text;

    Type(String text) {
      this.text = text;
    }

    /**
     * @param text the JSON text of the type.
     * @return the enum representing the raw JSON type.
     */
    public static Type getEnum(String text) {
      for (Type t : values()) {
        if (t.text.equals(text)) {
          return t;
        }
      }
      throw new IllegalArgumentException("unknown type: " + text);
    }
  }

  /** The set of formats a schema can represent. */
  public enum Format {
    BYTE("byte"),
    DATE("date"),
    DATETIME("date-time"),
    DOUBLE("double"),
    EMPTY(""),
    FLOAT("float"),
    INT32("int32"),
    INT64("int64"),
    UINT32("uint32"),
    UINT64("uint64");

    private String text;

    Format(String text) {
      this.text = text;
    }

    /**
     * @param text the JSON text of the format.
     * @return the enum representing the raw JSON format.
     */
    public static Format getEnum(String text) {
      for (Format f : values()) {
        if (f.text.equals(text)) {
          return f;
        }
      }
      throw new IllegalArgumentException("unknown format: " + text);
    }
  }

  /**
   * Returns a schema constructed from root.
   *
   * @param root the root node to parse.
   * @return a schema.
   */
  public static Schema from(DiscoveryNode root) {
    if (root.size() == 0) {
      return null;
    }
    Schema additionalProperties = Schema.from(root.getObject("additionalProperties"));
    String defaultValue = root.getString("default");
    String description = root.getString("description");
    Format format = Format.getEnum(root.getString("format"));
    String id = root.getString("id");
    String location = root.getString("location");
    String pattern = root.getString("pattern");
    Map<String, Schema> properties = new HashMap<String, Schema>();
    DiscoveryNode propertiesNode = root.getObject("properties");
    for (String name : propertiesNode.fieldNames()) {
      properties.put(name, Schema.from(propertiesNode.getObject(name)));
    }
    String reference = root.getString("$ref");
    boolean repeated = root.getBoolean("repeated");
    boolean required = root.getBoolean("required");
    Type type = Type.getEnum(root.getString("type"));

    return new AutoValue_Schema(
        additionalProperties,
        defaultValue,
        description,
        format,
        id,
        location,
        pattern,
        properties,
        reference,
        repeated,
        required,
        type);
  }

  /** @return the schema of the additionalProperties, or null if none. */
  @Nullable
  public abstract Schema additionalProperties();

  /** @return the default value. */
  public abstract String defaultValue();

  /** @return the description. */
  public abstract String description();

  /** @return the format. */
  public abstract Format format();

  /** @return the ID. */
  public abstract String id();

  /** @return the location. */
  public abstract String location();

  /** @return the pattern. */
  public abstract String pattern();

  /** @return the map of property names to schemas. */
  public abstract Map<String, Schema> properties();

  /** @return the reference. */
  public abstract String reference();

  /** @return whether or not the schema is repeated. */
  public abstract boolean repeated();

  /** @return whether or not the schema is required. */
  public abstract boolean required();

  public abstract Type type();
}
