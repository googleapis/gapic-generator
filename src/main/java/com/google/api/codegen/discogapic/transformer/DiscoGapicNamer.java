/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discogapic.transformer;

import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NameFormatter;
import com.google.api.codegen.util.NameFormatterDelegator;

/** Provides language-specific names for variables and classes. */
public class DiscoGapicNamer extends NameFormatterDelegator {

  public DiscoGapicNamer(NameFormatter nameFormatter) {
    super(nameFormatter);
  }

  /** Returns the variable name for a field. */
  public String getFieldVarName(String fieldName) {
    return privateFieldName(Name.anyCamel(fieldName));
  }

  /** Returns the resource getter method name for a resource field. */
  public String getResourceGetterName(String fieldName) {
    Name name;
    if (fieldName.contains("_")) {
      name = Name.anyCamel(fieldName.split("_"));
    } else {
      name = Name.anyCamel(fieldName);
    }
    return publicMethodName(Name.anyCamel("get").join(name));
  }
  /** Returns the resource getter method name for a resource field. */
  public String getResourceSetterName(String fieldName) {
    Name name;
    if (fieldName.contains("_")) {
      name = Name.anyCamel(fieldName.split("_"));
    } else {
      name = Name.anyCamel(fieldName);
    }
    return publicMethodName(Name.anyCamel("set").join(name));
  }

  /**
   * Returns the last substring after the input is split by periods. Ex: Input of
   * "compute.addresses.aggregatedList" returns the array: ["compute", "addresses", "List"].
   */
  public String[] getMethodNamePieces(String longMethodName) {
    String[] pieces = longMethodName.split("\\.");
    if (pieces.length < 3) {
      throw new IllegalArgumentException(
          "Fully qualified method name must be in the form [api].[resource].[method]");
    }
    return pieces;
  }

  /**
   * Returns the last substring after the input is split by periods. Ex: Input
   * "compute.addresses.aggregatedList" returns "aggregatedList".
   */
  public String getRequestName(String fullMethodName) {
    String[] pieces = getMethodNamePieces(fullMethodName);
    return privateFieldName(
        Name.anyCamel(pieces[pieces.length - 2], pieces[pieces.length - 1], "http", "request"));
  }

  /**
   * Returns the last substring after the input is split by periods. Ex: Input
   * "compute.addresses.aggregatedList" returns "aggregatedList".
   */
  public String getRequestName(Method method) {
    return getRequestName(method.id());
  }

  //TODO(andrealin): Naming methods for requests, responses, service name.
}
