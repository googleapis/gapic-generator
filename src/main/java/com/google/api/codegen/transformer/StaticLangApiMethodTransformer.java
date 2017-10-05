/* Copyright 2017 Google Inc
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiCallableImplType;
import com.google.api.codegen.viewmodel.ApiMethodDocView;
import com.google.api.codegen.viewmodel.CallableMethodDetailView;
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
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
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

  public StaticLangApiMethodView generatePagedFlattenedMethod(GapicMethodContext context) {
    return generatePagedFlattenedMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedFlattenedMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Sync, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedFlattenedMethod).build();
  }

  public StaticLangApiMethodView generatePagedFlattenedAsyncMethod(GapicMethodContext context) {
    return generatePagedFlattenedAsyncMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedFlattenedAsyncMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Async, methodViewBuilder);
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Async, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedFlattenedAsyncMethod).build();
  }

  public StaticLangApiMethodView generatePagedRequestObjectMethod(GapicMethodContext context) {
    return generatePagedRequestObjectMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generatePagedRequestObjectMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setRequestObjectMethodFields(
        context,
        namer.getPagedCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generatePagedRequestObjectAsyncMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Async, methodViewBuilder);
    setRequestObjectMethodFields(
        context,
        namer.getPagedCallableMethodName(context.getMethod()),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.AsyncPagedRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generatePagedCallableMethod(GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getPagedCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        namer.getPagedCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setCallableMethodFields(
        context, namer.getPagedCallableName(context.getMethod()), methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.PagedCallableMethod).build();
  }

  public StaticLangApiMethodView generateUnpagedListCallableMethod(GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        namer.getCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setListMethodFields(context, Synchronicity.Sync, methodViewBuilder);
    setCallableMethodFields(context, namer.getCallableName(context.getMethod()), methodViewBuilder);

    String getResourceListCallName =
        namer.getFieldGetFunctionName(
            context.getFeatureConfig(),
            context.getMethodConfig().getPageStreaming().getResourcesFieldConfig());

    UnpagedListCallableMethodDetailView unpagedListCallableDetails =
        UnpagedListCallableMethodDetailView.newBuilder()
            .resourceListGetFunction(getResourceListCallName)
            .build();
    methodViewBuilder.unpagedListCallableMethod(unpagedListCallableDetails);

    methodViewBuilder.responseTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType()));

    return methodViewBuilder.type(ClientMethodType.UnpagedListCallableMethod).build();
  }

  public StaticLangApiMethodView generateFlattenedAsyncMethod(
      GapicMethodContext context, ClientMethodType type) {
    return generateFlattenedAsyncMethod(context, Collections.<ParamWithSimpleDoc>emptyList(), type);
  }

  public StaticLangApiMethodView generateFlattenedAsyncMethod(
      GapicMethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      ClientMethodType type) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getCallableMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Async, methodViewBuilder);
    setStaticLangAsyncReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateFlattenedMethod(GapicMethodContext context) {
    return generateFlattenedMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateFlattenedMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Sync, methodViewBuilder);
    setStaticLangReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.FlattenedMethod).build();
  }

  public StaticLangApiMethodView generateRequestObjectMethod(GapicMethodContext context) {
    return generateRequestObjectMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateRequestObjectMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder);
    setStaticLangReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.RequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateRequestObjectAsyncMethod(GapicMethodContext context) {
    return generateRequestObjectAsyncMethod(context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateRequestObjectAsyncMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableAsyncMethodName(context.getMethod()),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder);
    setStaticLangAsyncReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.AsyncRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateCallableMethod(GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        context
            .getNamer()
            .getCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setCallableMethodFields(context, namer.getCallableName(context.getMethod()), methodViewBuilder);
    methodViewBuilder.responseTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType()));

    return methodViewBuilder.type(ClientMethodType.CallableMethod).build();
  }

  public StaticLangApiMethodView generateGrpcStreamingRequestObjectMethod(
      GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getGrpcStreamingApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        context
            .getNamer()
            .getGrpcStreamingApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        methodViewBuilder);
    setStaticLangGrpcStreamingReturnTypeName(context, methodViewBuilder);

    return methodViewBuilder.type(ClientMethodType.RequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationRequestObjectMethod(GapicMethodContext context) {
    return generateOperationRequestObjectMethod(
        context, Collections.<ParamWithSimpleDoc>emptyList());
  }

  public StaticLangApiMethodView generateOperationRequestObjectMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getCallableMethodName(context.getMethod()),
        Synchronicity.Sync,
        additionalParams,
        methodViewBuilder);
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(ClientMethodType.OperationRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationFlattenedMethod(
      GapicMethodContext context, List<ParamWithSimpleDoc> additionalParams) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getApiMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Sync, methodViewBuilder);
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));

    return methodViewBuilder.type(ClientMethodType.OperationFlattenedMethod).build();
  }

  public StaticLangApiMethodView generateAsyncOperationFlattenedMethod(GapicMethodContext context) {
    return generateAsyncOperationFlattenedMethod(
        context,
        Collections.<ParamWithSimpleDoc>emptyList(),
        ClientMethodType.AsyncOperationFlattenedMethod,
        false);
  }

  public StaticLangApiMethodView generateAsyncOperationFlattenedMethod(
      GapicMethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      ClientMethodType type,
      boolean requiresOperationMethod) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    methodViewBuilder.callableName(namer.getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, additionalParams, Synchronicity.Async, methodViewBuilder);
    if (requiresOperationMethod) {
      methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    }
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateAsyncOperationRequestObjectMethod(
      GapicMethodContext context) {
    return generateAsyncOperationRequestObjectMethod(
        context, Collections.<ParamWithSimpleDoc>emptyList(), false);
  }

  public StaticLangApiMethodView generateAsyncOperationRequestObjectMethod(
      GapicMethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      boolean requiresOperationMethod) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(
        namer.getAsyncApiMethodName(
            context.getMethod(), context.getMethodConfig().getVisibility()));
    methodViewBuilder.exampleName(
        namer.getAsyncApiMethodExampleName(context.getInterface(), context.getMethod()));
    setRequestObjectMethodFields(
        context,
        namer.getOperationCallableMethodName(context.getMethod()),
        Synchronicity.Async,
        additionalParams,
        methodViewBuilder);
    if (requiresOperationMethod) {
      methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));
    }
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));

    return methodViewBuilder.type(ClientMethodType.AsyncOperationRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateOperationCallableMethod(GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getOperationCallableMethodName(context.getMethod()));
    methodViewBuilder.exampleName(
        context
            .getNamer()
            .getOperationCallableMethodExampleName(context.getInterface(), context.getMethod()));
    setCallableMethodFields(
        context, namer.getOperationCallableName(context.getMethod()), methodViewBuilder);
    TypeRef returnType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    methodViewBuilder.responseTypeName(context.getTypeTable().getAndSaveNicknameFor(returnType));
    methodViewBuilder.operationMethod(lroTransformer.generateDetailView(context));

    return methodViewBuilder.type(ClientMethodType.OperationCallableMethod).build();
  }

  private void setCommonFields(
      GapicMethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();

    String requestTypeName =
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getInputType());
    methodViewBuilder.serviceRequestTypeName(requestTypeName);
    methodViewBuilder.serviceRequestTypeConstructor(namer.getTypeConstructor(requestTypeName));

    setServiceResponseTypeName(context, methodViewBuilder);

    methodViewBuilder.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    methodViewBuilder.apiVariableName(
        namer.getApiWrapperVariableName(context.getInterfaceConfig()));
    methodViewBuilder.stubName(namer.getStubName(context.getTargetInterface()));
    methodViewBuilder.settingsGetterName(namer.getSettingsFunctionName(context.getMethod()));
    methodViewBuilder.callableName(context.getNamer().getCallableName(context.getMethod()));
    methodViewBuilder.modifyMethodName(namer.getModifyMethodName(context.getMethod()));
    methodViewBuilder.grpcStreamingType(context.getMethodConfig().getGrpcStreamingType());
    methodViewBuilder.visibility(
        namer.getVisiblityKeyword(context.getMethodConfig().getVisibility()));
    methodViewBuilder.releaseLevelAnnotation(
        namer.getReleaseAnnotation(context.getMethodConfig().getReleaseLevel()));

    ServiceMessages messages = new ServiceMessages();
    if (context.getMethodConfig().isLongRunningOperation()) {
      methodViewBuilder.hasReturnValue(
          !messages.isEmptyType(context.getMethodConfig().getLongRunningConfig().getReturnType()));
    } else {
      methodViewBuilder.hasReturnValue(!messages.isEmptyType(context.getMethod().getOutputType()));
    }
  }

  protected void setServiceResponseTypeName(
      GapicMethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    if (context.getMethodConfig().isGrpcStreaming()) {
      String returnTypeFullName =
          namer.getGrpcStreamingApiReturnTypeName(context.getMethod(), context.getTypeTable());
      String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
      methodViewBuilder.serviceResponseTypeName(returnTypeNickname);
    } else {
      String responseTypeName =
          context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType());
      methodViewBuilder.serviceResponseTypeName(responseTypeName);
    }
  }

  private void setListMethodFields(
      GapicMethodContext context,
      Synchronicity synchronicity,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    ModelTypeTable typeTable = context.getTypeTable();
    SurfaceNamer namer = context.getNamer();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    String requestTypeName = typeTable.getAndSaveNicknameFor(context.getMethod().getInputType());
    String responseTypeName = typeTable.getAndSaveNicknameFor(context.getMethod().getOutputType());

    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    Field resourceField = resourceFieldConfig.getField();

    String resourceTypeName;

    if (context.getFeatureConfig().useResourceNameFormatOption(resourceFieldConfig)) {
      resourceTypeName = namer.getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    } else {
      resourceTypeName = typeTable.getAndSaveNicknameForElementType(resourceField.getType());
    }

    String iterateMethodName =
        context
            .getNamer()
            .getPagedResponseIterateMethod(context.getFeatureConfig(), resourceFieldConfig);

    String resourceFieldName = context.getNamer().getFieldName(resourceField);
    String resourceFieldGetFunctionName =
        namer.getFieldGetFunctionName(context.getFeatureConfig(), resourceFieldConfig);

    methodViewBuilder.listMethod(
        ListMethodDetailView.newBuilder()
            .requestTypeName(requestTypeName)
            .responseTypeName(responseTypeName)
            .resourceTypeName(resourceTypeName)
            .iterateMethodName(iterateMethodName)
            .resourceFieldName(resourceFieldName)
            .resourcesFieldGetFunction(resourceFieldGetFunctionName)
            .build());

    switch (synchronicity) {
      case Sync:
        methodViewBuilder.responseTypeName(
            namer.getAndSavePagedResponseTypeName(
                context.getMethod(), context.getTypeTable(), resourceFieldConfig));
        break;
      case Async:
        methodViewBuilder.responseTypeName(
            namer.getAndSaveAsyncPagedResponseTypeName(
                context.getMethod(), context.getTypeTable(), resourceFieldConfig));
        break;
    }
  }

  private void setFlattenedMethodFields(
      GapicMethodContext context,
      List<ParamWithSimpleDoc> additionalParams,
      Synchronicity synchronicity,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    Iterable<FieldConfig> fieldConfigs =
        context.getFlatteningConfig().getFlattenedFieldConfigs().values();
    methodViewBuilder.initCode(
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(context, fieldConfigs, InitCodeOutputType.FieldList)));
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(context.getMethod(), context.getMethodConfig()))
            .paramDocs(getMethodParamDocs(context, fieldConfigs, additionalParams))
            .throwsDocLines(namer.getThrowsDocLines(context.getMethodConfig()))
            .returnsDocLines(
                namer.getReturnDocLines(
                    context.getSurfaceTransformerContext(),
                    context.getMethodConfig(),
                    synchronicity))
            .build());

    List<RequestObjectParamView> params = new ArrayList<>();
    for (FieldConfig fieldConfig : fieldConfigs) {
      params.add(generateRequestObjectParam(context, fieldConfig));
    }
    methodViewBuilder.forwardingMethodParams(params);
    List<RequestObjectParamView> nonforwardingParams = new ArrayList<>(params);
    nonforwardingParams.addAll(ParamWithSimpleDoc.asRequestObjectParamViews(additionalParams));
    methodViewBuilder.methodParams(nonforwardingParams);
    methodViewBuilder.requestObjectParams(params);

    methodViewBuilder.pathTemplateChecks(generatePathTemplateChecks(context, fieldConfigs));
  }

  private void setRequestObjectMethodFields(
      GapicMethodContext context,
      String callableMethodName,
      Synchronicity sync,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    setRequestObjectMethodFields(
        context,
        callableMethodName,
        sync,
        Collections.<ParamWithSimpleDoc>emptyList(),
        methodViewBuilder);
  }

  private void setRequestObjectMethodFields(
      GapicMethodContext context,
      String callableMethodName,
      Synchronicity sync,
      List<ParamWithSimpleDoc> additionalParams,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    List<ParamDocView> paramDocs = new ArrayList<>();
    paramDocs.addAll(getRequestObjectParamDocs(context, context.getMethod().getInputType()));
    paramDocs.addAll(ParamWithSimpleDoc.asParamDocViews(additionalParams));
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(context.getMethod(), context.getMethodConfig()))
            .paramDocs(paramDocs)
            .throwsDocLines(namer.getThrowsDocLines(context.getMethodConfig()))
            .returnsDocLines(
                namer.getReturnDocLines(
                    context.getSurfaceTransformerContext(), context.getMethodConfig(), sync))
            .build());
    methodViewBuilder.initCode(
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(
                context,
                context.getMethodConfig().getRequiredFieldConfigs(),
                InitCodeOutputType.SingleObject)));

    methodViewBuilder.methodParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.requestObjectParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<PathTemplateCheckView>());

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
      GapicMethodContext context, String callableName, Builder methodViewBuilder) {
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(
                context.getNamer().getDocLines(context.getMethod(), context.getMethodConfig()))
            .paramDocs(new ArrayList<ParamDocView>())
            .throwsDocLines(new ArrayList<String>())
            .build());
    methodViewBuilder.initCode(
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(
                context,
                context.getMethodConfig().getRequiredFieldConfigs(),
                InitCodeOutputType.SingleObject)));

    methodViewBuilder.methodParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.requestObjectParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<PathTemplateCheckView>());

    String genericAwareResponseTypeFullName =
        context.getNamer().getGenericAwareResponseTypeName(context.getMethod().getOutputType());
    String genericAwareResponseType =
        context.getTypeTable().getAndSaveNicknameFor(genericAwareResponseTypeFullName);

    GapicMethodConfig methodConfig = context.getMethodConfig();
    ApiCallableImplType callableImplType = ApiCallableImplType.SimpleApiCallable;
    if (methodConfig.isGrpcStreaming()) {
      callableImplType = ApiCallableImplType.of(methodConfig.getGrpcStreamingType());
    } else if (methodConfig.isBatching()) {
      callableImplType = ApiCallableImplType.BatchingApiCallable;
    } else if (methodConfig.isLongRunningOperation()) {
      callableImplType = ApiCallableImplType.InitialOperationApiCallable;
    }

    methodViewBuilder.callableMethod(
        CallableMethodDetailView.newBuilder()
            .genericAwareResponseType(genericAwareResponseType)
            .callableName(callableName)
            .interfaceTypeName(
                context.getNamer().getApiCallableTypeName(callableImplType.serviceMethodType()))
            .build());
  }

  private void setStaticLangAsyncReturnTypeName(
      GapicMethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    String returnTypeFullName =
        namer.getStaticLangAsyncReturnTypeName(context.getMethod(), context.getMethodConfig());
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private void setStaticLangReturnTypeName(
      GapicMethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    String returnTypeFullName =
        namer.getStaticLangReturnTypeName(context.getMethod(), context.getMethodConfig());
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private void setStaticLangGrpcStreamingReturnTypeName(
      GapicMethodContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    // use the api return type name as the surface return type name
    String returnTypeFullName =
        namer.getGrpcStreamingApiReturnTypeName(context.getMethod(), context.getTypeTable());
    String returnTypeNickname = context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName);
    methodViewBuilder.responseTypeName(returnTypeNickname);
  }

  private List<PathTemplateCheckView> generatePathTemplateChecks(
      GapicMethodContext context, Iterable<FieldConfig> fieldConfigs) {
    List<PathTemplateCheckView> pathTemplateChecks = new ArrayList<>();
    if (!context.getFeatureConfig().enableStringFormatFunctions()) {
      return pathTemplateChecks;
    }
    for (FieldConfig fieldConfig : fieldConfigs) {
      if (!fieldConfig.useValidation()) {
        // Don't generate a path template check if fieldConfig is not configured to use validation.
        continue;
      }
      Field field = fieldConfig.getField();
      ImmutableMap<String, String> fieldNamePatterns =
          context.getMethodConfig().getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        SingleResourceNameConfig resourceNameConfig =
            context.getSingleResourceNameConfig(entityName);
        if (resourceNameConfig == null) {
          String methodName = context.getMethod().getSimpleName();
          throw new IllegalStateException(
              "No collection config with id '"
                  + entityName
                  + "' required by configuration for method '"
                  + methodName
                  + "'");
        }
        PathTemplateCheckView.Builder check = PathTemplateCheckView.newBuilder();
        check.pathTemplateName(
            context.getNamer().getPathTemplateName(context.getInterface(), resourceNameConfig));
        check.paramName(context.getNamer().getVariableName(field));
        check.allowEmptyString(shouldAllowEmpty(context, field));
        check.validationMessageContext(
            context
                .getNamer()
                .getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
        pathTemplateChecks.add(check.build());
      }
    }
    return pathTemplateChecks;
  }

  private boolean shouldAllowEmpty(GapicMethodContext context, Field field) {
    for (Field requiredField : context.getMethodConfig().getRequiredFields()) {
      if (requiredField.equals(field)) {
        return false;
      }
    }
    return true;
  }

  private RequestObjectParamView generateRequestObjectParam(
      GapicMethodContext context, FieldConfig fieldConfig) {
    SurfaceNamer namer = context.getNamer();
    FeatureConfig featureConfig = context.getFeatureConfig();
    ModelTypeTable typeTable = context.getTypeTable();
    Field field = fieldConfig.getField();

    Iterable<Field> requiredFields = context.getMethodConfig().getRequiredFields();
    boolean isRequired = false;
    for (Field f : requiredFields) {
      if (f.getSimpleName().equals(field.getSimpleName())) {
        isRequired = true;
      }
    }

    String typeName =
        namer.getNotImplementedString(
            "StaticLangApiMethodTransformer.generateRequestObjectParam - typeName");
    String elementTypeName =
        namer.getNotImplementedString(
            "StaticLangApiMethodTransformer.generateRequestObjectParam - elementTypeName");

    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
      if (namer.shouldImportRequestObjectParamType(field)) {
        typeName = namer.getAndSaveResourceTypeName(typeTable, fieldConfig);
      }
      if (namer.shouldImportRequestObjectParamElementType(field)) {
        // Use makeOptional to remove repeated property from type
        elementTypeName = namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig);
      }
    } else {
      if (namer.shouldImportRequestObjectParamType(field)) {
        typeName = typeTable.getAndSaveNicknameFor(field.getType());
        if (!isRequired) {
          typeName = namer.makePrimitiveTypeNullable(typeName, field.getType());
        }
      }
      if (namer.shouldImportRequestObjectParamElementType(field)) {
        elementTypeName = typeTable.getAndSaveNicknameForElementType(field.getType());
      }
    }

    String setCallName = namer.getFieldSetFunctionName(featureConfig, fieldConfig);
    String addCallName = namer.getFieldAddFunctionName(field);
    String getCallName = namer.getFieldGetFunctionName(field);
    String transformParamFunctionName = null;
    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)
        && fieldConfig.requiresParamTransformation()) {
      if (!fieldConfig.requiresParamTransformationFromAny()) {
        transformParamFunctionName = namer.getResourceOneofCreateMethod(typeTable, fieldConfig);
      }
    }

    RequestObjectParamView.Builder param = RequestObjectParamView.newBuilder();
    param.name(namer.getVariableName(field));
    param.keyName(namer.getFieldKey(field));
    param.nameAsMethodName(namer.getFieldGetFunctionName(featureConfig, fieldConfig));
    param.typeName(typeName);
    param.elementTypeName(elementTypeName);
    param.setCallName(setCallName);
    param.addCallName(addCallName);
    param.getCallName(getCallName);
    param.transformParamFunctionName(transformParamFunctionName);
    param.isMap(field.getType().isMap());
    param.isArray(!field.getType().isMap() && field.getType().isRepeated());
    param.isPrimitive(namer.isPrimitive(field.getType()));
    param.isOptional(!isRequired);
    if (!isRequired) {
      param.optionalDefault(namer.getOptionalFieldDefaultValue(fieldConfig, context));
    }

    return param.build();
  }

  private List<ParamDocView> getMethodParamDocs(
      GapicMethodContext context,
      Iterable<FieldConfig> fieldConfigs,
      List<ParamWithSimpleDoc> additionalParamDocs) {
    List<ParamDocView> allDocs = new ArrayList<>();
    if (context.getMethod().getRequestStreaming()) {
      allDocs.addAll(ParamWithSimpleDoc.asParamDocViews(additionalParamDocs));
      return allDocs;
    }
    for (FieldConfig fieldConfig : fieldConfigs) {
      Field field = fieldConfig.getField();
      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName(context.getNamer().getVariableName(field));
      paramDoc.typeName(context.getTypeTable().getAndSaveNicknameFor(field.getType()));

      List<String> docLines = null;
      GapicMethodConfig methodConfig = context.getMethodConfig();
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

  public List<SimpleParamDocView> getRequestObjectParamDocs(
      GapicMethodContext context, TypeRef typeRef) {
    SimpleParamDocView doc =
        SimpleParamDocView.newBuilder()
            .paramName("request")
            .typeName(context.getTypeTable().getAndSaveNicknameFor(typeRef))
            .lines(
                Arrays.<String>asList(
                    "The request object containing all of the parameters for the API call."))
            .build();
    return ImmutableList.of(doc);
  }

  private InitCodeContext createInitCodeContext(
      GapicMethodContext context,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethod().getInputType())
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldIterable(fieldConfigs))
        .outputType(initCodeOutputType)
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .build();
  }
}
