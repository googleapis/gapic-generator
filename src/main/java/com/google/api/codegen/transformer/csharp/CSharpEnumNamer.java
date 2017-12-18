/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.csharp;

/**
 * Directly ported from GetEnumValueName() in :
 * https://github.com/google/protobuf/blob/master/src/google/protobuf/compiler/csharp/csharp_helpers.cc
 * Must be kept in sync.
 */
public class CSharpEnumNamer {

  /** Format the enum value name as required for C#. */
  public String getEnumValueName(String enumName, String enumValueName) {
    String stripped = tryRemovePrefix(enumName, enumValueName);
    String result = shoutyToPascalCase(stripped);
    // Just in case we have an enum name of FOO and a value of FOO_2... make sure the returned
    // string is a valid identifier.
    if (Character.isDigit(result.charAt(0))) {
      result = "_" + result;
    }
    return result;
  }

  // Attempt to remove a prefix from a value, ignoring casing and skipping underscores.
  // (foo, foo_bar) => bar - underscore after prefix is skipped
  // (FOO, foo_bar) => bar - casing is ignored
  // (foo_bar, foobarbaz) => baz - underscore in prefix is ignored
  // (foobar, foo_barbaz) => baz - underscore in value is ignored
  // (foo, bar) => bar - prefix isn't matched; return original value
  private String tryRemovePrefix(String prefix, String value) {
    // First normalize to a lower-case no-underscores prefix to match against
    String prefixToMatch = "";
    for (int i = 0; i < prefix.length(); i++) {
      if (prefix.charAt(i) != '_') {
        prefixToMatch += Character.toLowerCase(prefix.charAt(i));
      }
    }

    // This keeps track of how much of value we've consumed
    int prefixIndex;
    int valueIndex;
    for (prefixIndex = 0, valueIndex = 0;
        prefixIndex < prefixToMatch.length() && valueIndex < value.length();
        valueIndex++) {
      // Skip over underscores in the value
      if (value.charAt(valueIndex) == '_') {
        continue;
      }
      if (Character.toLowerCase(value.charAt(valueIndex)) != prefixToMatch.charAt(prefixIndex++)) {
        // Failed to match the prefix - bail out early.
        return value;
      }
    }

    // If we didn't finish looking through the prefix, we can't strip it.
    if (prefixIndex < prefixToMatch.length()) {
      return value;
    }

    // Step over any underscores after the prefix
    while (valueIndex < value.length() && value.charAt(valueIndex) == '_') {
      valueIndex++;
    }

    // If there's nothing left (e.g. it was a prefix with only underscores afterwards), don't strip.
    if (valueIndex == value.length()) {
      return value;
    }

    return value.substring(valueIndex);
  }

  // Convert a string which is expected to be SHOUTY_CASE (but may not be *precisely* shouty)
  // into a PascalCase string. Precise rules implemented:
  // Previous input character      Current character         Case
  // Any                           Non-alphanumeric          Skipped
  // None - first char of input    Alphanumeric              Upper
  // Non-letter (e.g. _ or 1)      Alphanumeric              Upper
  // Numeric                       Alphanumeric              Upper
  // Lower letter                  Alphanumeric              Same as current
  // Upper letter                  Alphanumeric              Lower
  private String shoutyToPascalCase(String input) {
    String result = "";
    // Simple way of implementing "always start with upper"
    char previous = '_';
    for (int i = 0; i < input.length(); i++) {
      char current = input.charAt(i);
      if (!Character.isLetterOrDigit(current)) {
        previous = current;
        continue;
      }
      if (!Character.isLetterOrDigit(previous)) {
        result += Character.toUpperCase(current);
      } else if (Character.isDigit(previous)) {
        result += Character.toUpperCase(current);
      } else if (Character.isLowerCase(previous)) {
        result += current;
      } else {
        result += Character.toLowerCase(current);
      }
      previous = current;
    }
    return result;
  }
}
