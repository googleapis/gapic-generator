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
package com.google.api.codegen.util.py;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;
import com.google.common.collect.ImmutableSet;

/** The NameFormatter for Python. */
public class PythonNameFormatter implements NameFormatter {

  private String wrapIfKeywordOrBuiltIn(String name) {
    if (RESERVED_IDENTIFIER_SET.contains(name)) {
      return name + "_";
    } else {
      return name;
    }
  }

  @Override
  public String publicClassName(Name name) {
    return name.toUpperCamel();
  }

  @Override
  public String privateClassName(Name name) {
    return publicClassName(name);
  }

  @Override
  public String localVarName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerUnderscore());
  }

  @Override
  public String privateFieldName(Name name) {
    return name.toLowerUnderscore();
  }

  @Override
  public String publicFieldName(Name name) {
    return name.toLowerUnderscore();
  }

  @Override
  public String localVarReference(Name name) {
    return name.toLowerUnderscore();
  }

  @Override
  public String publicMethodName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerUnderscore());
  }

  @Override
  public String privateMethodName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerUnderscore());
  }

  @Override
  public String staticFunctionName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerUnderscore());
  }

  @Override
  public String inittedConstantName(Name name) {
    return name.toUpperUnderscore();
  }

  @Override
  public String keyName(Name name) {
    return name.toLowerUnderscore();
  }

  @Override
  public String qualifiedName(NamePath namePath) {
    return namePath.toDotted();
  }

  @Override
  public String packageFilePathPiece(Name name) {
    return name.toLowerUnderscore();
  }

  @Override
  public String classFileNameBase(Name name) {
    return name.toLowerUnderscore();
  }

  /**
   * A set of Python reserved keywords. See
   * https://docs.python.org/2/reference/lexical_analysis.html#keywords
   * https://docs.python.org/2/library/functions.html
   * https://docs.python.org/2/library/functions.html#non-essential-built-in-funcs
   * https://docs.python.org/2/library/constants.html#constants-added-by-the-site-module
   */
  public static final ImmutableSet<String> RESERVED_IDENTIFIER_SET =
      ImmutableSet.<String>builder()
          .add(
              "and",
              "del",
              "from",
              "not",
              "while",
              "as",
              "elif",
              "global",
              "or",
              "with",
              "assert",
              "else",
              "if",
              "pass",
              "yield",
              "break",
              "except",
              "import",
              "print",
              "class",
              "exec",
              "in",
              "raise",
              "continue",
              "finally",
              "is",
              "return",
              "def",
              "for",
              "lambda",
              "try",
              // Built-ins
              "abs",
              "all",
              "any",
              "apply",
              "basestring",
              "bin",
              "bool",
              "buffer",
              "bytearray",
              "bytes",
              "callable",
              "chr",
              "classmethod",
              "cmp",
              "coerce",
              "compile",
              "complex",
              "copyright",
              "credits",
              "delattr",
              "dict",
              "dir",
              "divmod",
              "enumerate",
              "eval",
              "execfile",
              "exit",
              "file",
              "filter",
              "float",
              "format",
              "frozenset",
              "getattr",
              "globals",
              "hasattr",
              "hash",
              "help",
              "hex",
              "id",
              "input",
              "int",
              "intern",
              "isinstance",
              "issubclass",
              "iter",
              "len",
              "license",
              "list",
              "locals",
              "long",
              "map",
              "max",
              "memoryview",
              "min",
              "next",
              "object",
              "oct",
              "open",
              "ord",
              "pow",
              "print",
              "property",
              "quit",
              "range",
              "raw_input",
              "reduce",
              "reload",
              "repr",
              "reversed",
              "round",
              "set",
              "setattr",
              "slice",
              "sorted",
              "staticmethod",
              "str",
              "sum",
              "super",
              "tuple",
              "type",
              "unichr",
              "unicode",
              "vars",
              "xrange",
              "zip",
              // "options" is not a Python keyword, but it's used as a keyword
              // argument in the methods generated by Gapic.
              "options")
          .build();
}
