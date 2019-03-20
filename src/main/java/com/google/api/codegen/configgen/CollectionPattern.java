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

import static com.google.common.collect.ImmutableList.toImmutableList;

import com.google.api.codegen.util.Inflector;
import com.google.api.codegen.util.LanguageUtil;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.FieldSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.LiteralSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.PathSegment;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.WildcardSegment;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Class representing a CollectionPattern that will be output to the collection config section */
public class CollectionPattern {

  public static CollectionPattern create(FieldSegment fieldSegment) {
    if (!isValidCollectionPattern(fieldSegment)) {
      throw new IllegalArgumentException("Field segment is not a valid collection pattern.");
    }

    Stream<PathSegment> wildcardSegments =
        fieldSegment.getSubPath().stream().filter(segment -> segment instanceof WildcardSegment);
    String simpleName =
        Streams.findLast(wildcardSegments)
            .map(segment -> fieldSegment.getSubPath().indexOf(segment))
            .filter(index -> index >= 0)
            .map(index -> buildLastWildcardEntityName(fieldSegment.getSubPath(), index))
            .orElseThrow(() -> new IllegalStateException("Field segment contained no wildcards."));

    List<PathSegment> templatizedSubpath =
        Streams.mapWithIndex(
                fieldSegment.getSubPath().stream(),
                (segment, index) ->
                    buildTemplatizedSegment(segment, (int) index, fieldSegment.getSubPath()))
            .collect(toImmutableList());

    return new CollectionPattern(fieldSegment, templatizedSubpath, simpleName);
  }

  /**
   * Builds a templatized path segment from the specified segment, replacing a wildcard with a
   * literal.
   *
   * @param segment the segment to replace
   * @param index the index of the segment in segments
   * @param pathSegments the full path containing segment at index
   * @return a templatized path segment with any wildcard segment replaced with a literal segment
   *     containing the entity name
   */
  private static PathSegment buildTemplatizedSegment(
      PathSegment segment, int index, List<PathSegment> pathSegments) {
    if (!(segment instanceof WildcardSegment)) {
      return segment;
    }

    String entityName = buildSimpleWildcardEntityName(pathSegments, index);
    if (((WildcardSegment) segment).isUnbounded()) {
      return new LiteralSegment(String.format("{%s_path=**}", entityName));
    }

    return new LiteralSegment(String.format("{%s}", entityName));
  }

  /**
   * Builds an entity name for the final wildcard path segment in pathSegments.
   *
   * <p>The entity name of a singular resource will be prefixed by its parent collection resource.
   *
   * @param pathSegments the full path containing the wildcard segment at wildcardIndex
   * @param wildcardIndex the index of the wildcard segment
   * @return the entity name that corresponds to the wildcard segment
   */
  private static String buildLastWildcardEntityName(
      List<PathSegment> pathSegments, int wildcardIndex) {
    String collectionEntityName = buildSimpleWildcardEntityName(pathSegments, wildcardIndex);

    // If there are an odd number of remaining segments, then the last segment is for a singular
    // resource. For example, "users/*/profile" has an odd number (1) segment after the wildcard.
    // "users/*" has an even number (0) segments after the wildcard.
    int remainingSegments = pathSegments.size() - wildcardIndex - 1;
    if (remainingSegments > 0) {
      PathSegment nextSegment = pathSegments.get(wildcardIndex + 1);
      String singularEntityName = LanguageUtil.upperCamelToLowerUnderscore(nextSegment.syntax());
      String entityName = String.format("%s_%s", collectionEntityName, singularEntityName);
      return appendUnboundWildcardSuffix(pathSegments, wildcardIndex, entityName);
    }

    return appendUnboundWildcardSuffix(pathSegments, wildcardIndex, collectionEntityName);
  }

  /**
   * Appends a suffix to entityName if the wildcard segment is unbounded.
   *
   * @param pathSegments the full path containing the wildcard segment at wildcardIndex
   * @param wildcardIndex the index of the wildcard segment
   * @param entityName the name of the resource entity
   * @return the suffixed entity name
   */
  private static String appendUnboundWildcardSuffix(
      List<PathSegment> pathSegments, int wildcardIndex, String entityName) {
    WildcardSegment wildcardSegment = (WildcardSegment) pathSegments.get(wildcardIndex);
    return wildcardSegment.isUnbounded() ? String.format("%s_path", entityName) : entityName;
  }

  /**
   * Builds the entity name for collection resource.
   *
   * <p>Does not handle singular resources or unbounded wildcards.
   *
   * @param pathSegments the full path containing the wildcard segment at wildcardIndex
   * @param wildcardIndex the index of the wildcard segment
   * @return the entity name for a collection resource
   */
  private static String buildSimpleWildcardEntityName(
      List<PathSegment> pathSegments, int wildcardIndex) {
    PathSegment wildcardSegment = pathSegments.get(wildcardIndex);
    if (wildcardIndex == 0) {
      return "unknown";
    }

    PathSegment prevSegment = pathSegments.get(wildcardIndex - 1);
    if (!(prevSegment instanceof LiteralSegment)) {
      return "unknown";
    }

    return LanguageUtil.upperCamelToLowerUnderscore(Inflector.singularize(prevSegment.syntax()));
  }

  private final FieldSegment fieldSegment;
  private final List<PathSegment> templatizedSubpath;
  private final String simpleName;

  private CollectionPattern(
      FieldSegment fieldSegment, List<PathSegment> templatizedSubpath, String simpleName) {
    this.fieldSegment = fieldSegment;
    this.templatizedSubpath = templatizedSubpath;
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
