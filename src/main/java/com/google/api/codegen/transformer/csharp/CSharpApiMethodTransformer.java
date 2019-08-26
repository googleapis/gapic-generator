/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.GapicMethodContext;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CSharpApiMethodTransformer extends StaticLangApiMethodTransformer {

  private static final CSharpCommonTransformer csharpCommonTransformer =
      new CSharpCommonTransformer();
  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.Global;

  public CSharpApiMethodTransformer(SampleTransformer sampleTransformer) {
    super(sampleTransformer);
  }

  public CSharpApiMethodTransformer() {
    super();
  }

  @Override
  protected void setServiceResponseTypeName(
      MethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    String responseTypeName =
        context
            .getMethodModel()
            .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer());
    methodViewBuilder.serviceResponseTypeName(responseTypeName);
  }

  @Override
  public List<SimpleParamDocView> getRequestObjectParamDocs(MethodContext context) {
    String requestTypeName =
        context
            .getMethodModel()
            .getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer());
    switch (context.getMethodConfig().getGrpcStreamingType()) {
      case NonStreaming:
        SimpleParamDocView nonStreamingDoc =
            SimpleParamDocView.newBuilder()
                .paramName("request")
                .typeName(requestTypeName)
                .lines(
                    ImmutableList.of(
                        "The request object containing all of the parameters for the API call."))
                .build();
        return ImmutableList.of(nonStreamingDoc);
      case ServerStreaming:
        SimpleParamDocView serverStreamingDoc =
            SimpleParamDocView.newBuilder()
                .paramName("request")
                .typeName(requestTypeName)
                .lines(
                    ImmutableList.of(
                        "The request object containing all of the parameters for the API call."))
                .build();
        SimpleParamDocView serverStreamingCallSettingsDoc =
            SimpleParamDocView.newBuilder()
                .paramName("callSettings")
                .typeName("CallSettings")
                .lines(ImmutableList.of("If not null, applies overrides to this RPC call."))
                .build();
        return ImmutableList.of(serverStreamingDoc, serverStreamingCallSettingsDoc);
      case BidiStreaming:
        SimpleParamDocView bidiStreamingCallSettingsDoc =
            SimpleParamDocView.newBuilder()
                .paramName("callSettings")
                .typeName("CallSettings")
                .lines(ImmutableList.of("If not null, applies overrides to this RPC call."))
                .build();
        SimpleParamDocView bidiStreamingSettingsDoc =
            SimpleParamDocView.newBuilder()
                .paramName("streamingSettings")
                .typeName("BidirectionalStreamingSettings")
                .lines(
                    ImmutableList.of("If not null, applies streaming overrides to this RPC call."))
                .build();
        return ImmutableList.of(bidiStreamingCallSettingsDoc, bidiStreamingSettingsDoc);
      default:
        throw new UnsupportedOperationException(
            "Cannot handle streaming type: " + context.getMethodConfig().getGrpcStreamingType());
    }
  }

  @Override
  public List<StaticLangApiMethodView> generateApiMethods(InterfaceContext interfaceContext) {
    Preconditions.checkArgument(
        interfaceContext instanceof GapicInterfaceContext,
        "Only applicable for protobuf-based API in CSharp.");
    GapicInterfaceContext context = (GapicInterfaceContext) interfaceContext;
    List<ParamWithSimpleDoc> pagedMethodAdditionalParams =
        new ImmutableList.Builder<ParamWithSimpleDoc>()
            .addAll(csharpCommonTransformer.pagedMethodAdditionalParams())
            .addAll(csharpCommonTransformer.callSettingsParam())
            .build();

    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    // gRPC streaming methods.
    for (MethodModel method : csharpCommonTransformer.getSupportedMethods(context)) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodContext requestMethodContext = context.asRequestMethodContext(method);
      if (methodConfig.isGrpcStreaming()) {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodContext methodContext =
                context
                    .asFlattenedMethodContext(requestMethodContext, flatteningGroup)
                    .withCallingForms(
                        ImmutableList.of(
                            CallingForm.FlattenedStreamingBidi,
                            CallingForm.FlattenedStreamingServer));
            apiMethods.add(
                generateGrpcStreamingFlattenedMethod(
                    methodContext, csharpCommonTransformer.callSettingsParam(), null));
            if (FlatteningConfig.hasAnyResourceNameParameter(flatteningGroup)) {
              apiMethods.add(
                  generateGrpcStreamingFlattenedMethod(
                      methodContext.withResourceNamesInSamplesOnly(),
                      csharpCommonTransformer.callSettingsParam(),
                      null));
            }
          }
        }
        requestMethodContext =
            requestMethodContext.withCallingForms(
                ImmutableList.of(
                    CallingForm.RequestStreamingBidi, CallingForm.RequestStreamingServer));
        apiMethods.add(generateGrpcStreamingRequestObjectMethod(requestMethodContext, null));
      } else if (requestMethodContext.isLongRunningMethodContext()) {

        // LRO methods.
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(requestMethodContext, flatteningGroup);
            apiMethods.addAll(generateFlattenedLroMethods(methodContext));
            if (FlatteningConfig.hasAnyResourceNameParameter(flatteningGroup)) {
              apiMethods.addAll(
                  generateFlattenedLroMethods(methodContext.withResourceNamesInSamplesOnly()));
            }
          }
        }
        apiMethods.add(
            generateAsyncOperationRequestObjectMethod(
                requestMethodContext.withCallingForms(
                    ImmutableList.of(CallingForm.LongRunningRequestAsyncPollUntilComplete)),
                csharpCommonTransformer.callSettingsParam(),
                true,
                null));
        apiMethods.add(
            generateOperationRequestObjectMethod(
                requestMethodContext.withCallingForms(
                    ImmutableList.of(CallingForm.LongRunningRequestPollUntilComplete)),
                csharpCommonTransformer.callSettingsParam(),
                null));
      } else if (methodConfig.isPageStreaming()) {

        // Paged streaming methods.
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            MethodContext methodContext =
                context.asFlattenedMethodContext(requestMethodContext, flatteningGroup);
            apiMethods.addAll(
                generatePageStreamingFlattenedMethods(methodContext, pagedMethodAdditionalParams));
            if (FlatteningConfig.hasAnyResourceNameParameter(flatteningGroup)) {
              apiMethods.addAll(
                  generatePageStreamingFlattenedMethods(
                      methodContext.withResourceNamesInSamplesOnly(), pagedMethodAdditionalParams));
            }
          }
        }
        apiMethods.add(
            generatePagedRequestObjectAsyncMethod(
                requestMethodContext.withCallingForms(
                    ImmutableList.of(
                        CallingForm.RequestAsyncPaged,
                        CallingForm.RequestAsyncPagedAll,
                        CallingForm.RequestAsyncPagedPageSize)),
                csharpCommonTransformer.callSettingsParam(),
                null));
        apiMethods.add(
            generatePagedRequestObjectMethod(
                requestMethodContext.withCallingForms(
                    ImmutableList.of(
                        CallingForm.RequestPaged,
                        CallingForm.RequestPagedAll,
                        CallingForm.RequestPagedPageSize)),
                csharpCommonTransformer.callSettingsParam(),
                null));
      } else {

        // Unary methods.
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(requestMethodContext, flatteningGroup);
            apiMethods.addAll(generateNormalFlattenedMethods(methodContext));
            if (FlatteningConfig.hasAnyResourceNameParameter(flatteningGroup)) {
              apiMethods.addAll(
                  generateNormalFlattenedMethods(methodContext.withResourceNamesInSamplesOnly()));
            }
          }
        }
        apiMethods.add(
            generateRequestObjectAsyncMethod(
                requestMethodContext.withCallingForms(
                    Collections.singletonList(CallingForm.RequestAsync)),
                csharpCommonTransformer.callSettingsParam(),
                ClientMethodType.AsyncRequestObjectCallSettingsMethod,
                null));
        apiMethods.add(
            generateRequestObjectAsyncMethod(
                requestMethodContext.withCallingForms(
                    Collections.singletonList(CallingForm.RequestAsync)),
                csharpCommonTransformer.cancellationTokenParam(),
                ClientMethodType.AsyncRequestObjectCancellationMethod,
                null));
        apiMethods.add(
            generateRequestObjectMethod(
                requestMethodContext.withCallingForms(
                    Collections.singletonList(CallingForm.Request)),
                csharpCommonTransformer.callSettingsParam(),
                null));
      }
    }

    return apiMethods;
  }

  @Override
  public StaticLangApiMethodView generateApiMethod(
      MethodContext methodContext, SampleContext sampleContext) {
    List<ParamWithSimpleDoc> pagedMethodAdditionalParams =
        new ImmutableList.Builder<ParamWithSimpleDoc>()
            .addAll(csharpCommonTransformer.pagedMethodAdditionalParams())
            .addAll(csharpCommonTransformer.callSettingsParam())
            .build();
    List<ParamWithSimpleDoc> callSettingsParam = csharpCommonTransformer.callSettingsParam();
    switch (sampleContext.clientMethodType()) {
        // Unary and gRPC streaming
      case FlattenedMethod:
        return generateFlattenedMethod(methodContext, callSettingsParam, sampleContext);
      case RequestObjectMethod:
        return generateRequestObjectMethod(methodContext, callSettingsParam, sampleContext);
      case AsyncRequestObjectMethod:
        return generateRequestObjectAsyncMethod(
            methodContext,
            callSettingsParam,
            ClientMethodType.AsyncRequestObjectCallSettingsMethod,
            sampleContext);
      case FlattenedAsyncCallSettingsMethod:
        return generateFlattenedAsyncMethod(
            methodContext,
            callSettingsParam,
            ClientMethodType.FlattenedAsyncCallSettingsMethod,
            sampleContext);

        // Paged streaming
      case PagedFlattenedMethod:
        return generatePagedFlattenedMethod(
            methodContext, pagedMethodAdditionalParams, sampleContext);
      case PagedRequestObjectMethod:
        return generatePagedRequestObjectMethod(
            methodContext, csharpCommonTransformer.callSettingsParam(), sampleContext);

      case PagedFlattenedAsyncMethod:
        return generateFlattenedAsyncMethod(
            methodContext,
            callSettingsParam,
            ClientMethodType.FlattenedAsyncCallSettingsMethod,
            sampleContext);
      case AsyncPagedRequestObjectMethod:
        return generatePagedRequestObjectAsyncMethod(
            methodContext, callSettingsParam, sampleContext);

        // LRO
      case OperationRequestObjectMethod:
        return generateOperationRequestObjectMethod(
            methodContext, callSettingsParam, sampleContext);
      case OperationFlattenedMethod:
        return generateOperationFlattenedMethod(methodContext, callSettingsParam, sampleContext);
      case AsyncOperationFlattenedMethod:
        return generateAsyncOperationFlattenedMethod(
            methodContext,
            callSettingsParam,
            ClientMethodType.AsyncOperationFlattenedMethod,
            true,
            sampleContext);
      case AsyncOperationRequestObjectMethod:
        return generateOperationRequestObjectMethod(
            methodContext, callSettingsParam, sampleContext);
      default:
        throw new IllegalStateException(
            String.format("Unrecognized client method type: %s", sampleContext.clientMethodType()));
    }
  }

  private List<StaticLangApiMethodView> generateFlattenedLroMethods(MethodContext methodContext) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    apiMethods.add(
        generateAsyncOperationFlattenedMethod(
            methodContext.withCallingForms(
                ImmutableList.of(CallingForm.LongRunningFlattenedAsyncPollUntilComplete)),
            csharpCommonTransformer.callSettingsParam(),
            ClientMethodType.AsyncOperationFlattenedCallSettingsMethod,
            true,
            null));
    apiMethods.add(
        generateAsyncOperationFlattenedMethod(
            methodContext.withCallingForms(Collections.emptyList()),
            csharpCommonTransformer.cancellationTokenParam(),
            ClientMethodType.AsyncOperationFlattenedCancellationMethod,
            true,
            null));
    apiMethods.add(
        generateOperationFlattenedMethod(
            methodContext.withCallingForms(
                ImmutableList.of(CallingForm.LongRunningFlattenedPollUntilComplete)),
            csharpCommonTransformer.callSettingsParam(),
            null));
    return apiMethods;
  }

  private List<StaticLangApiMethodView> generatePageStreamingFlattenedMethods(
      MethodContext methodContext, List<ParamWithSimpleDoc> pagedMethodAdditionalParams) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    apiMethods.add(
        generatePagedFlattenedAsyncMethod(
            methodContext.withCallingForms(
                ImmutableList.of(
                    CallingForm.FlattenedAsyncPaged,
                    CallingForm.FlattenedAsyncPagedAll,
                    CallingForm.FlattenedAsyncPagedPageSize)),
            pagedMethodAdditionalParams,
            null));
    apiMethods.add(
        generatePagedFlattenedMethod(
            methodContext.withCallingForms(
                ImmutableList.of(
                    CallingForm.FlattenedPaged,
                    CallingForm.FlattenedPagedAll,
                    CallingForm.FlattenedPagedPageSize)),
            pagedMethodAdditionalParams,
            null));
    return apiMethods;
  }

  private List<StaticLangApiMethodView> generateNormalFlattenedMethods(
      MethodContext methodContext) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    apiMethods.add(
        generateFlattenedAsyncMethod(
            methodContext.withCallingForms(Collections.singletonList(CallingForm.FlattenedAsync)),
            csharpCommonTransformer.callSettingsParam(),
            ClientMethodType.FlattenedAsyncCallSettingsMethod,
            null));
    apiMethods.add(
        generateFlattenedAsyncMethod(
            methodContext.withCallingForms(Collections.singletonList(CallingForm.FlattenedAsync)),
            csharpCommonTransformer.cancellationTokenParam(),
            ClientMethodType.FlattenedAsyncCancellationTokenMethod,
            null));
    apiMethods.add(
        generateFlattenedMethod(
            methodContext.withCallingForms(Collections.singletonList(CallingForm.Flattened)),
            csharpCommonTransformer.callSettingsParam(),
            null));
    return apiMethods;
  }
}
