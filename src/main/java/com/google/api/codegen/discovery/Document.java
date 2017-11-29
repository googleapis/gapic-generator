/* Copyright 2017 Google LLC
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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.gson.internal.LinkedTreeMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A representation of a Discovery Document.
 *
 * <p>Note that this class is not necessarily a 1-1 mapping of the official specification. For
 * example, this class combines all methods in the Discovery Document into one list for convenience.
 */
@AutoValue
public abstract class Document implements Node {

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

    ImmutableList.Builder<String> authScopes = new Builder<>();
    if (scopesNode.isEmpty()) {
      authType = AuthType.API_KEY;
    } else {
      authScopes.addAll(scopesNode.getFieldNames());
      if (scopesNode.has(CLOUD_PLATFORM_SCOPE)) {
        authType = AuthType.ADC;
      } else {
        authType = AuthType.OAUTH_3L;
      }
    }
    String canonicalName = root.getString("canonicalName");
    String description = root.getString("description");
    String id = root.getString("id");
    Map<String, Schema> schemas = parseSchemas(root);
    List<Method> methods = parseMethods(root);
    Collections.sort(methods); // Ensure methods are ordered alphabetically by their ID.
    String ownerDomain = root.getString("ownerDomain");
    String name = root.getString("name");
    if (canonicalName.isEmpty()) {
      canonicalName = name;
    }
    Map<String, List<Method>> resources = parseResources(root);
    String revision = root.getString("revision");
    String rootUrl = root.getString("rootUrl");
    String servicePath = root.getString("servicePath");
    String title = root.getString("title");
    String version = root.getString("version");
    boolean versionModule = root.getBoolean("version_module");

    String baseUrl =
        root.has("baseUrl")
            ? root.getString("baseUrl")
            : (rootUrl + Strings.nullToEmpty(root.getString("basePath")));

    Document thisDocument =
        new AutoValue_Document(
            "", // authInstructionsUrl (only intended to be overridden).
            authScopes.build(),
            authType,
            baseUrl,
            canonicalName,
            description,
            "", // discoveryDocUrl (only intended to be overridden).
            id,
            methods,
            name,
            ownerDomain,
            resources,
            revision,
            rootUrl,
            schemas,
            servicePath,
            title,
            version,
            versionModule);

    for (Schema schema : schemas.values()) {
      schema.setParent(thisDocument);
    }
    for (Method method : methods) {
      method.setParent(thisDocument);
    }
    for (List<Method> resourceMethods : resources.values()) {
      for (Method method : resourceMethods) {
        method.setParent(thisDocument);
      }
    }

    return thisDocument;
  }

  private static Map<String, List<Method>> parseResources(DiscoveryNode root) {
    List<Method> methods = new ArrayList<>();
    DiscoveryNode methodsNode = root.getObject("methods");
    List<String> resourceNames = methodsNode.getFieldNames();
    for (String name : resourceNames) {
      methods.add(Method.from(methodsNode.getObject(name), null));
    }
    Map<String, List<Method>> resources = new LinkedTreeMap<>();
    DiscoveryNode resourcesNode = root.getObject("resources");
    resourceNames = resourcesNode.getFieldNames();
    for (String name : resourceNames) {
      resources.put(name, parseMethods(resourcesNode.getObject(name)));
    }
    return resources;
  }

  private static List<Method> parseMethods(DiscoveryNode root) {
    List<Method> methods = new ArrayList<>();
    DiscoveryNode methodsNode = root.getObject("methods");
    List<String> resourceNames = methodsNode.getFieldNames();
    for (String name : resourceNames) {
      methods.add(Method.from(methodsNode.getObject(name), null));
    }
    DiscoveryNode resourcesNode = root.getObject("resources");
    resourceNames = resourcesNode.getFieldNames();
    for (String name : resourceNames) {
      methods.addAll(parseMethods(resourcesNode.getObject(name)));
    }
    return methods;
  }

  private static Map<String, Schema> parseSchemas(DiscoveryNode root) {
    Map<String, Schema> schemas = new HashMap<>();
    DiscoveryNode schemasNode = root.getObject("schemas");
    for (String name : schemasNode.getFieldNames()) {
      schemas.put(name, Schema.from(schemasNode.getObject(name), name, null));
    }
    return schemas;
  }

  /** @return the parent Node that contains this node. */
  @JsonIgnore @Nullable private Node parent;

  @Override
  public Node parent() {
    return parent;
  }

  // Package private for use by other Node objects.
  void setParent(Node parent) {
    this.parent = parent;
  }

  /** @return the auth instructions URL. */
  @JsonProperty("authInstructionsUrl")
  public abstract String authInstructionsUrl();

  /** @return The list of OAuth2 scopes; this can be empty. */
  public abstract List<String> authScopes();

  /** @return the auth type. */
  @JsonProperty("authType")
  public abstract AuthType authType();

  /** @return the base URL. */
  @JsonProperty("baseUrl")
  public abstract String baseUrl();

  /** @return the canonical name. */
  @JsonProperty("canonicalName")
  public abstract String canonicalName();

  /** @return the description. */
  @JsonProperty("description")
  public abstract String description();

  /** @return the discovery document URL. */
  @JsonProperty("discoveryDocUrl")
  public abstract String discoveryDocUrl();

  /** @return the ID of the Discovery document for the API. */
  @Override
  @JsonProperty("id")
  public abstract String id();

  /** @return the list of all methods. */
  @JsonProperty("methods")
  public abstract List<Method> methods();

  /** @return the name. */
  @JsonProperty("name")
  public abstract String name();

  /** @return the name. */
  @JsonProperty("name")
  public abstract String ownerDomain();

  /** @return the revision. */
  @JsonProperty("revision")
  public abstract Map<String, List<Method>> resources();

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
