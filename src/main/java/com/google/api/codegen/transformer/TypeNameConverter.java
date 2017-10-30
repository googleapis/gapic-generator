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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.api.tools.framework.model.EnumValue;

/** ModelTypeNameConverter maps FieldModel and TypeModel instances to TypeName instances. */
public interface TypeNameConverter {
  /** Provides a TypeName for the given FieldModel. */
  TypeName getTypeName(FieldModel type);

  TypeName getTypeName(InterfaceModel interfaceModel);

  /** Provides a TypedValue for the given enum FieldModel. */
  TypedValue getEnumValue(FieldModel type, EnumValue value);

  /** Provides a TypeName for the element type of the given FieldModel. */
  TypeName getTypeNameForElementType(FieldModel type);

  /** Provides a TypeName for the element type of the given FieldModel. */
  TypeName getTypeName(TypeModel type);

  /** Provides a TypeName for the given FieldConfig and resource short name. */
  TypeName getTypeNameForTypedResourceName(FieldConfig fieldConfig, String typedResourceShortName);

  /**
   * Provides a TypeName for the given FieldConfig and resource short name, using the inner type if
   * the underlying field is repeated.
   */
  TypeName getTypeNameForResourceNameElementType(
      FieldConfig fieldConfig, String typedResourceShortName);

  /** Provides a TypeName for the given short name, using the default package. */
  TypeName getTypeNameInImplicitPackage(String shortName);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  TypedValue getSnippetZeroValue(FieldModel type);

  /**
   * Provides a TypedValue containing the zero value of the given type, for use internally within
   * the auto-generated layer; plus the TypeName of the type. This will often return the same value
   * as {@link #getSnippetZeroValue(FieldModel)}.
   */
  TypedValue getImplZeroValue(FieldModel type);

  /** Renders the given value if it is a primitive type. */
  String renderPrimitiveValue(FieldModel type, String value);
}
