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

package com.google.api.codegen.transformer.php;

import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PhpMethodViewGenerator {

  private final DynamicLangApiMethodTransformer clientMethodTransformer;

  public PhpMethodViewGenerator(DynamicLangApiMethodTransformer transformer) {
    this.clientMethodTransformer = transformer;
  }

  public List<OptionalArrayMethodView> generateApiMethods(
      GapicInterfaceContext context, boolean hasMultipleServices) {
    return context
        .getSupportedMethods()
        .stream()
        .map(
            methodModel ->
                generateOneApiMethod(
                    context.asDynamicMethodContext(methodModel), null, hasMultipleServices))
        .collect(Collectors.toList());
  }

  private OptionalArrayMethodView generateOneApiMethod(
      GapicMethodContext methodContext,
      InitCodeContext initContext,
      boolean packageHasMultipleServices) {
    OptionalArrayMethodView methodView = null;
    if (methodContext.getMethodConfig().isPageStreaming()) {
      methodView =
          clientMethodTransformer.generatePagedStreamingMethod(
              methodContext,
              initContext,
              packageHasMultipleServices,
              Arrays.asList(CallingForm.RequestPagedAll, CallingForm.RequestPaged));
    } else if (methodContext.getMethodConfig().isLongRunningOperation()) {
      methodView =
          clientMethodTransformer.generateLongRunningMethod(
              methodContext,
              initContext,
              packageHasMultipleServices,
              Arrays.asList(CallingForm.LongRunningRequest, CallingForm.LongRunningRequestAsync));
    } else {
      List<CallingForm> callingForms;
      GrpcStreamingType streamingType = methodContext.getMethodConfig().getGrpcStreamingType();
      switch (streamingType) {
        case BidiStreaming:
          callingForms =
              Arrays.asList(
                  CallingForm.RequestStreamingBidi, CallingForm.RequestStreamingBidiAsync);
          break;
        case ClientStreaming:
          callingForms =
              Arrays.asList(
                  CallingForm.RequestStreamingClient, CallingForm.RequestStreamingClientAsync);
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

    return methodView;
  }
}
