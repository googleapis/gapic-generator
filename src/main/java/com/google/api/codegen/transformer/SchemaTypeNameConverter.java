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
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;

/** SchemaTypeNameConverter maps Schema instances to TypeName instances. */
public interface SchemaTypeNameConverter {

  enum BoxPrimitives {
    // Box primitive types, e.g. Boolean instead of boolean.
    BOX_PRIMITIVES,

    // Don't box primitive types.
    NO_BOX_PRIMTIVES
  }

  /** Provides a TypeName for the given Schema. */
  TypeName getTypeName(Schema type);

  /** Provides a TypeName for the given Schema. */
  TypeName getTypeName(Schema type, BoxPrimitives boxPrimitives);

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
  TypedValue getSnippetZeroValue(Schema schema);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  TypedValue getEnumValue(Schema schema, String value);

  /**
   * Provides a TypedValue containing the zero value of the given type, for use internally within
   * the vkit layer; plus the TypeName of the type. This will often return the same value as {@link
   * #getSnippetZeroValue(Schema)}.
   */
  TypedValue getImplZeroValue(Schema schema);

  /** Renders the given value if it is a primitive type. */
  String renderPrimitiveValue(Schema schema, String value);
}
