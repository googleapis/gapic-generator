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
package com.google.api.codegen.configgen;

import com.google.api.tools.framework.model.Method;
import java.util.LinkedHashMap;
import java.util.Map;

/** Class for collection config generator. */
public class FieldNamePatternConfigGenerator implements MethodConfigGenerator {

  private static final String CONFIG_KEY_FIELD_NAME_PATTERNS = "field_name_patterns";

  private final Map<String, String> nameMap;

  public FieldNamePatternConfigGenerator(Map<String, String> nameMap) {
    this.nameMap = nameMap;
  }

  /** Generates the field_name_pattern configuration for a method. */
  @Override
  public Map<String, Object> generate(Method method) {
    Map<String, Object> fieldPatternMap = new LinkedHashMap<String, Object>();
    for (CollectionPattern collectionPattern :
        CollectionPattern.getCollectionPatternsFromMethod(method)) {
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
}
