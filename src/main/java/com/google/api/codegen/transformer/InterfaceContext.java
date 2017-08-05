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
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProductConfig;
import java.util.List;

/**
 * The context for transforming an API interface, in an input-agnostic way, into a view model to use
 * for client library generation.
 */
public interface InterfaceContext extends TransformationContext {

  InterfaceConfig getInterfaceConfig();

  FeatureConfig getFeatureConfig();

  Iterable<MethodModel> getSupportedMethods();

  Iterable<MethodModel> getPageStreamingMethods();

  Iterable<MethodModel> getBatchingMethods();

  MethodConfig getMethodConfig(MethodModel method);

  MethodContext asRequestMethodContext(MethodModel method);

  MethodContext asDynamicMethodContext(MethodModel method);

  String getInterfaceFileName();

  String getInterfaceSimpleName();

  InterfaceContext withNewTypeTable();

  MethodContext asFlattenedMethodContext(MethodModel method, FlatteningConfig flatteningConfig);

  List<MethodModel> getInterfaceMethods();

  Iterable<MethodModel> getLongRunningMethods();

  @Override
  ProductConfig getProductConfig();

  @Override
  SurfaceNamer getNamer();

  @Override
  ImportTypeTable getImportTypeTable();
}
