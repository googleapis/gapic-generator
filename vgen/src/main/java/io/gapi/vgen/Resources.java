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
package io.gapi.vgen;

import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.FieldSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.LiteralSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.MethodKind;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.PathSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.WildcardSegment;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Utility class with methods for working with resource names.
 */
public class Resources {

  /**
   * Generate parameter names for wildcards in a resource path, based on singularized
   * collection names.
   */
  public static Iterable<String> getParamsForResourceNameWildcards(FieldSegment fieldSegment) {
    Preconditions.checkArgument(isTemplateFieldSegment(fieldSegment));

    // Using a LinkedHashSet to preserve insertion order
    Set<String> paramList = new LinkedHashSet<>();
    for (String collectionName : getWildcardCollectionNames(fieldSegment)) {
      // TODO(shinfan): Replace this simple singularizer with one that may be expected to
      // work across all APIs, such as Google NLP singularizer or Wordnet stemmer.
      String paramName = Inflector.singularize(collectionName);
      // TODO (garrettjones) handle potential non-uniqueness of parameter names;
      // make consistent with templatize().
      paramList.add(paramName);
    }

    return paramList;
  }

  /**
   * Returns the field segments referenced in the http attributes of the given methods.
   * Each FieldSegment will be composed of a field name and a path segment, representing an
   * abstract resource name with wildcards.
   */
  public static Iterable<FieldSegment> getFieldSegmentsFromHttpPaths(List<Method> methods) {
    // Using a map with the string representation of the resource path to avoid duplication
    // of field segments with equivalent paths.
    // Using a TreeMap in particular so that the ordering is deterministic
    // (useful for testability).
    Map<String, FieldSegment> specs = new TreeMap<>();

    for (Method method : methods) {
      HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
      for (PathSegment pathSegment : httpAttr.getPath()) {
        if (isTemplateFieldSegment(pathSegment)) {
          FieldSegment fieldSegment = (FieldSegment) pathSegment;
          String resourcePath = PathSegment.toSyntax(fieldSegment.getSubPath());
          // If there are multiple field segments with the same resource path, the last
          // one will be used, making the output deterministic. Also, the first field path
          // encountered tends to be simply "name" because it is the corresponding create
          // API method for the type.
          specs.put(resourcePath, fieldSegment);
        }
      }
    }

    return specs.values();
  }

  /**
   * Returns true if the method is idempotent according to the http method kind
   * (GET, PUT, DELETE).
   */
  public static boolean isIdempotent(Method method) {
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    MethodKind methodKind = httpAttr.getMethodKind();
    return methodKind.isIdempotent();
  }

  /**
   * Returns the templatized form of the resource path (replacing each * with a name) which
   * can be used with PathTemplate.
   */
  public static String templatize(FieldSegment fieldSegment) {
    StringBuilder builder = new StringBuilder();

    for (String collectionName : getWildcardCollectionNames(fieldSegment)) {
        String paramName = Inflector.singularize(collectionName);
        builder.append("/" + collectionName + "/{" + paramName + "}");
    }

    return builder.toString();
  }

  private static boolean isTemplateFieldSegment(PathSegment pathSegment) {
    if (!(pathSegment instanceof FieldSegment)) {
      return false;
    }
    FieldSegment fieldSegment = (FieldSegment) pathSegment;

    ImmutableList<PathSegment> subPath = fieldSegment.getSubPath();
    if (subPath == null) {
      return false;
    }
    if (subPath.size() == 1 && subPath.get(0) instanceof WildcardSegment) {
      return false;
    }

    return true;
  }

  private static List<String> getWildcardCollectionNames(FieldSegment fieldSegment) {
    List<String> collectionNames = new ArrayList<>();

    PathSegment lastSegment = null;
    for (PathSegment pathSegment : fieldSegment.getSubPath()) {
      if (pathSegment instanceof WildcardSegment && lastSegment != null
          && lastSegment instanceof LiteralSegment) {
        String collectionName = ((LiteralSegment) lastSegment).getLiteral();
        collectionNames.add(collectionName);
      }
      lastSegment = pathSegment;
    }

    return collectionNames;
  }
}
