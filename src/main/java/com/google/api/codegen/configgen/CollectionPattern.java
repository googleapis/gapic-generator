/* Copyright 2016 Google LLC
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
package com.google.api.codegen.configgen;

import com.google.api.codegen.Inflector;
import com.google.api.codegen.LanguageUtil;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.FieldSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.LiteralSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.PathSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.WildcardSegment;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/** Class representing a CollectionPattern that will be output to the collection config section */
public class CollectionPattern {

  public static CollectionPattern create(FieldSegment fieldSegment) {
    if (!isValidCollectionPattern(fieldSegment)) {
      throw new IllegalArgumentException("Field segment is not a valid collection pattern.");
    }
    return new CollectionPattern(fieldSegment);
  }

  private final FieldSegment fieldSegment;
  private final List<PathSegment> templatizedSubpath;
  private final String simpleName;

  private CollectionPattern(FieldSegment fieldSegment) {
    this.fieldSegment = fieldSegment;

    ImmutableList.Builder<PathSegment> builder = ImmutableList.<PathSegment>builder();
    PathSegment lastSegment = null;
    String simpleName = null;
    for (PathSegment pathSegment : fieldSegment.getSubPath()) {
      if (pathSegment instanceof WildcardSegment) {
        String name = "unknown";
        String suffix = "";
        if (lastSegment != null && lastSegment instanceof LiteralSegment) {
          name =
              LanguageUtil.upperCamelToLowerUnderscore(Inflector.singularize(lastSegment.syntax()));
        }
        if (((WildcardSegment) pathSegment).isUnbounded()) {
          name += "_path";
          suffix = "=**";
        }
        builder.add(new LiteralSegment(String.format("{%s%s}", name, suffix)));
        // simpleName will be set to the name of the last wildcard to be templatized
        simpleName = name;
      } else {
        builder.add(pathSegment);
      }
      lastSegment = pathSegment;
    }
    templatizedSubpath = builder.build();

    if (simpleName == null) {
      // This should never occur because of isValidCollectionPattern checks
      throw new IllegalStateException("Field segment contained no wildcards.");
    }
    this.simpleName = simpleName;
  }

  public String getFieldPath() {
    return fieldSegment.getFieldPath();
  }

  public String getSimpleName() {
    return simpleName;
  }

  public String getUniqueName(Set<String> usedNameSet) {
    String actualName = simpleName;
    int i = 2;
    while (usedNameSet.contains(actualName)) {
      actualName = simpleName + "_" + i;
      i += 1;
    }
    return actualName;
  }

  /**
   * Returns the templatized form of the resource path (replacing each * with a name) which can be
   * used with PathTemplate. Does not produce leading or trailing forward slashes.
   */
  public String getTemplatizedResourcePath() {
    return PathSegment.toSyntax(templatizedSubpath).substring(1);
  }

  /** Returns a list of CollectionPattern objects. */
  public static List<CollectionPattern> getCollectionPatternsFromMethod(Method method) {
    List<CollectionPattern> collectionPatterns = new LinkedList<CollectionPattern>();
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    if (httpAttr != null) {
      for (PathSegment pathSegment : httpAttr.getPath()) {
        if (CollectionPattern.isValidCollectionPattern(pathSegment)) {
          collectionPatterns.add(CollectionPattern.create((FieldSegment) pathSegment));
        }
      }
    }
    return collectionPatterns;
  }

  public static boolean isValidCollectionPattern(PathSegment pathSegment) {
    if (!(pathSegment instanceof FieldSegment)) {
      return false;
    }
    return isValidCollectionPattern((FieldSegment) pathSegment);
  }

  private static boolean isValidCollectionPattern(FieldSegment fieldSegment) {
    ImmutableList<PathSegment> subPath = fieldSegment.getSubPath();
    if (subPath == null) {
      return false;
    }
    if (subPath.size() <= 1) {
      return false;
    }
    boolean containsLiteralWildcardPair = false;
    PathSegment lastSegment = null;
    for (PathSegment segment : subPath) {
      if (segment instanceof WildcardSegment
          && lastSegment != null
          && lastSegment instanceof LiteralSegment) {
        containsLiteralWildcardPair = true;
        break;
      }
      lastSegment = segment;
    }
    if (!containsLiteralWildcardPair) {
      return false;
    }

    return true;
  }
}
