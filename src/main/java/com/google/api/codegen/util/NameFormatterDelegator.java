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
 * NameFormatterDelegator is an abstract class that implements the NameFormatter
 * interface and simply delegates calls to another NameFormatter. This allows a
 * child class to provide the interface of NameFormatter along with additional
 * functionality.
 *
 * Note to future maintainers: This class should only contain methods which
 * forward on to NameFormatter and nothing else; otherwise, it is no longer
 * functioning in spirit as a mix-in.
 */
public abstract class NameFormatterDelegator implements NameFormatter {

  private NameFormatter formatter;

  public NameFormatterDelegator(NameFormatter formatter) {
    this.formatter = formatter;
  }

  @Override
  public String className(Name name) {
    return formatter.className(name);
  }

  @Override
  public String varName(Name name) {
    return formatter.varName(name);
  }

  @Override
  public String varReference(Name name) {
    return formatter.varReference(name);
  }

  @Override
  public String methodName(Name name) {
    return formatter.methodName(name);
  }

  @Override
  public String staticFunctionName(Name name) {
    return formatter.staticFunctionName(name);
  }

  @Override
  public String inittedConstantName(Name name) {
    return formatter.inittedConstantName(name);
  }

  @Override
  public String keyName(Name name) {
    return formatter.keyName(name);
  }

  @Override
  public String qualifiedName(NamePath namePath) {
    return formatter.qualifiedName(namePath);
  }
}
