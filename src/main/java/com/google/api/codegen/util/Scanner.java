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

/**
 * Tokenizes a string. {@code scan()} returns the next token. A token is either the next unicode
 * code point in the string or a special token. If a special token is returned, the string value of
 * the token can be retrieved from {@code token}.
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
 * NUMBER: [0-9]+
 *
 * STRING: string quoted by " character.
 * \" escapes the quote character.
 * The string returned by token() is already un-escaped.
 * }</pre>
 */
public class Scanner {
  public static final int EOF = -1;
  public static final int IDENT = -2;
  public static final int NUMBER = -3;
  public static final int STRING = -4;

  private final String input;
  private int loc;
  private String token = "";

  public Scanner(String input) {
    this.input = input;
  }

  public int scan() {
    token = "";
    int codePoint;

    while (true) {
      if (loc == input.length()) {
        return EOF;
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
        Preconditions.checkArgument(loc != input.length(), "unclosed string literal: %s", input);
        codePoint = input.codePointAt(loc);

        if (!escaped && codePoint == '"') {
          loc += Character.charCount(codePoint);
          token = sb.toString();
          return STRING;
        }
        if (!escaped && codePoint == '\\') {
          escaped = true;
          continue;
        }
        if (escaped) {
          if (codePoint == '"') {
            sb.append('"');
          } else {
            throw new IllegalArgumentException(
                String.format("unrecognized escape %c: %s", codePoint, input));
          }
          escaped = false;
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
        if (loc == input.length()) {
          token = input.substring(start);
          return NUMBER;
        }
        codePoint = input.codePointAt(loc);
        if (!digit(codePoint)) {
          token = input.substring(start, loc);
          return NUMBER;
        }
        loc += Character.charCount(codePoint);
      }
    }

    if (identLead(codePoint) || codePoint == '$') {
      // If the identifier starts with '$', it isn't valid yet, since it needs the body too.
      boolean valid = identLead(codePoint);
      int start = loc;
      loc += Character.charCount(codePoint);
      while (true) {
        if (loc == input.length()) {
          Preconditions.checkArgument(valid, "identifier needs a letter after '$': %s", input);
          token = input.substring(start);
          return IDENT;
        }
        codePoint = input.codePointAt(loc);
        if (valid && !identFollow(codePoint)) {
          token = input.substring(start, loc);
          return IDENT;
        }
        Preconditions.checkArgument(
            valid || identLead(codePoint), "identifier needs a letter after '$': %s", input);
        loc += Character.charCount(codePoint);
        valid = true;
      }
    }
    // Consume and return the next character
    loc += Character.charCount(codePoint);
    return codePoint;
  }

  public String token() {
    return token;
  }

  public String input() {
    return input;
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
