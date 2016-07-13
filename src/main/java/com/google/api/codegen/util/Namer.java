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
package com.google.api.codegen.util;

public class Namer {

  private LanguageNamer languageNamer;

  public Namer(LanguageNamer languageNamer) {
    this.languageNamer = languageNamer;
  }

  public String className(Name name) {
    return languageNamer.getClassName(name);
  }

  public String varName(Name name) {
    return languageNamer.varName(name);
  }

  public String methodName(Name name) {
    return languageNamer.memberFunctionName(name);
  }

  public String staticFunctionName(Name name) {
    return languageNamer.staticFunctionName(name);
  }

  public String inittedConstantName(Name name) {
    return languageNamer.inittedConstantName(name);
  }

  public String keyName(Name name) {
    return languageNamer.keyName(name);
  }

  public String qualifiedName(QualifiedName qualifiedName) {
    return languageNamer.qualifiedName(qualifiedName);
  }
}
