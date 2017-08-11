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

/**
 * A read-only interface for mapping TypeRef instances to a corresponding String representation for
 * a particular language.
 *
 * <p>Passing this type ensures that mutable functionality in derived classes won't be called.
 */
public interface TypeFormatter {
  /** Get the full name for the given short name, using the default package. */
  String getImplicitPackageFullNameFor(String shortName);

  /** Get the full name for the given type. */
  String getFullNameFor(FieldModel type);

  /** Get the full name for the given type. */
  String getFullNameFor(InterfaceModel type);

  /** Get the full name for the element type of the given type. */
  String getFullNameForElementType(FieldModel type);

  /** Returns the nickname for the given type (without adding the full name to the import set). */
  String getNicknameFor(FieldModel type);

  /** Renders the primitive value of the given type. */
  String renderPrimitiveValue(FieldModel type, String key);
}
