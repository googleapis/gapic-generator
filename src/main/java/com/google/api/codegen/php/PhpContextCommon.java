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
package com.google.api.codegen.php;

import com.google.common.base.Splitter;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhpContextCommon {

  /**
   * A bi-map from full names to short names indicating the import map.
   */
  private final BiMap<String, String> imports = HashBiMap.create();

  public String getMinimallyQualifiedName(String fullName, String shortName) {
    // Derive a short name if possible
    if (imports.containsKey(fullName)) {
      // Short name already there.
      return imports.get(fullName);
    }
    if (imports.containsValue(shortName)) {
      // Short name clashes, use long name.
      return fullName;
    }
    imports.put(fullName, shortName);
    return shortName;
  }

  public List<String> getImports() {
    // Clean up the imports.
    List<String> cleanedImports = new ArrayList<>();
    for (String imported : imports.keySet()) {
      cleanedImports.add(imported);
    }
    Collections.sort(cleanedImports);
    return cleanedImports;
  }

  public Iterable<String> getDocLines(String text) {
    return getDocLinesWithPrefix(text, "");
  }

  public Iterable<String> getDocLinesWithPrefix(String text, String firstLinePrefix) {
    // TODO: convert markdown to phpDoc
    List<String> result = new ArrayList<>();
    String linePrefix = firstLinePrefix;
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add(" * " + linePrefix + line);
      linePrefix = "";
    }
    return result;
  }
}
