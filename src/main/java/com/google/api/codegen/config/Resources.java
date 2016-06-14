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
package com.google.api.codegen.config;

import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.FieldSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.LiteralSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.MethodKind;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.PathSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.WildcardSegment;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Utility class with methods for working with resource names.
 */
public class Resources {

  /**
   * Returns the field segments referenced in the http attributes of the given methods. Each
   * FieldSegment will be composed of a field name and a path segment, representing an abstract
   * resource name with wildcards.
   */
  public static Iterable<FieldSegment> getFieldSegmentsFromHttpPaths(List<Method> methods) {
    // Using a map with the string representation of the resource path to avoid duplication
    // of field segments with equivalent paths.
    // Using a TreeMap in particular so that the ordering is deterministic
    // (useful for testability).
    Map<String, FieldSegment> specs = new TreeMap<>();

    for (Method method : methods) {
      for (FieldSegment fieldSegment : getFieldSegmentsFromMethodHttpPath(method)) {
        String resourcePath = PathSegment.toSyntax(fieldSegment.getSubPath());
        // If there are multiple field segments with the same resource path, the last
        // one will be used, making the output deterministic. Also, the first field path
        // encountered tends to be simply "name" because it is the corresponding create
        // API method for the type.
        specs.put(resourcePath, fieldSegment);
      }
    }
    return specs.values();
  }

  /**
   * Returns a list of FieldSegment objects. Each FieldSegment will be composed of a field name and
   * a path segment, representing an abstract resource name with wildcards.
   */
  public static List<FieldSegment> getFieldSegmentsFromMethodHttpPath(Method method) {
    List<FieldSegment> fieldSegments = new LinkedList<FieldSegment>();
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    for (PathSegment pathSegment : httpAttr.getPath()) {
      if (isTemplateFieldSegment(pathSegment)) {
        fieldSegments.add((FieldSegment) pathSegment);
      }
    }
    return fieldSegments;
  }

  public static String getEntityName(FieldSegment segment) {
    Preconditions.checkArgument(isTemplateFieldSegment(segment));
    List<NamedWildcard> namedWildcardList = getNamedWildcards(segment);
    return namedWildcardList.get(namedWildcardList.size() - 1).paramName();
  }

  public static Map<String, String> getResourceToEntityNameMap(Iterable<FieldSegment> segments) {
    Set<String> usedNameSet = new HashSet<>();
    Map<String, String> nameMap = new LinkedHashMap<>();
    for (FieldSegment segment : segments) {
      String resourceNameString = templatize(segment);
      String entityNameString = getUniqueName(getEntityName(segment), usedNameSet);
      usedNameSet.add(entityNameString);
      nameMap.put(resourceNameString, entityNameString);
    }
    return nameMap;
  }

  private static String getUniqueName(String desiredName, Set<String> usedNameSet) {
    String actualName = desiredName;
    int i = 2;
    while (usedNameSet.contains(actualName)) {
      actualName = desiredName + "_" + i;
      i += 1;
    }
    return actualName;
  }

  /**
   * Returns true if the method is idempotent according to the http method kind (GET, PUT, DELETE).
   */
  public static boolean isIdempotent(Method method) {
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    MethodKind methodKind = httpAttr.getMethodKind();
    return methodKind.isIdempotent();
  }

  /**
   * Returns the templatized form of the resource path (replacing each * with a name) which can be
   * used with PathTemplate. Does not produce leading or trailing forward slashes.
   */
  public static String templatize(FieldSegment fieldSegment) {
    List<String> componentStrings = new ArrayList<String>();

    for (NamedWildcard namedWildcard : getNamedWildcards(fieldSegment)) {
      componentStrings.add(namedWildcard.resourcePathString());
    }

    return Joiner.on("/").join(componentStrings);
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

  private static List<NamedWildcard> getNamedWildcards(FieldSegment fieldSegment) {
    List<NamedWildcard> namedWildcardList = new ArrayList<>();

    PathSegment lastSegment = null;
    for (PathSegment pathSegment : fieldSegment.getSubPath()) {
      if (pathSegment instanceof WildcardSegment
          && lastSegment != null
          && lastSegment instanceof LiteralSegment) {
        namedWildcardList.add(
            NamedWildcard.create((LiteralSegment) lastSegment, (WildcardSegment) pathSegment));
      }
      lastSegment = pathSegment;
    }

    return namedWildcardList;
  }
}
