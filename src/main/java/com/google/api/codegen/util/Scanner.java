/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.util;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

/**
 * Tokenizes a string. {@code scan()} returns the next token. A token is either the next unicode
 * code point in the string or a special token. If a special token is returned, the string value of
 * the token can be retrieved from {@code token()}.
 *
 * <p>Errors are reported as IllegalArgumentExceptions.
 *
 * <p>Special tokens:
 *
 * <pre>{@code
 * EOF : end of the string.
 *
 * IDENT : An identifier.
 * The identifier may be prefixed with a single '$'. The "body" of the identifier is a unicode letter
 * or underscore followed by a possible empty run of unicode letters, digits, and underscores.
 *
 * INT: Run of decimal digits.
 * Leading zeros are not allowed.
 *
 * STRING: string quoted by " character.
 *   \" escapes the quote character.
 *   \\ escapes the backslash.
 *   \n escapes newline.
 *   \t escapes tab.
 * The string returned by token() is already un-escaped.
 * }</pre>
 */
public class Scanner {
  private static final ImmutableMap<Integer, Integer> ESCAPES =
      ImmutableMap.<Integer, Integer>builder()
          .put((int) '"', (int) '"')
          .put((int) '\\', (int) '\\')
          .put((int) 'b', (int) '\b')
          .put((int) 'n', (int) '\n')
          .put((int) 'r', (int) '\r')
          .put((int) 't', (int) '\t')
          .build();

  public static final int EOF = -1;
  public static final int IDENT = -2;
  public static final int INT = -3;
  public static final int STRING = -4;

  private final String input;
  private int loc;
  private String tokenStr = "";
  private int last;

  public Scanner(String input) {
    this.input = input;
  }

  public int scan() {
    tokenStr = "";
    int codePoint;

    while (true) {
      if (loc >= input.length()) {
        return last = EOF;
      }
      codePoint = input.codePointAt(loc);
      if (!Character.isWhitespace(codePoint)) {
        break;
      }
      loc += Character.charCount(codePoint);
    }

    if (codePoint == '"') {
      StringBuilder sb = new StringBuilder();
      boolean escaped = false;
      loc += Character.charCount(codePoint);

      while (true) {
        Preconditions.checkArgument(loc < input.length(), "unclosed string literal: %s", input);
        codePoint = input.codePointAt(loc);
        if (!escaped && codePoint == '"') {
          loc += Character.charCount(codePoint);
          tokenStr = sb.toString();
          return last = STRING;
        }
        if (!escaped && codePoint == '\\') {
          escaped = true;
          loc += Character.charCount(codePoint);
          continue;
        }
        if (escaped) {
          Integer esc = ESCAPES.get(codePoint);
          Preconditions.checkArgument(
              esc != null, "unrecognized escape '\\%c': %s", (char) codePoint, input);
          escaped = false;
          sb.appendCodePoint(esc);
          loc += Character.charCount(esc);
          continue;
        }
        sb.appendCodePoint(codePoint);

        loc += Character.charCount(codePoint);
      }
    }

    if (digit(codePoint)) {
      int start = loc;
      loc += Character.charCount(codePoint);
      while (true) {
        if (loc >= input.length()) {
          break;
        }
        codePoint = input.codePointAt(loc);
        if (!digit(codePoint)) {
          break;
        }
        loc += Character.charCount(codePoint);
      }

      tokenStr = input.substring(start, loc);
      if (input.codePointCount(start, loc) > 1 && tokenStr.startsWith("0")) {
        throw new IllegalArgumentException(
            String.format("leading zero not allowed: %s: %s", tokenStr, input));
      }
      return last = INT;
    }

    if (identLead(codePoint) || codePoint == '$') {
      // If the identifier starts with '$', it isn't valid yet, since it needs the body too.
      boolean valid = identLead(codePoint);
      int start = loc;
      loc += Character.charCount(codePoint);
      while (true) {
        if (loc >= input.length()) {
          Preconditions.checkArgument(
              valid, "identifier needs a letter or underscore after '$': %s", input);
          tokenStr = input.substring(start);
          return last = IDENT;
        }
        codePoint = input.codePointAt(loc);
        if (valid && !identFollow(codePoint)) {
          tokenStr = input.substring(start, loc);
          return last = IDENT;
        }
        Preconditions.checkArgument(
            valid || identLead(codePoint),
            "identifier needs a letter or underscore after '$': %s",
            input);
        loc += Character.charCount(codePoint);
        valid = true;
      }
    }
    // Consume and return the next character
    loc += Character.charCount(codePoint);
    return last = codePoint;
  }

  public int lastToken() {
    return last;
  }

  public String tokenStr() {
    return tokenStr;
  }

  public String input() {
    return input;
  }

  public int pos() {
    return loc;
  }

  private static boolean digit(int codePoint) {
    return codePoint >= '0' && codePoint <= '9';
  }

  private static boolean identLead(int codePoint) {
    return Character.isLetter(codePoint) || codePoint == '_';
  }

  private static boolean identFollow(int codePoint) {
    return identLead(codePoint) || Character.isDigit(codePoint);
  }
}
