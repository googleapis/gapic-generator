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
    List<String> parameterOrder = new ArrayList<>();
    for (DiscoveryNode nameNode : root.getArray("parameterOrder").getElements()) {
      parameterOrder.add(nameNode.asText());
    }

    DiscoveryNode parametersNode = root.getObject("parameters");
    HashMap<String, Schema> parameters = new HashMap<>();
    for (String name : root.getObject("parameters").getFieldNames()) {
      Schema schema = Schema.from(parametersNode.getObject(name), null);
      // TODO: Remove these checks once we're sure that parameters can't be objects/arrays.
      // This is based on the assumption that these types can't be serialized as a query or path parameter.
      Preconditions.checkState(schema.type() != Schema.Type.ANY);
      Preconditions.checkState(schema.type() != Schema.Type.ARRAY);
      Preconditions.checkState(schema.type() != Schema.Type.OBJECT);
      parameters.put(name, schema);
    }

    Schema request = Schema.from(root.getObject("request"), null);
    if (request.reference().isEmpty()) {
      request = null;
    }
    Schema response = Schema.from(root.getObject("response"), null);
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
            httpMethod,
            id,
            parameterOrder,
            parameters,
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

  public Node parent() {
    return parent;
  }

  /** @return the description. */
  public abstract String description();

  /** @return the HTTP method. */
  public abstract String httpMethod();

  /** @return the ID. */
  public abstract String id();

  /** @return the order of parameter names. */
  public abstract List<String> parameterOrder();

  /** @return the map of parameter names to schemas. */
  public abstract Map<String, Schema> parameters();

  /** @return the request schema, or null if none. */
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
}
