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

/**
 * Instances of ViewNamer provide names for specific components of a view.
 *
 * Naming is composed of two steps:
 *
 * 1. Composing a Name instance with the name pieces
 * 2. Formatting the Name for the particular type of identifier needed.
 *
 * This class delegates step 2 to the provided name formatter, which generally
 * would be a language-specific namer.
 */
public class ViewNamer implements NameFormatter {

  private NameFormatter formatter;

  public ViewNamer(NameFormatter formatter) {
    this.formatter = formatter;
  }

  public String className(Name name) {
    return formatter.className(name);
  }

  public String varName(Name name) {
    return formatter.varName(name);
  }

  public String methodName(Name name) {
    return formatter.methodName(name);
  }

  public String staticFunctionName(Name name) {
    return formatter.staticFunctionName(name);
  }

  public String inittedConstantName(Name name) {
    return formatter.inittedConstantName(name);
  }

  public String keyName(Name name) {
    return formatter.keyName(name);
  }

  public String qualifiedName(NamePath namePath) {
    return formatter.qualifiedName(namePath);
  }
}
