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
package com.google.api.codegen.py;

import com.google.api.tools.framework.model.EnumType;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A symbol table mapping proto enum values to their Pythonic aliases. Handles disambiguation of
 * enums that share the same proto simple name.
 */
public class PythonEnumSymbolTable {
  private final BiMap<EnumType, String> enums;
  private final Set<String> blacklist;

  public PythonEnumSymbolTable(Model model) {
    enums = HashBiMap.create();
    blacklist = new HashSet<>();
    for (TypeRef type : model.getSymbolTable().getDeclaredTypes()) {
      if (type.isEnum() && type.getEnumType().isReachable()) {
        addEnum(type.getEnumType(), type.getEnumType().getSimpleName());
      }
    }
  }

  /** Gets all reachable enum types. */
  public Iterable<EnumType> getEnums() {
    return enums.keySet();
  }

  /** Gets the Pythonic alias corresponding to a given enum type. */
  public String lookupName(EnumType type) {
    return enums.get(type);
  }

  private void addEnum(EnumType type, String name) {
    if (enums.inverse().containsKey(name)) {
      blacklist.add(name); // remove the ambiguous name from circulation
      EnumType oldType = enums.inverse().remove(name);
      addEnum(oldType, disambiguate(oldType, name));
      addEnum(type, disambiguate(type, name));

    } else if (blacklist.contains(name)) {
      addEnum(type, disambiguate(type, name));

    } else {
      enums.put(type, name);
    }
  }

  private String disambiguate(EnumType type, String currentName) {
    ProtoElement parent = type.getParent();
    StringBuilder newName = new StringBuilder(type.getSimpleName());
    while (newName.length() <= currentName.length() && parent != null) {
      newName.insert(0, parent.getSimpleName());
      parent = parent.getParent();
    }
    return newName.toString();
  }
}
