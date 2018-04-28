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
import com.google.api.codegen.discovery.Schema;

/**
 * A read-only interface for mapping Schema instances to a corresponding String representation for a
 * particular language.
 *
 * <p>Passing this type ensures that mutable functionality in derived classes won't be called.
 */
public interface SchemaTypeFormatter extends TypeFormatter {
  /** Returns the full name for the given type (without adding the full name to the import set).
   * @param type*/
  String getFullNameFor(DiscoveryField type);

  /**
   * Returns the inner type name for the given type (without adding the full name to the import
   * set). If there is no enclosing type, e.g. List or Map, then the inner type is the same as the
   * nickname.
   * @param type
   */
  String getInnerTypeNameFor(DiscoveryField type);

  /** Renders the primitive value of the given type. */
  String renderPrimitiveValue(Schema type, String value);

  String getNicknameFor(DiscoveryField type);
}
