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

import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.tools.framework.model.Interface;
import javax.annotation.Nullable;

/** The context for transforming a method to a view model object. */
public interface MethodContext {

  InterfaceContext getSurfaceTransformerContext();

  @Nullable
  Interface getInterface();

  GapicProductConfig getProductConfig();

  ImportTypeTable getTypeTable();

  SurfaceNamer getNamer();

  MethodConfig getMethodConfig();

  @Nullable
  FlatteningConfig getFlatteningConfig();

  FeatureConfig getFeatureConfig();

  boolean isFlattenedMethodContext();

  InterfaceConfig getInterfaceConfig();

  SingleResourceNameConfig getSingleResourceNameConfig(String entityName);

  MethodContext cloneWithEmptyTypeTable();

  /* Get the request type name and save it in the type table. */
  String getAndSaveRequestTypeName();

  /* Get the response type name and save it in the type table. */
  String getAndSaveResponseTypeName();

  // TODO(andrealin): Remove this method and implement in a MethodModel interface.
  String getBatchingDescriptorConstName();
}
