/* Copyright 2016 Google LLC
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
package com.google.api.codegen.config;

public enum ResourceNameType {
  /** No resource type is available. */
  NONE,

  /** A single concrete resource type. */
  SINGLE,

  /**
   * A single fixed type. This is used to indicate a special string that can be accepted by a
   * resource name field that is not formatted as a normal resource name. For example, a field might
   * accept resource names with the format "foos/{foo}", but also accept an unformatted string such
   * as "_deleted-foo_". Resource names of this type will typically be part of a ONEOF
   * configuration, alongside other resource names of the SINGLE type.
   */
  FIXED,

  /** A set of resource types, accessed using a "oneof" pattern. */
  ONEOF,

  /** Any resource type. */
  ANY
}
