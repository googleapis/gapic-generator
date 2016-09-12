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

import com.google.api.client.util.Strings;

/**
 * A utility class used to get and store unique symbols.
 *
 * If a symbol is already used, the table will try to append an index number onto the end of it.
 * The index will keep increasing until an unused symbol is found.
 */
public class SymbolTable {
  private final Set<String> symbolTable = new HashSet<>();

  public Name getNewSymbol(Name desiredName) {
    //Name actualName = desiredName;
    //for (int i = 2; symbolTable.contains(actualName.toLowerUnderscore()); i++) {
    //  actualName = desiredName.join(Integer.toString(i));
    //}
    String lower = desiredName.toLowerUnderscore();
    String suffix = getSuffix(lower);
    symbolTable.add(lower + suffix);
    if (Strings.isNullOrEmpty(suffix)) {
      return desiredName;
    }
    return desiredName.join(suffix);
  }

  public String getNewSymbol(String desiredName) {
    //String actualName = desiredName;
    //for (int i = 2; symbolTable.contains(actualName); i++) {
    //  actualName = desiredName.concat(Integer.toString(i));
    //}
    String suffix = getSuffix(desiredName);
    symbolTable.add(desiredName + suffix);
    return desiredName + suffix;
  }

  private String getSuffix(String desiredName) {
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
