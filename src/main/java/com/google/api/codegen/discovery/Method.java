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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

@AutoValue
public abstract class Method implements Comparable<Method> {

  /**
   * Returns a method constructed from root and resourceHierarchy.
   *
   * @param root the root node to parse.
   * @param resourceHierarchy an in-order list of parent resources, or an empty list if none.
   * @return a method.
   */
  public static Method from(DiscoveryNode root, List<String> resourceHierarchy) {
    String description = root.getString("description");
    String httpMethod = root.getString("httpMethod");
    String id = root.getString("id");
    List<String> parameterOrder = new ArrayList<>();
    for (DiscoveryNode idNode : root.getArray("parameterOrder").elements()) {
      parameterOrder.add(idNode.asText());
    }
    Map<String, Schema> parameters = new HashMap<>();
    DiscoveryNode parametersNode = root.getObject("parameters");
    for (String name : parametersNode.fieldNames()) {
      Schema schema = Schema.from(parametersNode.getObject(name));
      parameters.put(name, schema);
    }
    Schema request = Schema.from(root.getObject("request"));
    Schema response = Schema.from(root.getObject("response"));
    List<String> scopes = new ArrayList<>();
    for (DiscoveryNode scopeNode : root.getArray("scopes").elements()) {
      scopes.add(scopeNode.asText());
    }
    boolean supportsMediaDownload = root.getBoolean("supportsMediaDownload");
    boolean supportsMediaUpload = root.getBoolean("supportsMediaUpload");

    return new AutoValue_Method(
        description,
        httpMethod,
        id,
        parameterOrder,
        parameters,
        resourceHierarchy,
        request,
        response,
        scopes,
        supportsMediaDownload,
        supportsMediaUpload);
  }

  /** @return the description. */
  public abstract String description();

  /** @return the HTTP method. */
  public abstract String httpMethod();

  /** @return the ID. */
  public abstract String id();

  /** @return the parameter order. */
  public abstract List<String> parameterOrder();

  /** @return the map of parameter names to schemas. */
  public abstract Map<String, Schema> parameters();

  /** @return the in-order list of parent resources. */
  public abstract List<String> resourceHierarchy();

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

  @Override
  public int compareTo(Method other) {
    return id().compareTo(other.id());
  }
}
