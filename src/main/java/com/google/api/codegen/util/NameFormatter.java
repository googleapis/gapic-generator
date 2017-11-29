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
package com.google.api.codegen.util;

/**
 * A NameFormatter formats Name objects in using the casing scheme for a particular type of
 * identifier in a particular programming language.
 */
public interface NameFormatter {

  /** Formats the name as a public class name. */
  String publicClassName(Name name);

  /** Formats the name as a private class name. */
  String privateClassName(Name name);

  /** Formats the name as a public field name. */
  String publicFieldName(Name name);

  /** Formats the name as a private field name. */
  String privateFieldName(Name name);

  /** Formats the name as a local variable name. */
  String localVarName(Name name);

  /** Formats the name as a reference to a local variable name. */
  String localVarReference(Name name);

  /** Formats the name as a public method name. */
  String publicMethodName(Name name);

  /** Formats the name as a private method name. */
  String privateMethodName(Name name);

  /** Formats the name as a static function name. */
  String staticFunctionName(Name name);

  /**
   * Formats the name as a constant which requires initialization.
   *
   * <p>It may seem odd that the initialization aspect needs to be distinguished, but this is
   * relevant in PHP, where constants with non-trivial initialization have to be initialized on
   * first use instead of at declaration time.
   */
  String inittedConstantName(Name name);

  /** Formats the name as a key name, to be used as a key in a map or hash table. */
  String keyName(Name name);

  /** Formats the name path as a qualified name. */
  String qualifiedName(NamePath namePath);

  /** Formats the name of a package file path. */
  String packageFilePathPiece(Name name);

  /** Formats the base file name. */
  String classFileNameBase(Name name);
}
