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
package com.google.api.codegen.util;

import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Strings;

/**
 * A utility class used to get and store unique symbols.
 *
 * If a symbol is already used, the table will try to append an index number onto the end of it.
 * The index will keep increasing until an unused symbol is found.
 */
public class SymbolTable {

  private final Set<String> symbolTable = new HashSet<>();

  /**
   * Returns a unique name, with a numeric suffix in case of conflicts.
   *
   * Not guaranteed to work as expected if used in combination with {@link
   * #getNewSymbol(String)}.
   */
  public Name getNewSymbol(Name desiredName) {
    String lower = desiredName.toLowerUnderscore();
    String suffix = getAndSaveSuffix(lower);
    if (Strings.isNullOrEmpty(suffix)) {
      return desiredName;
    }
    return desiredName.join(suffix);
  }

  /**
   * Returns a unique name, with a numeric suffix in case of conflicts.
   *
   * Not guaranteed to work as expected if used in combination with {@link
   * #getNewSymbol(Name)}.
   */
  public String getNewSymbol(String desiredName) {
    String suffix = getAndSaveSuffix(desiredName);
    return desiredName + suffix;
  }

  /**
   * Returns the next numeric suffix that makes desiredName unique.
   *
   * Stores the joined desiredName/suffix in an internal map.
   * For example, if "foo" is passed, "" is returned. If "foo" is passed again,
   * "2" is returned, and then "3" and so on.
   */
  private String getAndSaveSuffix(String desiredName) {
    if (!symbolTable.contains(desiredName)) {
      symbolTable.add(desiredName);
      return "";
    }
    int i = 2;
    while (symbolTable.contains(desiredName + Integer.toString(i))) {
      i++;
    }
    symbolTable.add(desiredName + Integer.toString(i));
    return Integer.toString(i);
  }
}
