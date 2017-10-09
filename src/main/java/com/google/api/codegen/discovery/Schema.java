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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A representation of a Discovery Document schema.
 *
 * <p>Note that this class is not necessarily a 1-1 mapping of the official specification.
 */
@AutoValue
public abstract class Schema implements Node {

  /**
   * Returns the schema this schema references, or this if this schema references no other.
   *
   * <p>If the reference property of a schema is non-empty, then it references another schema. This
   * method returns the schema that this schema eventually references by walking back to a parent
   * document. If the given schema does not reference another schema, this schema is returned.
   *
   * @return the first non-reference schema, or this if this schema references no other.
   */
  public Schema dereference() {
    if (!Strings.isNullOrEmpty(reference())) {
      Node document = parent;
      while (document != null && !(document instanceof Document)) {
        document = document.parent();
      }
      if (document != null) {
        Schema schema = ((Document) document).schemas().get(reference());
        // If a document is an eventual parent of this schema, then reference() must be a key in the
        // document's "schemas" object.
        Preconditions.checkState(schema != null);
        return schema;
      }
    }
    return this;
  }

  /**
   * Returns true if this schema contains a property with the given name.
   *
   * @param name the name of the property.
   * @return whether or not this schema has a property with the given name.
   */
  public boolean hasProperty(String name) {
    return properties().keySet().contains(name);
  }

  /**
   * Returns a schema constructed from root, or an empty schema if root has no children.
   *
   * @param root the root node to parse.
   * @param key in the parent node's schema map, the key that maps to this schema.
   * @param parent the parent of this schema.
   * @return a schema.
   */
  public static Schema from(DiscoveryNode root, String key, Node parent) {
    if (root.isEmpty()) {
      return empty();
    }
    Schema additionalProperties = Schema.from(root.getObject("additionalProperties"), "", null);
    if (additionalProperties.type() == Type.EMPTY && additionalProperties.reference().isEmpty()) {
      additionalProperties = null;
    }
    String defaultValue = root.getString("default");
    String description = root.getString("description");
    Format format = Format.getEnum(root.getString("format"));
    String id = root.getString("id");
    boolean isEnum = !root.getArray("enum").isEmpty();
    Schema items = Schema.from(root.getObject("items"), key, null);
    if (items.type() == Type.EMPTY && items.reference().isEmpty()) {
      items = null;
    }
    String location = root.getString("location");
    String pattern = root.getString("pattern");

    Map<String, Schema> properties = new HashMap<>();
    DiscoveryNode propertiesNode = root.getObject("properties");
    for (String name : propertiesNode.getFieldNames()) {
      properties.put(name, Schema.from(propertiesNode.getObject(name), name, null));
    }

    String reference = root.getString("$ref");
    boolean repeated = root.getBoolean("repeated");
    boolean required = root.getBoolean("required");
    Type type = Type.getEnum(root.getString("type"));

    Schema thisSchema =
        new AutoValue_Schema(
            additionalProperties,
            defaultValue,
            description,
            format,
            id,
            isEnum,
            items,
            key,
            location,
            pattern,
            properties,
            reference,
            repeated,
            required,
            type);
    thisSchema.parent = parent;
    if (items != null) {
      items.setParent(thisSchema);
    }
    for (Schema schema : properties.values()) {
      schema.setParent(thisSchema);
    }
    if (additionalProperties != null) {
      additionalProperties.setParent(thisSchema);
    }

    return thisSchema;
  }

  /** @return a non-null identifier for this schema. */
  public String getIdentifier() {
    return id().isEmpty() ? key() : id();
  }

  public static Schema empty() {
    return new AutoValue_Schema(
        null,
        "",
        "",
        Format.EMPTY,
        "",
        false,
        null,
        "",
        "",
        "",
        new HashMap<String, Schema>(),
        "",
        false,
        false,
        Type.EMPTY);
  }

  @JsonIgnore @Nullable private Node parent;

  /** @return the {@link Node} that contains this Schema. */
  @Nullable
  public Node parent() {
    return parent;
  }

  void setParent(Node parent) {
    this.parent = parent;
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

  /** @return whether or not the schema is an enum. */
  public abstract boolean isEnum();

  /**
   * @return the schema for each element in the array if this schema is an array, or null if not.
   */
  @Nullable
  public abstract Schema items();

  /** @return the key that this object's parent uses to map to this Schema. */
  public abstract String key();

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

  /** @return the type. */
  public abstract Type type();

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
      if (text.isEmpty()) {
        return EMPTY;
      }
      for (Format f : values()) {
        if (f.text.equals(text)) {
          return f;
        }
      }
      // Unexpected formats are ignored.
      return EMPTY;
    }
  }

  public boolean isPathParam() {
    return location().equals("path");
  }

  @Override
  public String toString() {
    return String.format("Schema \"%s\", type %s", getIdentifier(), type());
  }
}
