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

/**
 * Contains the common logic for generating view models for GAPIC surface methods. This is used in
 * the generation of both the client libraries and of the standalone samples for each of the library
 * surface methods. Since in either case the resulting methods have one or more samples, this
 * populates the view models with appropriate sample view models.
 */
public class PhpMethodViewGenerator {

  private final DynamicLangApiMethodTransformer clientMethodTransformer;

  public PhpMethodViewGenerator(DynamicLangApiMethodTransformer transformer) {
    this.clientMethodTransformer = transformer;
  }

  public List<OptionalArrayMethodView> generateApiMethods(GapicInterfaceContext context) {
    return context
        .getSupportedMethods()
        .stream()
        .map(methodModel -> generateOneApiMethod(context.asRequestMethodContext(methodModel), null))
        .collect(Collectors.toList());
  }

  private OptionalArrayMethodView generateOneApiMethod(
      GapicMethodContext methodContext, InitCodeContext initContext) {
    OptionalArrayMethodView methodView = null;
    if (methodContext.getMethodConfig().isPageStreaming()) {
      methodView =
          clientMethodTransformer.generatePagedStreamingMethod(
              methodContext,
              initContext,
              false,
              Arrays.asList(CallingForm.RequestPaged, CallingForm.RequestPagedAll));
    } else if (methodContext.isLongRunningMethodContext()) {
      methodView =
          clientMethodTransformer.generateLongRunningMethod(
              methodContext,
              initContext,
              false,
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
              methodContext, initContext, false, callingForms);
    }

    return methodView;
  }
}
