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

package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.common.collect.ImmutableList;
import java.util.Arrays;
import java.util.List;

/**
 * Contains the common logic for generating view models for GAPIC surface methods. This is used in
 * the generation of both the client libraries and of the standalone samples for each of the library
 * surface methods. Since in either case the resulting methods have one or more samples, this
 * populates the view models with appropriate sample view models.
 */
public class NodeJSMethodViewGenerator {
  final DynamicLangApiMethodTransformer clientMethodTransformer;

  public NodeJSMethodViewGenerator(DynamicLangApiMethodTransformer transformer) {
    clientMethodTransformer = transformer;
  }

  public List<OptionalArrayMethodView> generateApiMethods(
      GapicInterfaceContext context, boolean packageHasMultipleServices) {
    ImmutableList.Builder<OptionalArrayMethodView> apiMethodsAndSamples = ImmutableList.builder();

    for (MethodModel method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asDynamicMethodContext(method);
      OptionalArrayMethodView methodView;
      if (methodContext.getMethodConfig().isPageStreaming()) {
        methodView =
            clientMethodTransformer.generatePagedStreamingMethod(
                methodContext,
                packageHasMultipleServices,
                Arrays.asList(CallingForm.RequestAsyncPaged, CallingForm.RequestAsyncPagedAll));
      } else if (methodContext.getMethodConfig().isLongRunningOperation()) {
        methodView =
            clientMethodTransformer.generateLongRunningMethod(
                methodContext,
                packageHasMultipleServices,
                Arrays.asList(CallingForm.LongRunningPromise, CallingForm.LongRunningEventEmitter));
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
                methodContext, packageHasMultipleServices, callingForms);
      }

      apiMethodsAndSamples.add(methodView);
    }

    return apiMethodsAndSamples.build();
  }
}
