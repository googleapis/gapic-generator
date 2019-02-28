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

import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.SingleResourceNameConfig;
import javax.annotation.Nullable;

/** The context for transforming a method to a view model object. */
public interface MethodContext {
  InterfaceContext getSurfaceInterfaceContext();

  MethodModel getMethodModel();

  InterfaceModel getTargetInterface();

  InterfaceModel getInterfaceModel();

  InterfaceConfig getInterfaceConfig();

  GapicProductConfig getProductConfig();

  ImportTypeTable getTypeTable();

  SurfaceNamer getNamer();

  MethodConfig getMethodConfig();

  @Nullable
  FlatteningConfig getFlatteningConfig();

  @Nullable
  LongRunningConfig getLongRunningConfig();

  boolean isLongRunningMethodContext();

  FeatureConfig getFeatureConfig();

  boolean isFlattenedMethodContext();

  SingleResourceNameConfig getSingleResourceNameConfig(String entityName);

  MethodContext cloneWithEmptyTypeTable();

  // TODO(andrealin): Move this out when HTTP is implemented in gax.
  String getGrpcContainerTypeName();

  MethodContext withResourceNamesInSamplesOnly();
}
