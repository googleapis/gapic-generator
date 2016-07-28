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
package com.google.api.codegen.csharp;

import com.google.common.base.Splitter;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

/**
 * A class that provides helper methods for snippet files generating C# code to get data and
 * perform data transformations that are difficult or messy to do in the snippets themselves.
 */
public class CSharpContextCommon {

  /**
   * The set of namespaces which need to be imported.
   */
  // TODO(jonskeet): Handle naming collisions.
  private final TreeSet<String> imports = new TreeSet<>();

  /**
   * Adds the given type name to the import list. Returns an empty string so that the output is not
   * affected.
   */
  public String addImport(String namespace) {
    imports.add(namespace);
    return "";
  }

  // Snippet Helpers
  // ===============

  /**
   * Splits given text into lines and returns an iterable of strings each one representing a line
   * decorated for an XML documentation comment, wrapped in the given element
   */
  public Iterable<String> getXmlDocLines(String text, String element) {
    // TODO(jonskeet): Convert markdown to XML documentation format.
    // https://github.com/googleapis/toolkit/issues/331
    List<String> result = new ArrayList<>();
    result.add("/// <" + element + ">");
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add("/// " + line.replace("&", "&amp;").replace("<", "&lt;"));
    }
    result.add("/// </" + element + ">");
    return result;
  }

  /**
   * Splits given text into lines and returns an iterable of strings each one representing a line
   * decorated for an XML documentation comment, wrapped in the given element
   */
  public Iterable<String> getXmlParameterLines(String text, String parameterName) {
    // TODO(jonskeet): Convert markdown to XML documentation format.
    // https://github.com/googleapis/toolkit/issues/331
    List<String> result = new ArrayList<>();
    result.add("/// <paramref name=\"" + parameterName + "\">");
    for (String line : Splitter.on(String.format("%n")).split(text)) {
      result.add("/// " + line.replace("&", "&amp;").replace("<", "&lt;"));
    }
    result.add("/// </paramref>");
    return result;
  }

  // This member function is necessary to provide access to snippets for
  // the functionality, since snippets can't call static functions.
  public String underscoresToCamelCase(
      String input, boolean capNextLetter, boolean preservePeriod) {
    return s_underscoresToCamelCase(input, capNextLetter, preservePeriod);
  }

  public static String s_underscoresToCamelCase(String input) {
    return s_underscoresToCamelCase(input, false, false);
  }

  public static String s_underscoresToPascalCase(String input) {
    return s_underscoresToCamelCase(input, true, false);
  }

  // Copied from csharp_helpers.cc and converted into Java.
  // The existing lowerUnderscoreToUpperCamel etc don't handle dots in the way we want.
  // TODO: investigate that and add more common methods if necessary.
  // This function is necessary to provide a static entry point for the same-named
  // member function.
  public static String s_underscoresToCamelCase(
      String input, boolean capNextLetter, boolean preservePeriod) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if ('a' <= c && c <= 'z') {
        result.append(capNextLetter ? Character.toUpperCase(c) : c);
        capNextLetter = false;
      } else if ('A' <= c && c <= 'Z') {
        if (i == 0 && !capNextLetter) {
          // Force first letter to lower-case unless explicitly told to
          // capitalize it.
          result.append(Character.toLowerCase(c));
        } else {
          // Capital letters after the first are left as-is.
          result.append(c);
        }
        capNextLetter = false;
      } else if ('0' <= c && c <= '9') {
        result.append(c);
        capNextLetter = true;
      } else {
        capNextLetter = true;
        if (c == '.' && preservePeriod) {
          result.append('.');
        }
      }
    }
    // Add a trailing "_" if the name should be altered.
    if (input.endsWith("#")) {
      result.append('_');
    }
    return result.toString();
  }

  public TreeSet<String> getImports() {
    return imports;
  }

  public static Iterable<String> s_prefix(Iterable<String> input, String prefix) {
    ArrayList<String> output = new ArrayList<>();
    for (String s : input) {
      output.add(prefix + s);
    }
    return output;
  }
}
