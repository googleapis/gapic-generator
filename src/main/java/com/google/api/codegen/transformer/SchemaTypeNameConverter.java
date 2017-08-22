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

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.api.tools.framework.model.EnumValue;

/** SchemaTypeNameConverter maps Schema instances to TypeName instances. */
public abstract class SchemaTypeNameConverter implements TypeNameConverter {

  public enum BoxingBehavior {
    // Box primitive types, e.g. Boolean instead of boolean.
    BOX_PRIMITIVES,

    // Don't box primitive types.
    NO_BOX_PRIMITIVES
  }

  /** Provides a TypeName for the given Schema. */
  public abstract TypeName getTypeName(Schema type);

  /** Provides a TypeName for the given Schema. */
  public abstract TypeName getTypeName(Schema type, BoxingBehavior boxingBehavior);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  public abstract TypedValue getSnippetZeroValue(Schema schema);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  public abstract TypedValue getEnumValue(Schema schema, String value);

  /** Provides a TypeName for the element type of the given FieldType. */
  public abstract TypeName getTypeNameForElementType(Schema type);

  /**
   * Provides a TypedValue containing the zero value of the given type, for use internally within
   * the vkit layer; plus the TypeName of the type. This will often return the same value as {@link
   * #getSnippetZeroValue(Schema)}.
   */
  public abstract TypedValue getImplZeroValue(Schema schema);

  /** Renders the given value if it is a primitive type. */
  public abstract String renderPrimitiveValue(Schema schema, String value);

  @Override
  public TypeName getTypeName(InterfaceModel interfaceModel) {
    return new TypeName(interfaceModel.getFullName());
  }

  @Override
  public TypeName getTypeName(FieldModel type) {
    return getTypeName(type.getDiscoveryField());
  }

  @Override
  public TypedValue getEnumValue(FieldModel type, EnumValue value) {
    return TypedValue.create(getTypeName(type), "%s." + value.getSimpleName());
  }

  @Override
  public TypeName getTypeNameForElementType(FieldModel type) {
    return getTypeNameForElementType(type.getDiscoveryField());
  }

  @Override
  public TypedValue getSnippetZeroValue(FieldModel type) {
    return getSnippetZeroValue((type.getDiscoveryField()));
  }

  @Override
  public TypedValue getImplZeroValue(FieldModel type) {
    return getImplZeroValue((type.getDiscoveryField()));
  }

  @Override
  public String renderPrimitiveValue(FieldModel type, String value) {
    return renderPrimitiveValue(type.getDiscoveryField(), value);
  }
}
