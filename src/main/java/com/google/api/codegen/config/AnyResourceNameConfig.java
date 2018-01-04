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

/**
 * AnyResourceNameConfig is a singleton configuration indicating acceptance of any resource name
 * format.
 */
public class AnyResourceNameConfig implements ResourceNameConfig {

  public static final String ENTITY_NAME = "resource_name";

  public static final String GAPIC_CONFIG_ANY_VALUE = "*";

  private static AnyResourceNameConfig instance = new AnyResourceNameConfig();

  public static AnyResourceNameConfig instance() {
    return instance;
  }

  private AnyResourceNameConfig() {}

  @Override
  public ResourceNameType getResourceNameType() {
    return ResourceNameType.ANY;
  }

  @Override
  public String getEntityName() {
    return ENTITY_NAME;
  }

  @Override
  public ProtoFile getAssignedProtoFile() {
    return null;
  }
}
