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

@AutoValue
public abstract class Document {

  /**
   * Returns a document constructed from root.
   *
   * @param root the root node to parse.
   * @return a document.
   */
  public static Document from(DiscoveryNode root) {
    String description = root.getString("description");
    List<Method> methods = parseMethods(root);
    String name = root.getString("name");
    String revision = root.getString("revision");
    String rootUrl = root.getString("rootUrl");
    Map<String, Schema> schemas = parseSchemas(root);
    String servicePath = root.getString("servicePath");
    String title = root.getString("title");
    String version = root.getString("version");
    boolean versionModule = root.getBoolean("version_module");

    return new AutoValue_Document(
        description,
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

  private static List<Method> parseMethods(DiscoveryNode root) {
    return parseMethods(root, new ArrayList<String>());
  }

  private static List<Method> parseMethods(DiscoveryNode root, List<String> resourceHierarchy) {
    List<Method> methods = new ArrayList<>();

    DiscoveryNode methodsNode = root.getObject("methods");
    List<String> resourceNames = methodsNode.fieldNames();
    for (String resourceName : resourceNames) {
      List<String> copy = new ArrayList<>(resourceHierarchy);
      copy.add(resourceName);
      methods.add(Method.from(methodsNode.getObject(resourceName), copy));
    }
    DiscoveryNode resourcesNode = root.getObject("resources");
    resourceNames = resourcesNode.fieldNames();
    for (String resourceName : resourceNames) {
      List<String> copy = new ArrayList<>(resourceHierarchy);
      copy.add(resourceName);
      methods.addAll(parseMethods(resourcesNode.getObject(resourceName), copy));
    }
    return methods;
  }

  private static Map<String, Schema> parseSchemas(DiscoveryNode root) {
    Map<String, Schema> schemas = new HashMap<>();
    DiscoveryNode schemasNode = root.getObject("schemas");
    for (String name : schemasNode.fieldNames()) {
      schemas.put(name, Schema.from(schemasNode.getObject(name)));
    }
    return schemas;
  }

  /** @return the description. */
  public abstract String description();

  /** @return the list of all methods. */
  public abstract List<Method> methods();

  /** @return the name. */
  public abstract String name();

  /** @return the revision. */
  public abstract String revision();

  /** @return the root URL. */
  public abstract String rootUrl();

  /** @return the map of schema IDs to schemas. */
  public abstract Map<String, Schema> schemas();

  /** @return the service path. */
  public abstract String servicePath();

  /** @return the title. */
  public abstract String title();

  /** @return the version. */
  public abstract String version();

  /** @return whether or not to version the module. */
  public abstract boolean versionModule();
}
