/* Copyright 2017 Google LLC
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
package com.google.api.codegen.discogapic.transformer;

import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Enums;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;

/** Provides language-specific names for variables and classes of Discovery-Document models. */
public class DiscoGapicNamer {

  /* Create a DiscoGapicNamer for a Discovery-based API. */
  public DiscoGapicNamer() {}

  /** Returns the resource getter method name for a resource field. */
  public String getResourceGetterName(String fieldName, SurfaceNamer languageNamer) {
    return languageNamer.publicMethodName(
        Name.anyCamel("get").join(DiscoGapicParser.stringToName(fieldName)));
  }

  /** Returns the resource setter method name for a resource field. */
  public String getResourceSetterName(
      String fieldName, Enums.Cardinality isRepeated, SurfaceNamer languageNamer) {
    switch (isRepeated) {
      case IS_REPEATED:
        return languageNamer.publicMethodName(
            Name.from("add", "all").join(DiscoGapicParser.stringToName(fieldName)));
      case NOT_REPEATED:
      default:
        return languageNamer.publicMethodName(
            Name.from("set").join(DiscoGapicParser.stringToName(fieldName)));
    }
  }

  /** Get the request type name from a method. */
  public TypeName getRequestTypeName(Method method, SurfaceNamer languageNamer) {
    TypeNameConverter typeNameConverter = languageNamer.getTypeNameConverter();
    return typeNameConverter.getTypeNameInImplicitPackage(
        languageNamer.publicClassName(DiscoGapicParser.getRequestName(method)));
  }
}
