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

import com.google.common.base.Strings;
import com.google.common.collect.HashMultiset;
import java.util.Set;

/**
 * A utility class used to get and store unique symbols.
 *
 * <p>If a symbol is already used, the table will try to append an index number onto the end of it.
 * The index will keep increasing until an unused symbol is found.
 */
public class SymbolTable {

  private final HashMultiset<String> symbolTable;

  public SymbolTable() {
    symbolTable = HashMultiset.create();
  }

  private SymbolTable(Set<String> seed) {
    symbolTable = HashMultiset.create(seed);
  }

  /**
   * Returns a new SymbolTable seeded with all the words in seed.
   *
   * <p>For example, if seed is {"int"}, a subsequent call to {@link #getNewSymbol(String)} for
   * "int" will return "int2".
   *
   * <p>The behavior of the returned SymbolTable is guaranteed if used with {@link
   * #getNewSymbol(String)}, but not with {@link #getNewSymbol(Name)}.
   */
  public static SymbolTable fromSeed(Set<String> seed) {
    return new SymbolTable(seed);
  }

  /**
   * Returns a unique name, with a numeric suffix in case of conflicts.
   *
   * <p>Not guaranteed to work as expected if used in combination with {@link
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
   * <p>Not guaranteed to work as expected if used in combination with {@link #getNewSymbol(Name)}.
   */
  public String getNewSymbol(String desiredName) {
    String suffix = getAndSaveSuffix(desiredName);
    return desiredName + suffix;
  }

  /**
   * Returns the next numeric suffix that makes desiredName unique.
   *
   * <p>Stores the joined desiredName/suffix in an internal map. For example, if "foo" is passed, ""
   * is returned. If "foo" is passed again, "2" is returned, and then "3" and so on.
   */
  private String getAndSaveSuffix(String desiredName) {
    int count = symbolTable.add(desiredName, 1) + 1;
    return count == 1 ? "" : Integer.toString(count);
  }
}
