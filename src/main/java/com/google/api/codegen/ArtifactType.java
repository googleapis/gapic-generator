/* Copyright 2018 Google LLC
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
package com.google.api.codegen;

public enum ArtifactType {
  GAPIC_CONFIG,
  DISCOGAPIC_CONFIG,

  // This will be split into GAPIC and GAPIC_PACKAGE
  LEGACY_GAPIC_AND_PACKAGE,
  // This will be split into DISCOGAPIC and DISCOGAPIC_PACKAGE
  LEGACY_DISCOGAPIC_AND_PACKAGE,

  // The different artifact types will be split out (e.g. PROTOBUF_PACKAGE, GRPC_PACKAGE)
  LEGACY_GRPC_PACKAGE
}
