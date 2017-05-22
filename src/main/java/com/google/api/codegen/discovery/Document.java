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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A representation of a Discovery Document.
 *
 * <p>Note that this class is not necessarily a 1-1 mapping of the official specification. For
 * example, this class combines all methods in the Discovery Document into one list for convenience.
 */
@AutoValue
public abstract class Document {

  // TODO(saicheems): Assert that all references link to a valid schema?

  private static final String CLOUD_PLATFORM_SCOPE =
      "https://www.googleapis.com/auth/cloud-platform";

  /**
   * Returns a document constructed from root.
   *
   * @param root the root node to parse.
   * @return a document.
   */
  public static Document from(DiscoveryNode root) {
    AuthType authType;
    DiscoveryNode scopesNode = root.getObject("auth").getObject("oauth2").getObject("scopes");
    if (scopesNode.isEmpty()) {
      authType = AuthType.API_KEY;
    } else if (scopesNode.has(CLOUD_PLATFORM_SCOPE)) {
      authType = AuthType.ADC;
    } else {
      authType = AuthType.OAUTH_3L;
    }
    String canonicalName = root.getString("canonicalName");
    String description = root.getString("description");
    Map<String, Schema> schemas = parseSchemas(root);
    List<Method> methods = parseMethods(root, "");
    Collections.sort(methods); // Ensure methods are ordered alphabetically by their ID.
    String name = root.getString("name");
    if (canonicalName.isEmpty()) {
      canonicalName = name;
    }
    String revision = root.getString("revision");
    String rootUrl = root.getString("rootUrl");
    String servicePath = root.getString("servicePath");
    String title = root.getString("title");
    String version = root.getString("version");
    boolean versionModule = root.getBoolean("version_module");

    return new AutoValue_Document(
        "", // authInstructionsUrl (only intended to be overridden).
        authType,
        canonicalName,
        description,
        "", // discoveryDocUrl (only intended to be overridden).
        methods,
        name,
        revision,
        rootUrl,
        schemas,
        servicePath,
        title,
        version,
        versionModule);
  }

  /**
   * Returns the schema the given schema references.
   *
   * <p>If the reference property of a schema is non-empty, then it references another schema. This
   * method returns the schema that the given schema eventually references. If the given schema does
   * not reference another schema, it is returned. If schema is null, null is returned.
   *
   * @param schema the schema to dereference.
   * @return the first non-reference schema, or null if schema is null.
   */
  public Schema dereferenceSchema(Schema schema) {
    if (schema == null) {
      return null;
    }
    if (!schema.reference().isEmpty()) {
      return dereferenceSchema(schemas().get(schema.reference()));
    }
    return schema;
  }

  private static List<Method> parseMethods(DiscoveryNode root, String path) {
    List<Method> methods = new ArrayList<>();
    DiscoveryNode methodsNode = root.getObject("methods");
    List<String> resourceNames = methodsNode.getFieldNames();
    if (!path.isEmpty()) {
      path += ".";
    }
    for (String name : resourceNames) {
      methods.add(Method.from(methodsNode.getObject(name), path + "methods." + name));
    }
    DiscoveryNode resourcesNode = root.getObject("resources");
    resourceNames = resourcesNode.getFieldNames();
    for (String name : resourceNames) {
      methods.addAll(parseMethods(resourcesNode.getObject(name), path + "resources." + name));
    }
    return methods;
  }

  private static Map<String, Schema> parseSchemas(DiscoveryNode root) {
    Map<String, Schema> schemas = new HashMap<>();
    DiscoveryNode schemasNode = root.getObject("schemas");
    for (String name : schemasNode.getFieldNames()) {
      schemas.put(name, Schema.from(schemasNode.getObject(name), "schemas." + name));
    }
    return schemas;
  }

  /** @return the auth instructions URL. */
  @JsonProperty("authInstructionsUrl")
  public abstract String authInstructionsUrl();

  /** @return the auth type. */
  @JsonProperty("authType")
  public abstract AuthType authType();

  /** @return the canonical name. */
  @JsonProperty("canonicalName")
  public abstract String canonicalName();

  /** @return the description. */
  @JsonProperty("description")
  public abstract String description();

  /** @return the discovery document URL. */
  @JsonProperty("discoveryDocUrl")
  public abstract String discoveryDocUrl();

  /** @return the list of all methods. */
  @JsonProperty("methods")
  public abstract List<Method> methods();

  /** @return the name. */
  @JsonProperty("name")
  public abstract String name();

  /** @return the revision. */
  @JsonProperty("revision")
  public abstract String revision();

  /** @return the root URL. */
  @JsonProperty("rootUrl")
  public abstract String rootUrl();

  /** @return the map of schema IDs to schemas. */
  @JsonProperty("schemas")
  public abstract Map<String, Schema> schemas();

  /** @return the service path. */
  @JsonProperty("servicePath")
  public abstract String servicePath();

  /** @return the title. */
  @JsonProperty("title")
  public abstract String title();

  /** @return the version. */
  @JsonProperty("version")
  public abstract String version();

  /** @return whether or not to version the module. */
  @JsonProperty("versionModule")
  public abstract boolean versionModule();

  /** The set of supported authentication formats. */
  public enum AuthType {
    /** No auth. */
    NONE,
    /** Application default credentials based auth. */
    ADC,
    /** 3-legged OAuth based auth. */
    OAUTH_3L,
    /** API key based auth. */
    API_KEY,
  }
}
