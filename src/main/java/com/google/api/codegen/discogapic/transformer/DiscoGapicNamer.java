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
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;

/** Provides language-specific names for variables and classes of Discovery-Document models. */
public class DiscoGapicNamer {
  private final SurfaceNamer languageNamer;
  private static final String regexDelimiter = "\\.";

  /* Create a JavaSurfaceNamer for a Discovery-based API. */
  public DiscoGapicNamer(SurfaceNamer parentNamer) {
    this.languageNamer = parentNamer;
  }

  public DiscoGapicNamer cloneWithPackageName(String packageName) {
    return new DiscoGapicNamer(languageNamer.cloneWithPackageNameForDiscovery(packageName));
  }

  /* @return the underlying language surface namer. */
  public SurfaceNamer getLanguageNamer() {
    return languageNamer;
  }

  /** Returns the variable name for a field. */
  public String getFieldVarName(String fieldName) {
    return languageNamer.privateFieldName(Name.anyCamel(fieldName));
  }

  /** Returns the resource getter method name for a resource field. */
  public String getResourceGetterName(String fieldName) {
    Name name;
    if (fieldName.contains("_")) {
      name = Name.anyCamel(fieldName.split("_"));
    } else {
      name = Name.anyCamel(fieldName);
    }
    return languageNamer.publicMethodName(Name.anyCamel("get").join(name));
  }

  /** Returns the resource setter method name for a resource field. */
  public String getResourceSetterName(String fieldName) {
    Name name;
    if (fieldName.contains("_")) {
      name = Name.anyCamel(fieldName.split("_"));
    } else {
      name = Name.anyCamel(fieldName);
    }
    return languageNamer.publicMethodName(Name.anyCamel("set").join(name));
  }

  /**
   * Formats the method as a Name. Methods are generally in the format
   * "[api].[resource].[function]".
   */
  public static Name methodAsName(Method method) {
    String[] pieces = method.id().split(regexDelimiter);
    Name result = Name.anyCamel(pieces[1]);
    for (int i = 2; i < pieces.length; i++) {
      result = result.join(Name.anyCamel(pieces[i]));
    }
    return result;
  }

  public static String getSimpleInterfaceName(String interfaceName) {
    String[] pieces = interfaceName.split(regexDelimiter);
    return pieces[pieces.length - 1];
  }

  /** Get the request type name from a method. */
  public static Name getRequestName(Method method) {
    String[] pieces = method.id().split(regexDelimiter);
    return Name.anyCamel(pieces[pieces.length - 2], pieces[pieces.length - 1], "http", "request");
  }

  /** Get the response type name from a method. */
  public static Name getResponseName(Method method) {
    if (method.response() != null) {
      String typeName =
          method.response().reference() != null
              ? method.response().reference()
              : method.response().getIdentifier();
      return Name.anyCamel(typeName);
    } else {
      String[] pieces = method.id().split(regexDelimiter);
      return Name.anyCamel(
          pieces[pieces.length - 2], pieces[pieces.length - 1], "http", "response");
    }
  }

  //TODO(andrealin): Naming methods for service name.
}
