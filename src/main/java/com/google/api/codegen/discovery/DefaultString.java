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
package com.google.api.codegen.discovery;

import com.google.api.codegen.Inflector;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Nullable;

/**
 * Creates default string from path patterns.
 */
public class DefaultString {

  private static final String WILDCARD_PATTERN = "[^/]*";

  /**
   * Returns a default string from `pattern`, or null if the pattern is not supported.
   */
  public static String forPattern(String pattern) {
    // We only care about patterns that has alternating literal and wildcard like
    //  ^foo/[^/]*/bar/[^/]*$
    // Ignore if what we get looks nothing like this.
    if (pattern == null
        || !pattern.startsWith("^")
        || !pattern.endsWith("$")
        || substrCount(pattern, "/") != substrCount(pattern, WILDCARD_PATTERN) * 3 - 1) {
      return null;
    }
    pattern = pattern.substring(1, pattern.length() - 1);
    ImmutableList<Elem> elems = parse(pattern);
    if (!validElems(elems)) {
      return null;
    }

    StringBuilder ret = new StringBuilder();
    for (int i = 0; i < elems.size(); i += 2) {
      String literal = elems.get(i).getLiteral();
      ret.append('/')
          .append(literal)
          .append("/{MY-")
          .append(Inflector.singularize(literal).toUpperCase())
          .append('}');
    }
    return ret.substring(1);
  }

  /**
   * Counts the number of non-overlapping instances of `needle` in `haystack`.
   */
  private static int substrCount(String haystack, String needle) {
    int count = 0;
    int fromIndex = 0;
    for (; ; ) {
      int index = haystack.indexOf(needle, fromIndex);
      if (index < 0) {
        return count;
      }
      fromIndex = index + needle.length();
      count++;
    }
  }

  /**
   * Parses pattern, with the leading '^' and trailing '$' removed, into a list representing the
   * pattern.
   */
  private static ImmutableList<Elem> parse(String pattern) {
    List<Elem> elems = new ArrayList<>();
    while (pattern.length() > 0) {
      int slash;
      if (pattern.startsWith(WILDCARD_PATTERN)) {
        elems.add(Elem.WILDCARD);
        pattern = pattern.substring(WILDCARD_PATTERN.length());
      } else if ((slash = pattern.indexOf("/")) >= 0) {
        elems.add(Elem.createLiteral(pattern.substring(0, slash)));
        pattern = pattern.substring(slash);
      } else {
        elems.add(Elem.createLiteral(pattern));
        pattern = "";
      }

      if (pattern.startsWith("/")) {
        pattern = pattern.substring(1);
      }
    }
    return ImmutableList.<Elem>copyOf(elems);
  }

  /**
   * Returns whether the pattern represented by the list is in a form we expect.
   *
   * A valid pattern must have the same number of literals and wildcards, alternating, and starts
   * with a literal. Literals must consists of only letters.
   */
  private static boolean validElems(ImmutableList<Elem> elems) {
    if (elems.size() % 2 != 0) {
      return false;
    }
    ImmutableList<ElemType> expect =
        ImmutableList.<ElemType>of(ElemType.LITERAL, ElemType.WILDCARD);
    for (int i = 0; i < elems.size(); i++) {
      if (elems.get(i).getType() != expect.get(i % expect.size())) {
        return false;
      }
    }
    for (int i = 0; i < elems.size(); i += 2) {
      for (char c : elems.get(i).getLiteral().toCharArray()) {
        if (!Character.isLetter(c)) {
          return false;
        }
      }
    }
    return true;
  }

  enum ElemType {
    LITERAL,
    WILDCARD
  }

  @AutoValue
  abstract static class Elem {
    abstract ElemType getType();

    @Nullable
    abstract String getLiteral();

    private static final Elem WILDCARD = new AutoValue_DefaultString_Elem(ElemType.WILDCARD, null);

    private static Elem createLiteral(String lit) {
      return new AutoValue_DefaultString_Elem(ElemType.LITERAL, lit);
    }
  }
}
