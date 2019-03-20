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
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
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
        "SampleGen is for protobuf-based API" + " only.");
    GapicInterfaceContext context = (GapicInterfaceContext) interfaceContext;
    List<ParamWithSimpleDoc> pagedMethodAdditionalParams =
        new ImmutableList.Builder<ParamWithSimpleDoc>()
            .addAll(csharpCommonTransformer.pagedMethodAdditionalParams())
            .addAll(csharpCommonTransformer.callSettingsParam())
            .build();

    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (MethodModel method : csharpCommonTransformer.getSupportedMethods(context)) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodContext requestMethodContext = context.asRequestMethodContext(method);
      if (methodConfig.isGrpcStreaming()) {
        // Only for protobuf-based APIs.
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(requestMethodContext, flatteningGroup);
            apiMethods.add(
                generateGrpcStreamingFlattenedMethod(
                    methodContext, csharpCommonTransformer.callSettingsParam()));
          }
        }
        apiMethods.add(generateGrpcStreamingRequestObjectMethod(requestMethodContext));
      } else if (requestMethodContext.isLongRunningMethodContext()) {
        // Only for protobuf-based APIs.
        GapicMethodContext gapicMethodContext = (GapicMethodContext) requestMethodContext;
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(requestMethodContext, flatteningGroup);
            apiMethods.add(
                generateAsyncOperationFlattenedMethod(
                    methodContext,
                    csharpCommonTransformer.callSettingsParam(),
                    ClientMethodType.AsyncOperationFlattenedCallSettingsMethod,
                    true));
            apiMethods.add(
                generateAsyncOperationFlattenedMethod(
                    methodContext,
                    csharpCommonTransformer.cancellationTokenParam(),
                    ClientMethodType.AsyncOperationFlattenedCancellationMethod,
                    true));
            apiMethods.add(
                generateOperationFlattenedMethod(
                    methodContext, csharpCommonTransformer.callSettingsParam()));
          }
        }
        apiMethods.add(
            generateAsyncOperationRequestObjectMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam(), true));
        apiMethods.add(
            generateOperationRequestObjectMethod(
                gapicMethodContext, csharpCommonTransformer.callSettingsParam()));
      } else if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(requestMethodContext, flatteningGroup);
            apiMethods.add(
                generatePagedFlattenedAsyncMethod(methodContext, pagedMethodAdditionalParams));
            apiMethods.add(
                generatePagedFlattenedMethod(methodContext, pagedMethodAdditionalParams));
          }
        }
        apiMethods.add(
            generatePagedRequestObjectAsyncMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
        apiMethods.add(
            generatePagedRequestObjectMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
      } else {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            GapicMethodContext methodContext =
                context.asFlattenedMethodContext(requestMethodContext, flatteningGroup);
            apiMethods.add(
                generateFlattenedAsyncMethod(
                    methodContext,
                    csharpCommonTransformer.callSettingsParam(),
                    ClientMethodType.FlattenedAsyncCallSettingsMethod));
            apiMethods.add(
                generateFlattenedAsyncMethod(
                    methodContext,
                    csharpCommonTransformer.cancellationTokenParam(),
                    ClientMethodType.FlattenedAsyncCancellationTokenMethod));
            apiMethods.add(
                generateFlattenedMethod(
                    methodContext, csharpCommonTransformer.callSettingsParam()));
          }
        }
        apiMethods.add(
            generateRequestObjectAsyncMethod(
                requestMethodContext,
                csharpCommonTransformer.callSettingsParam(),
                ClientMethodType.AsyncRequestObjectCallSettingsMethod));
        apiMethods.add(
            generateRequestObjectAsyncMethod(
                requestMethodContext,
                csharpCommonTransformer.cancellationTokenParam(),
                ClientMethodType.AsyncRequestObjectCancellationMethod));
        apiMethods.add(
            generateRequestObjectMethod(
                requestMethodContext, csharpCommonTransformer.callSettingsParam()));
      }
    }

    return apiMethods;
  }
}
