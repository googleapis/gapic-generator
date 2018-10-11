/* Copyright 2016 Google LLC
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
package com.google.api.codegen.config;

import com.google.api.tools.framework.model.ProtoFile;
import javax.annotation.Nullable;

public interface ResourceNameConfig {

  /** Returns the name used for uniquely identifying the entity in config. */
  String getEntityId();

  /** Returns the name used as a basis for generating methods. */
  String getEntityName();

  @Nullable
  String getCommonResourceName();

  /** Returns the resource name type. */
  ResourceNameType getResourceNameType();

  /**
   * Returns the proto file to which the resource name config has been assigned. This is required to
   * ensure that a consistent namespace can be calculated for the resource name.
   */
  ProtoFile getAssignedProtoFile();
}
