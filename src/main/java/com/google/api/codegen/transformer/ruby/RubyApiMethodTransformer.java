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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicMethodContext;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.ServiceMessages;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.viewmodel.ApiMethodDocView;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// LangMethodViewGenerator is a bit redundant with quite a few repeated code.
// Trying to adopt a different way in Ruby. If it works, consider refactor existing languages
// to use the same pattern.
public class RubyApiMethodTransformer extends DynamicLangApiMethodTransformer {

	public DynamicLangApiMethodTransformer(ApiMethodParamTransformer apiMethodParamTransformer) {
    super(apiMethodParamTransformer, SampleTransformer.create(SampleType.IN_CODE));
  }

  public DynamicLangApiMethodTransformer(
      ApiMethodParamTransformer apiMethodParamTransformer, SampleTransformer sampleTransformer) {
    super(apiMethodParamTransformer, sampleTransformer);
  }

  @Override
  public generateApiMethods(InterfaceContext context) {
  	final boolean packageHasMultipleServices = false;
    final InitCodeContext initContext = null;

    if (methodContext.getMethodConfig().isPageStreaming()) {
      methodView =
          clientMethodTransformer.generatePagedStreamingMethod(
              methodContext,
              null,
              packageHasMultipleServices,
              Arrays.asList(CallingForm.RequestPagedAll, CallingForm.RequestPaged));
    } else if (methodContext.isLongRunningMethodContext()) {
      methodView =
          clientMethodTransformer.generateLongRunningMethod(
              methodContext,
              initContext,
              packageHasMultipleServices,
              Arrays.asList(CallingForm.LongRunningPromise));
    } else {
      List<CallingForm> callingForms;
      GrpcStreamingType streamingType = methodContext.getMethodConfig().getGrpcStreamingType();
      switch (streamingType) {
        case BidiStreaming:
          callingForms = Arrays.asList(CallingForm.RequestStreamingBidi);
          break;
        case ClientStreaming:
          callingForms = Arrays.asList(CallingForm.RequestStreamingClient);
          break;
        case ServerStreaming:
          callingForms = Arrays.asList(CallingForm.RequestStreamingServer);
          break;
        case NonStreaming:
          callingForms = Arrays.asList(CallingForm.Request);
          break;
        default:
          throw new IllegalArgumentException(
              "unhandled grpcStreamingType: " + streamingType.toString());
      }
      methodView =
          clientMethodTransformer.generateRequestMethod(
              methodContext, initContext, packageHasMultipleServices, callingForms);
  }

}