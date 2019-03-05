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
package com.google.api.codegen.config;

import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TransformationContext;
import java.util.List;

/**
 * The context for transforming an API interface, in an input-agnostic way, into a view model to use
 * for client library generation.
 */
public interface InterfaceContext extends TransformationContext {

  ApiModel getApiModel();

  InterfaceConfig getInterfaceConfig();

  InterfaceModel getInterfaceModel();

  FeatureConfig getFeatureConfig();

  Iterable<? extends MethodModel> getSupportedMethods();

  Iterable<? extends MethodModel> getPublicMethods();

  Iterable<MethodModel> getPageStreamingMethods();

  Iterable<MethodModel> getBatchingMethods();

  MethodConfig getMethodConfig(MethodModel method);

  MethodContext asRequestMethodContext(MethodModel method);

  String getInterfaceDescription();

  @Override
  InterfaceContext withNewTypeTable();

  @Override
  InterfaceContext withNewTypeTable(String newPackageName);

  MethodContext asFlattenedMethodContext(
      MethodContext methodContext, FlatteningConfig flatteningConfig);

  /* @return the methods in the interface. */
  List<? extends MethodModel> getInterfaceMethods();

  /* @return the methods in the interface that have method configs. */
  List<? extends MethodModel> getInterfaceConfigMethods();

  Iterable<MethodModel> getLongRunningMethods();

  String serviceTitle();

  @Override
  GapicProductConfig getProductConfig();

  @Override
  SurfaceNamer getNamer();

  @Override
  ImportTypeTable getImportTypeTable();

  String getServiceAddress();
}
