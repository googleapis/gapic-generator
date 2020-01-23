/* Copyright 2019 Google LLC
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

package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Contains the common logic for generating view models for GAPIC surface methods. This is used in
 * the generation of both the client libraries and of the standalone samples for each of the library
 * surface methods. Since in either case the resulting methods have one or more samples, this
 * populates the view models with appropriate sample view models.
 */
public class JavaApiMethodTransformer extends StaticLangApiMethodTransformer {

  public JavaApiMethodTransformer(SampleTransformer sampleTransformer) {
    super(sampleTransformer);
  }

  public JavaApiMethodTransformer() {
    super();
  }

  /**
   * Generates the StaticLangApiMethodView for each of the methods in the InterfaceContext.
   *
   * @param context The context containing the methods for which to generate StaticLangApiMethodView
   * @return The list of StaticLangApiMethodView, one per method
   */
  @Override
  public List<StaticLangApiMethodView> generateApiMethods(InterfaceContext context) {
    return Streams.stream(context.getSupportedMethods())
        .map(m -> generateApiMethods(context.asRequestMethodContext(m), null))
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }

  @Override
  public StaticLangApiMethodView generateApiMethod(
      MethodContext methodContext, SampleContext sampleContext) {
    switch (sampleContext.clientMethodType()) {
      case PagedFlattenedMethod:
        return generatePagedFlattenedMethod(methodContext, sampleContext);
      case PagedRequestObjectMethod:
        return generatePagedRequestObjectMethod(methodContext, sampleContext);
      case PagedCallableMethod:
        return generatePagedCallableMethod(methodContext, sampleContext);
      case UnpagedListCallableMethod:
        return generateUnpagedListCallableMethod(methodContext, sampleContext);
      case FlattenedMethod:
        return generateFlattenedMethod(methodContext, sampleContext);
      case RequestObjectMethod:
        return generateRequestObjectMethod(methodContext, sampleContext);
      case CallableMethod:
        return generateCallableMethod(methodContext, sampleContext);
      case AsyncOperationFlattenedMethod:
        return generateAsyncOperationFlattenedMethod(methodContext, sampleContext);
      case AsyncOperationRequestObjectMethod:
        return generateAsyncOperationRequestObjectMethod(methodContext, sampleContext);
      case OperationCallableMethod:
        return generateOperationCallableMethod(methodContext, sampleContext);
      default:
        throw new IllegalStateException(
            String.format("Unrecognized client method type: %s", sampleContext.clientMethodType()));
    }
  }

  private List<StaticLangApiMethodView> generateApiMethods(
      MethodContext methodContext, SampleContext sampleContext) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (methodConfig.isPageStreaming()) {
      return generatePagedStreamingMethods(methodContext, sampleContext);
    } else if (methodConfig.isGrpcStreaming()) {
      return generateGrpcStreamingMethods(methodContext, sampleContext);
    } else if (methodContext.isLongRunningMethodContext()) {
      return generateLongRunningMethods(methodContext, sampleContext);
    } else {
      return generateUnaryMethods(methodContext, sampleContext);
    }
  }

  private List<StaticLangApiMethodView> generateUnaryMethods(
      MethodContext methodContext, @Nullable SampleContext sampleContext) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    InterfaceContext interfaceContext = methodContext.getSurfaceInterfaceContext();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (methodConfig.isFlattening()) {
      for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
        MethodContext flattenedMethodContext =
            interfaceContext.asFlattenedMethodContext(methodContext, flatteningGroup);

        apiMethods.add(
            generateFlattenedMethod(
                flattenedMethodContext.withCallingForms(
                    Collections.singletonList(CallingForm.Flattened)),
                sampleContext));

        // if (FlatteningConfig.hasAnyResourceNameParameter(flatteningGroup)) {
        //   apiMethods.add(
        //       generateFlattenedMethod(
        //           flattenedMethodContext
        //               .withResourceNamesInSamplesOnly()
        //               .withCallingForms(Collections.singletonList(CallingForm.Flattened)),
        //           sampleContext));
        // }
      }
    }
    apiMethods.add(
        generateRequestObjectMethod(
            methodContext.withCallingForms(Collections.singletonList(CallingForm.Request)),
            sampleContext));

    apiMethods.add(
        generateCallableMethod(
            methodContext.withCallingForms(Collections.singletonList(CallingForm.Callable)),
            sampleContext));
    return apiMethods;
  }

  private List<StaticLangApiMethodView> generateGrpcStreamingMethods(
      MethodContext methodContext, @Nullable SampleContext sampleContext) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    InterfaceContext interfaceContext = methodContext.getSurfaceInterfaceContext();
    List<CallingForm> callingForms;
    ImportTypeTable typeTable = interfaceContext.getImportTypeTable();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    switch (methodConfig.getGrpcStreamingType()) {
      case BidiStreaming:
        typeTable.saveNicknameFor("com.google.api.gax.rpc.BidiStreamingCallable");
        callingForms = Collections.singletonList(CallingForm.CallableStreamingBidi);
        break;
      case ClientStreaming:
        typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientStreamingCallable");
        callingForms = Collections.singletonList(CallingForm.CallableStreamingClient);
        break;
      case ServerStreaming:
        typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallable");
        callingForms = Collections.singletonList(CallingForm.CallableStreamingServer);
        break;
      default:
        throw new IllegalArgumentException(
            "Invalid streaming type: " + methodConfig.getGrpcStreamingType());
    }
    apiMethods.add(
        generateCallableMethod(methodContext.withCallingForms(callingForms), sampleContext));

    return apiMethods;
  }

  private List<StaticLangApiMethodView> generateLongRunningMethods(
      MethodContext methodContext, @Nullable SampleContext sampleContext) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    InterfaceContext interfaceContext = methodContext.getSurfaceInterfaceContext();
    interfaceContext
        .getImportTypeTable()
        .saveNicknameFor("com.google.api.gax.rpc.OperationCallable");
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (methodConfig.isFlattening()) {
      for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
        MethodContext flattenedMethodContext =
            interfaceContext
                .asFlattenedMethodContext(methodContext, flatteningGroup)
                .withCallingForms(Collections.singletonList(CallingForm.LongRunningFlattenedAsync));
        if (FlatteningConfig.hasAnyRepeatedResourceNameParameter(flatteningGroup)) {
          flattenedMethodContext = flattenedMethodContext.withResourceNamesInSamplesOnly();
        }
        apiMethods.add(
            generateAsyncOperationFlattenedMethod(flattenedMethodContext, sampleContext));

        if (FlatteningConfig.hasAnyResourceNameParameter(flatteningGroup)) {
          apiMethods.add(
              generateAsyncOperationFlattenedMethod(
                  flattenedMethodContext.withResourceNamesInSamplesOnly(), sampleContext));
        }
      }
    }
    apiMethods.add(
        generateAsyncOperationRequestObjectMethod(
            methodContext.withCallingForms(
                Collections.singletonList(CallingForm.LongRunningRequestAsync)),
            sampleContext));
    apiMethods.add(
        generateOperationCallableMethod(
            methodContext.withCallingForms(
                Collections.singletonList(CallingForm.LongRunningCallable)),
            sampleContext));
    apiMethods.add(
        generateCallableMethod(
            methodContext.withCallingForms(Collections.singletonList(CallingForm.Callable)),
            sampleContext));
    return apiMethods;
  }

  private List<StaticLangApiMethodView> generatePagedStreamingMethods(
      MethodContext methodContext, @Nullable SampleContext sampleContext) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    InterfaceContext interfaceContext = methodContext.getSurfaceInterfaceContext();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (methodConfig.isFlattening()) {
      for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
        MethodContext flattenedMethodContext =
            interfaceContext
                .asFlattenedMethodContext(methodContext, flatteningGroup)
                .withCallingForms(ImmutableList.of(CallingForm.FlattenedPaged));
        if (!FlatteningConfig.hasAnyRepeatedResourceNameParameter(flatteningGroup)) {
          apiMethods.add(generatePagedFlattenedMethod(flattenedMethodContext, sampleContext));
        }
        if (FlatteningConfig.hasAnyResourceNameParameter(flatteningGroup)) {
          apiMethods.add(
              generatePagedFlattenedMethod(
                  flattenedMethodContext.withResourceNamesInSamplesOnly(), sampleContext));
        }
      }
    }
    apiMethods.add(
        generatePagedRequestObjectMethod(
            methodContext.withCallingForms(ImmutableList.of(CallingForm.RequestPaged)),
            sampleContext));
    apiMethods.add(
        generatePagedCallableMethod(
            methodContext.withCallingForms(ImmutableList.of(CallingForm.CallablePaged)),
            sampleContext));

    apiMethods.add(
        generateUnpagedListCallableMethod(
            methodContext.withCallingForms(Collections.singletonList(CallingForm.CallableList)),
            sampleContext));
    return apiMethods;
  }
}
