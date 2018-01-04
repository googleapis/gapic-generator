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
package com.google.api.codegen;

/** Utility class for manipulating words */
public class Inflector {

  /** Gives the singular form of an English word (only works for regular English plurals). */
  public static String singularize(String in) {
    if (in.endsWith("lves") || in.endsWith("rves")) {
      return in.substring(0, in.length() - 3) + "f";

    } else if (in.endsWith("ies")) {
      return in.substring(0, in.length() - 3) + "y";

    } else if (in.endsWith("ses")) {
      return in.substring(0, in.length() - 3) + "s";

    } else if (in.charAt(in.length() - 1) == 's' && in.charAt(in.length() - 2) != 's') {
      return in.substring(0, in.length() - 1);
    }

    return in;
  }

  /** Gives the singular form of an English word (only works for regular English plurals). */
  public static String pluralize(String in) {
    if (in.endsWith("x") || in.endsWith("s") || in.endsWith("sh") || in.endsWith("ch")) {
      return in + "es";
    } else {
      return in + "s";
    }
  }
}
