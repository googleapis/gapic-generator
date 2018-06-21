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
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.gapic.ServiceMessages;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiMethodDocView;
import com.google.api.codegen.viewmodel.CallableMethodDetailView;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ListMethodDetailView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.PathTemplateCheckView;
import com.google.api.codegen.viewmodel.RequestObjectMethodDetailView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView.Builder;
import com.google.api.codegen.viewmodel.UnpagedListCallableMethodDetailView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * StaticLangApiMethodTransformer generates view objects from method definitions for static
 * languages.
 */
public class StaticLangApiMethodTransformer {
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final LongRunningTransformer lroTransformer = new LongRunningTransformer();
  private final StaticLangResourceObjectTransformer resourceObjectTransformer =
      new StaticLangResourceObjectTransformer();
  private final HeaderRequestParamTransformer headerRequestParamTransformer =
      new HeaderRequestParamTransformer();
  private final SampleTransformer sampleTransformer;

  public StaticLangApiMethodTransformer(SampleType sampleType) {
    this.sampleTransformer = new SampleTransformer(sampleType);
  }

  public StaticLangApiMethodTransformer() {
    this(SampleType.IN_CODE);
  }

  public StaticLangApiMethodView generatePagedFlattenedMethod(MethodContext context) {
    return generatePagedFlattenedMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedFlattenedMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(
            context.getMethodModel(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterfaceConfig(), context.getMethodModel()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setFlattenedMethodFields(
        context,
        additionalParams,
        Synchronicity.Sync,
        methodViewBuilder,
        Arrays.asList(CallingForm.FlattenedPaged));

    return methodViewBuilder.type(ClientMethodType.PagedFlattenedMethod).build();
  }

  public StaticLangApiMethodView generatePagedFlattenedAsyncMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel methodModel = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(methodModel, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(namer.getAsyncApiMethodExampleName(methodModel));
    setListMethodFields(context, Synchronicity.Async, methodViewBuilder);
    setFlattenedMethodFields(
        context,
        additionalParams,
        Synchronicity.Async,
        methodViewBuilder,
        Arrays.asList(CallingForm.FlattenedAsyncPaged));

    return methodViewBuilder.type(ClientMethodType.PagedFlattenedAsyncMethod).build();
  }

  public StaticLangApiMethodView generatePagedRequestObjectMethod(MethodContext context) {
    return generatePagedRequestObjectMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedRequestObjectMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterfaceConfig(), method));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setRequestObjectMethodFields(
        context,
        namer.getPagedCallableMethodName(method),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder,
        Arrays.asList(CallingForm.RequestPaged));

    return methodViewBuilder.type(ClientMethodType.PagedRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generatePagedRequestObjectAsyncMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethodModel(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(namer.getAsyncApiMethodExampleName(method));
    setListMethodFields(context, Synchronicity.Async, methodViewBuilder);
    setRequestObjectMethodFields(
        context,
        namer.getPagedCallableMethodName(method),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder,
        Arrays.asList(CallingForm.RequestAsyncPaged));

    return methodViewBuilder.type(ClientMethodType.AsyncPagedRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generatePagedCallableMethod(MethodContext context) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getPagedCallableMethodName(method));
    methodViewBuilder.exampleName(namer.getPagedCallableMethodExampleName(method));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setCallableMethodFields(
        context,
        namer.getPagedCallableName(method),
        methodViewBuilder,
        Arrays.asList(CallingForm.CallablePaged));

    return methodViewBuilder.type(ClientMethodType.PagedCallableMethod).build();
  }

  public StaticLangApiMethodView generateUnpagedListCallableMethod(MethodContext context) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(method));
    methodViewBuilder.exampleName(namer.getCallableMethodExampleName(method));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setCallableMethodFields(
        context,
        namer.getCallableName(method),
        methodViewBuilder,
        Arrays.asList(CallingForm.CallableList));

    String getResourceListCallName =
        namer.getFieldGetFunctionName(
            context.getFeatureConfig(),
            context.getMethodConfig().getPageStreaming().getResourcesFieldConfig());

    String resourceListParseFunction = "";
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    if (context.getFeatureConfig().useResourceNameConverters(resourceFieldConfig)) {
      resourceListParseFunction =
          namer.getResourceTypeParseListMethodName(context.getTypeTable(), resourceFieldConfig);
    }

    UnpagedListCallableMethodDetailView unpagedListCallableDetails =
        UnpagedListCallableMethodDetailView.newBuilder()
            .resourceListGetFunction(getResourceListCallName)
            .resourceListParseFunction(resourceListParseFunction)
            .build();
    methodViewBuilder.unpagedListCallableMethod(unpagedListCallableDetails);

    methodViewBuilder.responseTypeName(
        context
            .getMethodModel()
            .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));

    return methodViewBuilder.type(ClientMethodType.UnpagedListCallableMethod).build();
  }

  public StaticLangApiMethodView generateFlattenedAsyncMethod(
      MethodContext context, ClientMethodType type) {
    return generateFlattenedAsyncMethod(context, Collections.<ParamWithSimpleDoc>emptyList(), type);
  }

  public StaticLangApiMethodView generateFlattenedAsyncMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams, ClientMethodType type) {
    return generateFlattenedAsyncMethod(
        context, additionalParams, type, Arrays.asList(CallingForm.FlattenedAsync));
  }

  public StaticLangApiMethodView generateFlattenedAsyncMethod(
      MethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      ClientMethodType type,
      List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(namer.getCallableMethodExampleName(method));
    methodViewBuilder.callableName(namer.getCallableName(method));
    setFlattenedMethodFields(
        context, additionalParams, Synchronicity.Async, methodViewBuilder, callingForms);
    setStaticLangAsyncReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateFlattenedMethod(MethodContext context) {
    return generateFlattenedMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateFlattenedMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterfaceConfig(), method));
    methodViewBuilder.callableName(namer.getCallableName(method));
    setFlattenedMethodFields(
        context,
        additionalParams,
        Synchronicity.Sync,
        methodViewBuilder,
        Arrays.asList(CallingForm.Flattened));
    setStaticLangReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.FlattenedMethod).build();
  }

  public StaticLangApiMethodView generateRequestObjectMethod(MethodContext context) {
    return generateRequestObjectMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateRequestObjectMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterfaceConfig(), method));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(method),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder,
        Arrays.asList(CallingForm.Request));
    setStaticLangReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.RequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateRequestObjectAsyncMethod(MethodContext context) {
    return generateRequestObjectAsyncMethod(
        context,
        Collections.<ParamWithSimpleDoc>emptyList(),
        ClientMethodType.AsyncRequestObjectCallSettingsMethod);
  }

  public StaticLangApiMethodView generateRequestObjectAsyncMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams, ClientMethodType type) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(namer.getAsyncApiMethodExampleName(method));
    setRequestObjectMethodFields(
        context,
        namer.getCallableAsyncMethodName(method),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder,
        Arrays.asList(CallingForm.RequestAsync));
    setStaticLangAsyncReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateCallableMethod(MethodContext context) {
    return generateCallableMethod(context, Arrays.asList(CallingForm.Callable));
  }

  public StaticLangApiMethodView generateCallableMethod(
      MethodContext context, List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(method));
    methodViewBuilder.exampleName(context.getNamer().getCallableMethodExampleName(method));
    setCallableMethodFields(
        context, namer.getCallableName(method), methodViewBuilder, callingForms);
    methodViewBuilder.responseTypeName(
        context
            .getMethodModel()
            .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer()));

    return methodViewBuilder.type(ClientMethodType.CallableMethod).build();
  }

  public StaticLangApiMethodView generateGrpcStreamingRequestObjectMethod(MethodContext context) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getGrpcStreamingApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getGrpcStreamingApiMethodExampleName(context.getInterfaceConfig(), method));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(method),
        Synchronicity.Sync,
        methodViewBuilder,
        Arrays.asList(CallingForm.RequestStreamingServer));
    setStaticLangGrpcStreamingReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.RequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateGrpcStreamingFlattenedMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getGrpcStreamingApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getGrpcStreamingApiMethodExampleName(context.getInterfaceConfig(), method));
    methodViewBuilder.callableName(namer.getCallableName(method));
    setFlattenedMethodFields(
        context,
        additionalParams,
        Synchronicity.Sync,
        methodViewBuilder,
        Arrays.asList(CallingForm.Flattened));
    setStaticLangGrpcStreamingReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.FlattenedMethod).build();
  }

  public StaticLangApiMethodView generateOperationRequestObjectMethod(MethodContext context) {
    return generateOperationRequestObjectMethod(
        context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateOperationRequestObjectMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterfaceConfig(), method));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(method),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder,
        Arrays.asList(CallingForm.LongRunningRequest));
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    TypeModel returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(ClientMethodType.OperationRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationFlattenedMethod(
      MethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterfaceConfig(), method));
    methodViewBuilder.callableName(namer.getCallableName(method));
    setFlattenedMethodFields(
        context,
        additionalParams,
        Synchronicity.Sync,
        methodViewBuilder,
        Arrays.asList(CallingForm.LongRunningFlattened));
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    TypeModel returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));
    return methodViewBuilder.type(ClientMethodType.OperationFlattenedMethod).build();
  }

  public StaticLangApiMethodView generateAsyncOperationFlattenedMethod(MethodContext context) {
    return generateAsyncOperationFlattenedMethod(
        context,
        Collections.<ParamWithSimpleDoc>emptyList(),
        ClientMethodType.AsyncOperationFlattenedMethod,
        false);
  }

  public StaticLangApiMethodView generateAsyncOperationFlattenedMethod(
      MethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      ClientMethodType type,
      boolean requiresOperationMethod) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethodModel(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(namer.getAsyncApiMethodExampleName(method));
    methodViewBuilder.callableName(namer.getCallableName(method));
    setFlattenedMethodFields(
        context,
        additionalParams,
        Synchronicity.Async,
        methodViewBuilder,
        Arrays.asList(CallingForm.LongRunningFlattenedAsync));
    if (requiresOperationMethod) {
      methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    }
    TypeModel returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateAsyncOperationRequestObjectMethod(MethodContext context) {
    return generateAsyncOperationRequestObjectMethod(
        context, Collections.<ParamWithSimpleDoc>emptyList(), false);
  }

  public StaticLangApiMethodView generateAsyncOperationRequestObjectMethod(
      MethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      boolean requiresOperationMethod) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethodModel(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(namer.getAsyncApiMethodExampleName(method));
    setRequestObjectMethodFields(
        context,
        namer.getOperationCallableMethodName(method),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder,
        Arrays.asList(CallingForm.LongRunningRequestAsync));
    if (requiresOperationMethod) {
      // Only for protobuf-based APIs.
      methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    }
    if (context.getMethodConfig().isLongRunningOperation()) {
      // Only for protobuf-based APIs.
      TypeModel returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
      methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));
      methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    } else {
      throw new IllegalArgumentException(
          "Discovery-based APIs do not have LongRunning operations.");
    }
    return methodViewBuilder.type(ClientMethodType.AsyncOperationRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationCallableMethod(MethodContext context) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getOperationCallableMethodName(method));
    methodViewBuilder.exampleName(context.getNamer().getOperationCallableMethodExampleName(method));
    setCallableMethodFields(
        context,
        namer.getOperationCallableName(method),
        methodViewBuilder,
        Arrays.asList(CallingForm.LongRunningCallable));
    TypeModel returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));

    return methodViewBuilder.type(ClientMethodType.OperationCallableMethod).build();
  }

  private void setCommonFields(
      MethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();

    String requestTypeName =
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer());
    methodViewBuilder.serviceRequestTypeName(requestTypeName);
    methodViewBuilder.serviceRequestTypeConstructor(namer.getTypeConstructor(requestTypeName));
    methodViewBuilder.serviceConstructorName(
        namer.getApiWrapperClassConstructorName(context.getInterfaceConfig()));

    setServiceResponseTypeName(context, methodViewBuilder);

    methodViewBuilder.apiClassName(namer.getApiWrapperClassName(interfaceConfig));
    methodViewBuilder.apiVariableName(namer.getApiWrapperVariableName(interfaceConfig));
    methodViewBuilder.stubName(namer.getStubName(context.getTargetInterface()));
    methodViewBuilder.settingsGetterName(namer.getSettingsFunctionName(method));
    methodViewBuilder.callableName(context.getNamer().getCallableName(method));
    methodViewBuilder.modifyMethodName(namer.getModifyMethodName(context));
    methodViewBuilder.grpcStreamingType(context.getMethodConfig().getGrpcStreamingType());
    methodViewBuilder.visibility(
        namer.getVisiblityKeyword(context.getMethodConfig().getVisibility()));
    methodViewBuilder.releaseLevelAnnotation(
        namer.getReleaseAnnotation(context.getMethodConfig().getReleaseLevel()));

    ServiceMessages messages = new ServiceMessages();
    if (context.getMethodConfig().isLongRunningOperation()) {
      methodViewBuilder.hasReturnValue(
          !context.getMethodConfig().getLongRunningConfig().getReturnType().isEmptyType());
    } else {
      methodViewBuilder.hasReturnValue(!method.isOutputTypeEmpty());
    }
    methodViewBuilder.headerRequestParams(
        headerRequestParamTransformer.generateHeaderRequestParams(context));
  }

  protected void setServiceResponseTypeName(
      MethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    if (context.getMethodConfig().isGrpcStreaming()) {
      // Only applicable for protobuf APIs.
      String returnTypeFullName =
          namer.getGrpcStreamingApiReturnTypeName(context, context.getTypeTable());
      String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
      methodViewBuilder.serviceResponseTypeName(returnTypeNickname);
    } else {
      String responseTypeName =
          context
              .getMethodModel()
              .getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer());
      methodViewBuilder.serviceResponseTypeName(responseTypeName);
    }
  }

  private void setListMethodFields(
      MethodContext context,
      Synchronicity synchronicity,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    MethodModel method = context.getMethodModel();
    ImportTypeTable typeTable = context.getTypeTable();
    SurfaceNamer namer = context.getNamer();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    String requestTypeName =
        method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer());
    String responseTypeName =
        method.getAndSaveResponseTypeName(context.getTypeTable(), context.getNamer());

    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    FieldModel resourceField = resourceFieldConfig.getField();

    String resourceTypeName;

    if (context.getFeatureConfig().useResourceNameFormatOption(resourceFieldConfig)) {
      resourceTypeName = namer.getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    } else {
      resourceTypeName = typeTable.getAndSaveNicknameForElementType(resourceField);
    }

    String iterateMethodName =
        namer.getPagedResponseIterateMethod(context.getFeatureConfig(), resourceFieldConfig);

    String resourceFieldName = namer.getFieldName(resourceField);
    String resourceFieldGetterName =
        namer.getFieldGetFunctionName(context.getFeatureConfig(), resourceFieldConfig);

    methodViewBuilder.listMethod(
        ListMethodDetailView.newBuilder()
            .requestTypeName(requestTypeName)
            .responseTypeName(responseTypeName)
            .resourceTypeName(resourceTypeName)
            .iterateMethodName(iterateMethodName)
            .resourceFieldName(resourceFieldName)
            .resourcesFieldGetFunction(resourceFieldGetterName)
            .build());

    switch (synchronicity) {
      case Sync:
        methodViewBuilder.responseTypeName(
            namer.getAndSavePagedResponseTypeName(context, resourceFieldConfig));
        break;
      case Async:
        methodViewBuilder.responseTypeName(
            namer.getAndSaveAsyncPagedResponseTypeName(context, resourceFieldConfig));
        break;
    }
  }

  private void setFlattenedMethodFields(
      MethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      Synchronicity synchronicity,
      StaticLangApiMethodView.Builder methodViewBuilder,
      List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    Iterable<FieldConfig> fieldConfigs =
        context.getFlatteningConfig().getFlattenedFieldConfigs().values();
    sampleTransformer.generateSamples(
        methodViewBuilder,
        context,
        fieldConfigs,
        InitCodeOutputType.FieldList,
        initCodeContext ->
            initCodeTransformer.generateInitCode(
                context.cloneWithEmptyTypeTable(), initCodeContext),
        callingForms);

    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(method, context.getMethodConfig()))
            .paramDocs(getMethodParamDocs(context, fieldConfigs, additionalParams))
            .throwsDocLines(namer.getThrowsDocLines(context.getMethodConfig()))
            .returnsDocLines(
                namer.getReturnDocLines(
                    context.getSurfaceInterfaceContext(), context, synchronicity))
            .build());

    List<RequestObjectParamView> params = new ArrayList<>();
    for (FieldConfig fieldConfig : fieldConfigs) {
      params.add(resourceObjectTransformer.generateRequestObjectParam(context, fieldConfig));
    }
    //    if (method.hasExtraFieldMask()) {
    //      params.add(resourceObjectTransformer.generateFieldMaskParam(context.getNamer()));
    //    }
    methodViewBuilder.forwardingMethodParams(params);
    List<RequestObjectParamView> nonforwardingParams = new ArrayList<>(params);
    nonforwardingParams.addAll(ParamWithSimpleDoc.asRequestObjectParamViews(additionalParams));
    methodViewBuilder.methodParams(nonforwardingParams);
    methodViewBuilder.requestObjectParams(params);

    methodViewBuilder.pathTemplateChecks(generatePathTemplateChecks(context, fieldConfigs));
  }

  private void setRequestObjectMethodFields(
      MethodContext context,
      String callableMethodName,
      Synchronicity sync,
      StaticLangApiMethodView.Builder methodViewBuilder,
      List<CallingForm> callingForms) {
    setRequestObjectMethodFields(
        context,
        callableMethodName,
        sync,
        Collections.<ParamWithSimpleDoc>emptyList(),
        methodViewBuilder,
        callingForms);
  }

  private void setRequestObjectMethodFields(
      MethodContext context,
      String callableMethodName,
      Synchronicity sync,
      List<ParamWithSimpleDoc> additionalParams,
      StaticLangApiMethodView.Builder methodViewBuilder,
      List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    List<ParamDocView> paramDocs = new ArrayList<>();
    paramDocs.addAll(getRequestObjectParamDocs(context));
    paramDocs.addAll(ParamWithSimpleDoc.asParamDocViews(additionalParams));
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(method, context.getMethodConfig()))
            .paramDocs(paramDocs)
            .throwsDocLines(namer.getThrowsDocLines(context.getMethodConfig()))
            .returnsDocLines(
                namer.getReturnDocLines(context.getSurfaceInterfaceContext(), context, sync))
            .build());

    sampleTransformer.generateSamples(
        methodViewBuilder,
        context,
        context.getMethodConfig().getRequiredFieldConfigs(),
        InitCodeOutputType.SingleObject,
        initCodeContext ->
            initCodeTransformer.generateInitCode(
                context.cloneWithEmptyTypeTable(), initCodeContext),
        callingForms);

    methodViewBuilder.methodParams(new ArrayList<>());
    methodViewBuilder.requestObjectParams(new ArrayList<>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<>());

    RequestObjectMethodDetailView.Builder detailBuilder =
        RequestObjectMethodDetailView.newBuilder();
    if (context.getMethodConfig().hasRequestObjectMethod()) {
      detailBuilder.accessModifier(
          context.getNamer().getVisiblityKeyword(context.getMethodConfig().getVisibility()));
    } else {
      detailBuilder.accessModifier(context.getNamer().getPrivateAccessModifier());
    }
    detailBuilder.callableMethodName(callableMethodName);
    methodViewBuilder.requestObjectMethod(detailBuilder.build());
  }

  private void setCallableMethodFields(
      MethodContext context,
      String callableName,
      Builder methodViewBuilder,
      List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(context.getNamer().getDocLines(method, context.getMethodConfig()))
            .paramDocs(new ArrayList<ParamDocView>())
            .throwsDocLines(new ArrayList<String>())
            .build());

    sampleTransformer.generateSamples(
        methodViewBuilder,
        context,
        context.getMethodConfig().getRequiredFieldConfigs(),
        InitCodeOutputType.SingleObject,
        initCodeContext ->
            initCodeTransformer.generateInitCode(
                context.cloneWithEmptyTypeTable(), initCodeContext),
        callingForms);

    methodViewBuilder.methodParams(new ArrayList<>());
    methodViewBuilder.requestObjectParams(new ArrayList<>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<>());

    String requestTypeFullName =
        context.getMethodModel().getInputTypeName(context.getTypeTable()).getFullName();
    String requestType = context.getTypeTable().getAndSaveNicknameFor(requestTypeFullName);

    String genericAwareResponseTypeFullName =
        context.getNamer().getGenericAwareResponseTypeName(context);
    String genericAwareResponseType =
        context.getTypeTable().getAndSaveNicknameFor(genericAwareResponseTypeFullName);

    MethodConfig methodConfig = context.getMethodConfig();
    ApiCallableImplType callableImplType = ApiCallableImplType.SimpleApiCallable;
    if (methodConfig.isGrpcStreaming()) {
      callableImplType = ApiCallableImplType.of(methodConfig.getGrpcStreamingType());
    } else if (methodConfig.isBatching()) {
      callableImplType = ApiCallableImplType.BatchingApiCallable;
    }

    methodViewBuilder.callableMethod(
        CallableMethodDetailView.newBuilder()
            .requestType(requestType)
            .genericAwareResponseType(genericAwareResponseType)
            .callableName(callableName)
            .interfaceTypeName(
                context.getNamer().getApiCallableTypeName(callableImplType.serviceMethodType()))
            .build());
  }

  private void setStaticLangAsyncReturnTypeName(
      MethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    String returnTypeFullName = namer.getStaticLangAsyncReturnTypeName(context);
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private void setStaticLangReturnTypeName(
      MethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    String returnTypeFullName = namer.getStaticLangReturnTypeName(context);
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private void setStaticLangGrpcStreamingReturnTypeName(
      MethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    // use the api return type name as the surface return type name
    String returnTypeFullName =
        namer.getGrpcStreamingApiReturnTypeName(context, context.getTypeTable());
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private List<PathTemplateCheckView> generatePathTemplateChecks(
      MethodContext context, Iterable<FieldConfig> fieldConfigs) {
    List<PathTemplateCheckView> pathTemplateChecks = new ArrayList<>();
    if (!context.getFeatureConfig().enableStringFormatFunctions()) {
      return pathTemplateChecks;
    }
    for (FieldConfig fieldConfig : fieldConfigs) {
      if (!fieldConfig.useValidation()) {
        // Don't generate a path template check if fieldConfig is not configured to use validation.
        continue;
      }
      FieldModel field = fieldConfig.getField();
      ImmutableMap<String, String> fieldNamePatterns =
          context.getMethodConfig().getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        SingleResourceNameConfig resourceNameConfig =
            context.getSingleResourceNameConfig(entityName);
        if (resourceNameConfig == null) {
          String methodName = context.getMethodModel().getSimpleName();
          throw new IllegalStateException(
              "No collection config with id '"
                  + entityName
                  + "' required by configuration for method '"
                  + methodName
                  + "'");
        }
        PathTemplateCheckView.Builder check = PathTemplateCheckView.newBuilder();
        check.pathTemplateName(
            context
                .getNamer()
                .getPathTemplateName(context.getInterfaceConfig(), resourceNameConfig));
        check.paramName(context.getNamer().getVariableName(field));
        check.allowEmptyString(shouldAllowEmpty(context, field));
        check.validationMessageContext(
            context
                .getNamer()
                .getApiMethodName(
                    context.getMethodModel(), context.getMethodConfig().getVisibility()));
        pathTemplateChecks.add(check.build());
      }
    }
    return pathTemplateChecks;
  }

  private boolean shouldAllowEmpty(MethodContext context, FieldModel field) {
    for (FieldModel requiredField : context.getMethodConfig().getRequiredFields()) {
      if (requiredField.equals(field)) {
        return false;
      }
    }
    return true;
  }

  private List<ParamDocView> getMethodParamDocs(
      MethodContext context,
      Iterable<FieldConfig> fieldConfigs,
      List<ParamWithSimpleDoc> additionalParamDocs) {
    MethodModel method = context.getMethodModel();
    List<ParamDocView> allDocs = new ArrayList<>();
    if (method.getRequestStreaming()) {
      allDocs.addAll(ParamWithSimpleDoc.asParamDocViews(additionalParamDocs));
      return allDocs;
    }
    for (FieldConfig fieldConfig : fieldConfigs) {
      FieldModel field = fieldConfig.getField();
      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName(context.getNamer().getVariableName(field));
      paramDoc.typeName(context.getTypeTable().getAndSaveNicknameFor(field));

      List<String> docLines = null;
      MethodConfig methodConfig = context.getMethodConfig();
      if (methodConfig.isPageStreaming()
          && methodConfig.getPageStreaming().hasPageSizeField()
          && field.equals(methodConfig.getPageStreaming().getPageSizeField())) {
        docLines =
            Arrays.asList(
                new String[] {
                  "The maximum number of resources contained in the underlying API",
                  "response. The API may return fewer values in a page, even if",
                  "there are additional values to be retrieved."
                });
      } else if (methodConfig.isPageStreaming()
          && field.equals(methodConfig.getPageStreaming().getRequestTokenField())) {
        docLines =
            Arrays.asList(
                new String[] {
                  "A page token is used to specify a page of values to be returned.",
                  "If no page token is specified (the default), the first page",
                  "of values will be returned. Any page token used here must have",
                  "been generated by a previous call to the API."
                });
      } else {
        docLines = context.getNamer().getDocLines(field);
      }

      paramDoc.lines(docLines);

      allDocs.add(paramDoc.build());
    }
    allDocs.addAll(ParamWithSimpleDoc.asParamDocViews(additionalParamDocs));
    return allDocs;
  }

  public List<SimpleParamDocView> getRequestObjectParamDocs(MethodContext context) {
    MethodModel method = context.getMethodModel();
    SimpleParamDocView doc =
        SimpleParamDocView.newBuilder()
            .paramName("request")
            .typeName(method.getAndSaveRequestTypeName(context.getTypeTable(), context.getNamer()))
            .lines(
                Arrays.<String>asList(
                    "The request object containing all of the parameters for the API call."))
            .build();
    return ImmutableList.of(doc);
  }
}
