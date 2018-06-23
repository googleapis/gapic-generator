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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.config.DiscoveryRequestType;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;
import com.google.api.tools.framework.model.EnumValue;

/** SchemaTypeNameConverter maps Schema instances to TypeName instances. */
public abstract class SchemaTypeNameConverter implements TypeNameConverter {
  public abstract DiscoGapicNamer getDiscoGapicNamer();

  public abstract SurfaceNamer getNamer();

  public enum BoxingBehavior {
    // Box primitive types, e.g. Boolean instead of boolean.
    BOX_PRIMITIVES,

    // Don't box primitive types.
    NO_BOX_PRIMITIVES
  }

  /**
   * Provides a TypeName for the given Schema.
   *
   * @param field
   */
  public abstract TypeName getTypeName(DiscoveryField field);

  /** Provides a TypeName for the given Schema. */
  public abstract TypeName getTypeName(DiscoveryField type, BoxingBehavior boxingBehavior);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  public abstract TypedValue getSnippetZeroValue(DiscoveryField field);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  public abstract TypedValue getSnippetZeroValue(TypeModel type);

  /**
   * Provides a TypedValue containing the zero value of the given type, plus the TypeName of the
   * type; suitable for use within code snippets.
   */
  public abstract TypedValue getEnumValue(DiscoveryField field, String value);

  /** Provides a TypeName for the element type of the given schema. */
  public abstract TypeName getTypeNameForElementType(DiscoveryField type);

  /** Provides a TypeName for the element type of the given TypeModel. */
  public abstract TypeName getTypeNameForElementType(TypeModel type);

  /**
   * Provides a TypedValue containing the zero value of the given type, for use internally within
   * the vkit layer; plus the TypeName of the type. This will often return the same value as {@link
   * #getSnippetZeroValue(DiscoveryField)}.
   */
  public abstract TypedValue getImplZeroValue(DiscoveryField discoveryField);

  /** Renders the given value if it is a primitive type. */
  public abstract String renderPrimitiveValue(Schema schema, String value);

  /** Renders the given value if it is a primitive type. */
  public abstract String renderPrimitiveValue(TypeModel type, String value);

  /** Renders the value as a string. */
  public abstract String renderValueAsString(String value);

  @Override
  public TypeName getTypeName(InterfaceModel interfaceModel) {
    return new TypeName(interfaceModel.getFullName());
  }

  @Override
  public TypeName getTypeName(FieldModel type) {
    return getTypeName((DiscoveryField) type);
  }

  @Override
  public TypeName getTypeName(TypeModel type) {
    if (type instanceof DiscoveryRequestType) {
      Method method = ((DiscoveryRequestType) type).parentMethod().getDiscoMethod();
      return getDiscoGapicNamer().getRequestTypeName(method, getNamer());
    }
    if (type.isEmptyType()) {
      return getTypeNameForElementType(type);
    }
    return getTypeName((DiscoveryField) type);
  }

  @Override
  public TypedValue getEnumValue(FieldModel type, EnumValue value) {
    return TypedValue.create(getTypeName(type), "%s." + value.getSimpleName());
  }

  @Override
  public TypeName getTypeNameForElementType(FieldModel type) {
    return getTypeNameForElementType(type.getType());
  }

  @Override
  public TypedValue getSnippetZeroValue(FieldModel type) {
    return getSnippetZeroValue(type.getType());
  }

  @Override
  public TypedValue getImplZeroValue(FieldModel type) {
    return getImplZeroValue((DiscoveryField) type);
  }

  @Override
  public String renderPrimitiveValue(FieldModel type, String value) {
    return renderPrimitiveValue(((DiscoveryField) type).getDiscoveryField(), value);
  }
}
