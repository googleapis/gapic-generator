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
import com.google.api.codegen.LanguageUtil;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/** Creates default string from path patterns. */
public class DefaultString {

  @AutoValue
  abstract static class SampleKey {
    abstract String getApiName();

    abstract String getFieldName();

    abstract String getRegexp();

    static SampleKey create(String apiName, String fieldName, String regexp) {
      return new AutoValue_DefaultString_SampleKey(apiName, fieldName, regexp);
    }
  }

  private final String define;
  private final String comment;

  public DefaultString(String define, String comment) {
    this.define = define;
    this.comment = comment;
  }

  public String getDefine() {
    return define;
  }

  public String getComment() {
    return comment;
  }

  /**
   * Returns a non-trivial placeholder for pattern with a no-brace and lower-case format style. An
   * empty string is returned for unrecognized patterns.
   *
   * <p>Variables are formatted according to format (ex: "my-%s").
   *
   * <p>For example: "projects/my-project/logs/my-log" or "my-project"
   */
  public static String getNonTrivialPlaceholder(String pattern, String format) {
    if (pattern != null) {
      String def = forPattern(pattern, format);
      if (def != null) {
        return def;
      }
    }
    return "";
  }

  private static final Set<String> WILDCARD_PATTERNS =
      ImmutableSet.of("[^/]+", "[^/]*", ".+", ".*");

  /** Returns a default string from `pattern`, or null if the pattern is not supported. */
  @VisibleForTesting
  static String forPattern(String pattern, String placeholderFormat) {
    // We only care about patterns that have alternating literal and wildcard like
    //  ^foo/[^/]*/bar/[^/]*$
    // Ignore if what we get looks nothing like this.
    if (pattern == null || !pattern.startsWith("^") || !pattern.endsWith("$")) {
      return null;
    }
    pattern = pattern.substring(1, pattern.length() - 1);
    ImmutableList<Elem> elems = parse(pattern);
    if (!validElems(elems)) {
      return null;
    }

    StringBuilder defaultString = new StringBuilder();
    for (int i = 0; i < elems.size(); i += 2) {
      String literal = elems.get(i).getLiteral();
      String placeholder = Inflector.singularize(literal);
      placeholder = LanguageUtil.lowerCamelToLowerUnderscore(placeholder).replace('_', '-');
      defaultString.append('/').append(literal);
      // if i + 1 >= elems.size() then the terminating segment is a literal. In
      // that case, don't append a placeholder.
      if (i + 1 < elems.size()) {
        defaultString.append('/').append(String.format(placeholderFormat, placeholder));
      }
    }
    return defaultString.substring(1);
  }

  /**
   * Parses pattern, with the leading '^' and trailing '$' removed, into a list representing the
   * pattern.
   */
  private static ImmutableList<Elem> parse(String pattern) {
    List<Elem> elems = new ArrayList<>();
    while (pattern.length() > 0) {
      int slash;
      String wildcardPattern = "";
      for (String w : WILDCARD_PATTERNS) {
        if (pattern.startsWith(w)) {
          wildcardPattern = w;
        }
      }
      if (wildcardPattern.length() > 0) {
        elems.add(Elem.WILDCARD);
        pattern = pattern.substring(wildcardPattern.length());
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
   * <p>A valid pattern must start with a literal and have an alternating pattern of literals and
   * wildcards. Literals must consists of only letters.
   */
  private static boolean validElems(ImmutableList<Elem> elems) {
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
