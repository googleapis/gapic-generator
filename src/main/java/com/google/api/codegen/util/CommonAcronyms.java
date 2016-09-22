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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import java.util.Map;

/**
 * Utility class to replace fully capitalized common acronyms with an upper camel interpretation.
 */
public class CommonAcronyms {
  private static final ImmutableMap<String, String> ACRONYMS =
      ImmutableMap.<String, String>builder()
          .put("IAM", "Iam")
          .put("HTTP", "Http")
          .put("XML", "Xml")
          .put("API", "Api")
          .build();

  private static final ImmutableSet<String> AMBIGUOUS_CASES =
      ImmutableSet.<String>builder().add("APIAM").build();

  public static String replaceAcronyms(String str) {
    if (hasAmbiguousReplacements(str)) {
      throw new IllegalArgumentException(
          "CommonAcronyms: A situation where combined acronyms was found. Acronym replacement "
              + "is ambiguous. ex. \"APIAMName\".");
    }
    for (Map.Entry<String, String> entry : ACRONYMS.entrySet()) {
      str = str.replace(entry.getKey(), entry.getValue());
    }
    return str;
  }

  private static boolean hasAmbiguousReplacements(String str) {
    for (String ambiguousCase : AMBIGUOUS_CASES) {
      if (str.contains(ambiguousCase)) {
        return true;
      }
    }
    return false;
  }
}
