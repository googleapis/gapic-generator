/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.discovery;

import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A representation of a Discovery Document method.
 *
 * <p>Note that this class is not necessarily a 1-1 mapping of the official specification.
 */
@AutoValue
public abstract class Method implements Comparable<Method>, Node {

  /**
   * Returns a method constructed from root.
   *
   * @param root the root node to parse.
   * @return a method.
   */
  public static Method from(DiscoveryNode root, Node parent) {
    String description = root.getString("description");
    String httpMethod = root.getString("httpMethod");
    String id = root.getString("id");
    String path = root.getString("path");
    String flatPath = root.has("flatPath") ? root.getString("flatPath") : path;
    List<String> parameterOrder = new ArrayList<>();
    for (DiscoveryNode nameNode : root.getArray("parameterOrder").getElements()) {
      parameterOrder.add(nameNode.asText());
    }

    DiscoveryNode parametersNode = root.getObject("parameters");
    Map<String, Schema> parameters = new HashMap<>();
    Map<String, Schema> queryParams = new HashMap<>();
    Map<String, Schema> pathParams = new HashMap<>();

    for (String name : root.getObject("parameters").getFieldNames()) {
      Schema schema = Schema.from(parametersNode.getObject(name), name, null);
      // TODO: Remove these checks once we're sure that parameters can't be objects/arrays.
      // This is based on the assumption that these types can't be serialized as a query or path parameter.
      Preconditions.checkState(schema.type() != Schema.Type.ANY);
      Preconditions.checkState(schema.type() != Schema.Type.ARRAY);
      Preconditions.checkState(schema.type() != Schema.Type.OBJECT);
      parameters.put(name, schema);
      if (schema.location().toLowerCase().equals("path")) {
        pathParams.put(name, schema);
      } else if (schema.location().toLowerCase().equals("query")) {
        queryParams.put(name, schema);
      }
    }

    Schema request = Schema.from(root.getObject("request"), "request", null);
    if (request.reference().isEmpty()) {
      request = null;
    }
    Schema response = Schema.from(root.getObject("response"), "response", null);
    if (response.reference().isEmpty()) {
      response = null;
    }
    List<String> scopes = new ArrayList<>();
    for (DiscoveryNode scopeNode : root.getArray("scopes").getElements()) {
      scopes.add(scopeNode.asText());
    }
    boolean supportsMediaDownload = root.getBoolean("supportsMediaDownload");
    boolean supportsMediaUpload = root.getBoolean("supportsMediaUpload");

    Method thisMethod =
        new AutoValue_Method(
            description,
            flatPath,
            httpMethod,
            id,
            parameterOrder,
            parameters,
            path,
            pathParams,
            queryParams,
            request,
            response,
            scopes,
            supportsMediaDownload,
            supportsMediaUpload);

    thisMethod.parent = parent;
    if (request != null) {
      request.setParent(thisMethod);
    }
    if (response != null) {
      response.setParent(thisMethod);
    }
    for (Schema schema : parameters.values()) {
      schema.setParent(thisMethod);
    }
    return thisMethod;
  }

  @Override
  public int compareTo(Method other) {
    return id().compareTo(other.id());
  }

  /** @return the parent Node. */
  private Node parent;

  void setParent(Node parent) {
    this.parent = parent;
  }

  @Override
  public Node parent() {
    return parent;
  }

  /** @return the description. */
  public abstract String description();

  /** @return the flat URI path of this REST method. */
  public abstract String flatPath();

  /** @return the HTTP method. */
  public abstract String httpMethod();

  /** @return the ID. */
  @Override
  public abstract String id();

  /** @return the order of parameter names. */
  public abstract List<String> parameterOrder();

  /** @return the map of parameter names to schemas. */
  public abstract Map<String, Schema> parameters();

  /** @return the URI path of this REST method. */
  public abstract String path();

  /** @return the list of path parameters. */
  public abstract Map<String, Schema> pathParams();

  /** @return the list of path parameters. */
  public abstract Map<String, Schema> queryParams();

  /** @return the request's resource object schema, or null if none. */
  @Nullable
  public abstract Schema request();

  /** @return the response schema, or null if none. */
  @Nullable
  public abstract Schema response();

  /** @return the list of scopes. */
  public abstract List<String> scopes();

  /** @return whether or not the method supports media download. */
  public abstract boolean supportsMediaDownload();

  /** @return whether or not the method supports media upload. */
  public abstract boolean supportsMediaUpload();

  /**
   * @return if the method acts on a set of resources whose size may be greater than 1, e.g. List
   *     methods.
   */
  public boolean isPluralMethod() {
    return parameters().containsKey("maxResults");
  }

  public Document getDocument() {
    Node parent = this;
    while (!(parent instanceof Document) && parent != null) {
      parent = this.parent();
    }
    return (Document) parent;
  }
}
