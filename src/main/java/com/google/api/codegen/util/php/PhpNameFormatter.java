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
package com.google.api.codegen.util.php;

import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NamePath;
import com.google.common.collect.ImmutableSet;

/** The NameFormatter for PHP. */
public class PhpNameFormatter implements NameFormatter {

  private String wrapIfKeywordOrBuiltIn(String name) {
    // PHP keywords are case-insensitive.
    if (KEYWORD_BUILT_IN_SET.contains(name.toLowerCase())) {
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
    return name.toLowerCamel();
  }

  @Override
  public String privateFieldName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String publicFieldName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String localVarReference(Name name) {
    return "$" + localVarName(name);
  }

  @Override
  public String publicMethodName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String privateMethodName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String staticFunctionName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String inittedConstantName(Name name) {
    return wrapIfKeywordOrBuiltIn(name.toLowerCamel());
  }

  @Override
  public String keyName(Name name) {
    return name.toLowerCamel();
  }

  @Override
  public String qualifiedName(NamePath namePath) {
    return namePath.toBackslashed();
  }

  @Override
  public String packageFilePathPiece(Name name) {
    return name.toOriginal();
  }

  @Override
  public String classFileNameBase(Name name) {
    return name.toOriginal();
  }

  /**
   * A set of PHP keywords and built-ins. keywords: http://php.net/manual/en/reserved.keywords.php
   */
  private static final ImmutableSet<String> KEYWORD_BUILT_IN_SET =
      ImmutableSet.<String>builder()
          .add(
              "__halt_compiler",
              "abstract",
              "and",
              "array",
              "as",
              "break",
              "callable",
              "case",
              "catch",
              "class",
              "clone",
              "const",
              "continue",
              "declare",
              "default",
              "die",
              "do",
              "echo",
              "else",
              "elseif",
              "empty",
              "enddeclare",
              "endfor",
              "endforeach",
              "endif",
              "endswitch",
              "endwhile",
              "eval",
              "exit",
              "extends",
              "final",
              "finally",
              "for",
              "foreach",
              "function",
              "global",
              "goto",
              "if",
              "implements",
              "include",
              "include_once",
              "instanceof",
              "insteadof",
              "interface",
              "isset",
              "list",
              "namespace",
              "new",
              "or",
              "print",
              "private",
              "protected",
              "public",
              "require",
              "require_once",
              "return",
              "static",
              "switch",
              "throw",
              "trait",
              "try",
              "unset",
              "use",
              "var",
              "while",
              "xor",
              "yield",
              "__CLASS__",
              "__DIR__",
              "__FILE__",
              "__FUNCTION__",
              "__LINE__",
              "__METHOD__",
              "__NAMESPACE__",
              "__TRAIT__")
          .build();
}
