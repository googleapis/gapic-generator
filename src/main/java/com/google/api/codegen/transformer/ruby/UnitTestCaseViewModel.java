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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.base.Preconditions;

/** ViewModel of each unit test case for Ruby. */
// TODO(jcanizales): Only RubyGapicContext here is Ruby-specific, and unnecessarily so. Refactor so
// this can be shared with other languages.
public class UnitTestCaseViewModel {

  /**
   * Names used for the response variables in the generated Ruby samples. They change depending on
   * the method type.
   */
  public enum RubySampleResponseName {
    Empty,
    Response,
    Element,
    ResultsAndMetadata
  }

  private final InterfaceConfig interfaceConfig;
  private final Method method;
  private final ModelTypeTable typeTable;
  private final ApiConfig apiConfig;

  public UnitTestCaseViewModel(
      InterfaceConfig interfaceConfig,
      Method method,
      ModelTypeTable typeTable,
      ApiConfig apiConfig) {
    this.interfaceConfig = Preconditions.checkNotNull(interfaceConfig);
    this.method = Preconditions.checkNotNull(method);
    this.typeTable = Preconditions.checkNotNull(typeTable);
    this.apiConfig = Preconditions.checkNotNull(apiConfig);
  }

  // FIXME: Don't duplicate any of this code.
  private OptionalArrayMethodView getMethodView(Interface service, Method method) {
    SurfaceTransformerContext context = getSurfaceTransformerContextFromService(service);
    MethodTransformerContext methodContext = context.asDynamicMethodContext(method);
    DynamicLangApiMethodTransformer methodTransformer =
        new DynamicLangApiMethodTransformer(new RubyApiMethodParamTransformer());
    return methodTransformer.generateMethod(methodContext);
  }

  private SurfaceTransformerContext getSurfaceTransformerContextFromService(Interface service) {
    ModelTypeTable modelTypeTable =
        new ModelTypeTable(
            new RubyTypeTable(apiConfig.getPackageName()),
            new RubyModelTypeNameConverter(apiConfig.getPackageName()));
    return SurfaceTransformerContext.create(
        service,
        apiConfig,
        modelTypeTable,
        new RubySurfaceNamer(apiConfig.getPackageName()),
        new RubyFeatureConfig());
  }

  public OptionalArrayMethodView methodView() {
    return this.getMethodView(interfaceConfig.getInterface(), method);
  }

  public String requestTypeName() {
    return typeTable.getAndSaveNicknameFor(method.getInputType());
  }

  public String responseTypeName() {
    return typeTable.getAndSaveNicknameFor(method.getOutputType());
  }

  public RubySampleResponseName sampleResponseName() {
    OptionalArrayMethodView methodView = methodView();
    if (!methodView.hasReturnValue()) {
      return RubySampleResponseName.Empty;
    }
    if (methodView.isLongRunningOperation()) {
      return RubySampleResponseName.ResultsAndMetadata;
    }
    if (methodView.type() == ClientMethodType.PagedOptionalArrayMethod) {
      return RubySampleResponseName.Element;
    }

    Preconditions.checkArgument(
        methodView.type() == ClientMethodType.OptionalArrayMethod,
        "This code needs to be updated with the new method type used: %s",
        methodView.type());

    switch (methodView.grpcStreamingType()) {
      case NonStreaming:
        // fall through
      case ClientStreaming:
        return RubySampleResponseName.Response;
      case ServerStreaming:
        // fall through
      case BidiStreaming:
        return RubySampleResponseName.Element;
    }

    // Unreachable; to placate the compiler.
    return RubySampleResponseName.Response;
  }
}
