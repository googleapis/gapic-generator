/* Copyright 2016 Google LLC
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
package com.google.api.codegen.util.ruby;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;
import com.google.common.collect.ImmutableSet;

/** The NameFormatter for Ruby. */
public class RubyNameFormatter implements NameFormatter {

  private String wrapIfKeywordOrBuiltIn(String name) {
    if (RESERVED_IDENTIFIER_SET.contains(name)) {
      return name + "_";
    } else {
      return name;
    }
  }

  @Override
  public String publicClassName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toUpperCamel());
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
    return wrapIfKeywordOrBuiltIn(name.toLowerUnderscore());
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
    return wrapIfKeywordOrBuiltIn(name.toUpperUnderscore());
  }

  @Override
  public String keyName(Name name) {
    return name.toLowerUnderscore();
  }

  @Override
  public String qualifiedName(NamePath namePath) {
    return namePath.withUpperPieces().toDoubleColoned();
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
   * A set of Ruby keywords and built-ins. See:
   * http://docs.ruby-lang.org/en/2.3.0/keywords_rdoc.html
   */
  public static final ImmutableSet<String> RESERVED_IDENTIFIER_SET =
      ImmutableSet.<String>builder()
          .add(
              "__ENCODING__",
              "__LINE__",
              "__FILE__",
              "BEGIN",
              "END",
              "alias",
              "and",
              "begin",
              "break",
              "case",
              "class",
              "def",
              "defined?",
              "do",
              "else",
              "elsif",
              "end",
              "ensure",
              "false",
              "for",
              "if",
              "in",
              "module",
              "next",
              "nil",
              "not",
              "or",
              "redo",
              "rescue",
              "retry",
              "return",
              "self",
              "super",
              "then",
              "true",
              "undef",
              "unless",
              "until",
              "when",
              "while",
              "yield",
              // "options" is here because it's a common keyword argument to
              // specify a CallOptions instance.
              "options")
          .build();
}
