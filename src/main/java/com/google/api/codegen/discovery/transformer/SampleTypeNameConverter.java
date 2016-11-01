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
package com.google.api.codegen.discovery.transformer;

import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypedValue;

/** Maps Type instances to TypeName instances. */
// TODO(saicheems): Rename this, it's responsible for more than type names.
public interface SampleTypeNameConverter {

  /** Provides a TypeName for the service. */
  TypeName getServiceTypeName(String apiTypeName);

  /** Provides a TypeName from the given API type name and TypeInfo. */
  TypeName getRequestTypeName(String apiTypeName, TypeInfo typeInfo);

  /** Provides a TypeName for the given TypeInfo. */
  TypeName getTypeName(TypeInfo typeInfo);

  /** Provides the element TypeName for the given TypeInfo. */
  TypeName getTypeNameForElementType(TypeInfo typeInfo);

  /** Provides a TypedValue containing the zero value of the given TypeInfo. */
  TypedValue getZeroValue(TypeInfo typeInfo);
}
