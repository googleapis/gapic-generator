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
import com.google.api.tools.framework.aspects.http.model.HttpAttribute.PathSegment;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;

import autovalue.shaded.com.google.common.common.collect.ImmutableMap;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Class for collection config generator.
 */
public class CollectionPatternsGenerator implements MethodConfigGenerator {

  private static final String CONFIG_KEY_NAME_PATTERN = "name_pattern";
  private static final String CONFIG_KEY_ENTITY_NAME = "entity_name";
  private static final String CONFIG_KEY_FIELD_NAME_PATTERNS = "field_name_patterns";

  private final ImmutableMap<String, String> nameMap;

  public CollectionPatternsGenerator(Interface service) {
    nameMap = getResourceToEntityNameMap(service.getMethods());
  }

  /**
   * Generates the field_name_pattern configuration for a method.
   */
  @Override
  public Map<String, Object> generate(Method method) {
    Map<String, Object> fieldPatternMap = new LinkedHashMap<String, Object>();
    for (CollectionPattern collectionPattern : getCollectionPatternsFromMethod(method)) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      fieldPatternMap.put(collectionPattern.getFieldPath(), nameMap.get(resourceNameString));
    }
    if (fieldPatternMap.size() > 0) {
      Map<String, Object> result = new LinkedHashMap<String, Object>();
      result.put(CONFIG_KEY_FIELD_NAME_PATTERNS, fieldPatternMap);
      return result;
    } else {
      return null;
    }
  }

  /**
   * Generates a collection configurations section.
   */
  public List<Object> generateCollectionConfigs() {
    List<Object> output = new LinkedList<Object>();
    for (String resourceNameString : nameMap.keySet()) {
      Map<String, Object> collectionMap = new LinkedHashMap<String, Object>();
      collectionMap.put(CONFIG_KEY_NAME_PATTERN, resourceNameString);
      collectionMap.put(CONFIG_KEY_ENTITY_NAME, nameMap.get(resourceNameString));
      output.add(collectionMap);
    }
    return output;
  }

  /**
   * Returns a list of CollectionPattern objects.
   */
  public static List<CollectionPattern> getCollectionPatternsFromMethod(Method method) {
    List<CollectionPattern> collectionPatterns = new LinkedList<CollectionPattern>();
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    for (PathSegment pathSegment : httpAttr.getPath()) {
      if (CollectionPattern.isValidCollectionPattern(pathSegment)) {
        collectionPatterns.add(CollectionPattern.create((FieldSegment) pathSegment));
      }
    }
    return collectionPatterns;
  }

  /**
   * Examines all of the resource paths used by the methods, and returns a map from each unique
   * resource paths to a short name used by the collection configuration.
   */
  private static ImmutableMap<String, String> getResourceToEntityNameMap(List<Method> methods) {
    // Using a map with the string representation of the resource path to avoid duplication
    // of equivalent paths.
    // Using a TreeMap in particular so that the ordering is deterministic
    // (useful for testability).
    Map<String, CollectionPattern> specs = new TreeMap<>();
    for (Method method : methods) {
      for (CollectionPattern collectionPattern : getCollectionPatternsFromMethod(method)) {
        String resourcePath = collectionPattern.getTemplatizedResourcePath();
        // If there are multiple field segments with the same resource path, the last
        // one will be used, making the output deterministic. Also, the first field path
        // encountered tends to be simply "name" because it is the corresponding create
        // API method for the type.
        specs.put(resourcePath, collectionPattern);
      }
    }

    Set<String> usedNameSet = new HashSet<>();
    ImmutableMap.Builder<String, String> nameMapBuilder = ImmutableMap.<String, String>builder();
    for (CollectionPattern collectionPattern : specs.values()) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      String entityNameString = collectionPattern.getUniqueName(usedNameSet);
      usedNameSet.add(entityNameString);
      nameMapBuilder.put(resourceNameString, entityNameString);
    }
    return nameMapBuilder.build();
  }
}
