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
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodContext;
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
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * DynamicLangApiMethodTransformer generates view objects from method definitions for dynamic
 * languages.
 */
public class DynamicLangApiMethodTransformer {
  private final ApiMethodParamTransformer apiMethodParamTransformer;
  private final LongRunningTransformer lroTransformer = new LongRunningTransformer();
  private final HeaderRequestParamTransformer headerRequestParamTransformer =
      new HeaderRequestParamTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final SampleTransformer sampleTransformer;

  public DynamicLangApiMethodTransformer(ApiMethodParamTransformer apiMethodParamTransformer) {
    this(apiMethodParamTransformer, SampleTransformer.create(SampleType.IN_CODE));
  }

  public DynamicLangApiMethodTransformer(
      ApiMethodParamTransformer apiMethodParamTransformer, SampleTransformer sampleTransformer) {
    this.apiMethodParamTransformer = apiMethodParamTransformer;
    this.sampleTransformer = sampleTransformer;
  }

  /** Generates method views for all methods in an interface. */
  public List<OptionalArrayMethodView> generateApiMethods(InterfaceContext context) {
    return Streams.stream(context.getSupportedMethods())
        .map(m -> generateApiMethod(context.asRequestMethodContext(m)))
        .collect(Collectors.toList());
  }

  /** Generate the method view for an RPC method. */
  public OptionalArrayMethodView generateApiMethod(MethodContext context) {
    return generateApiMethod(
        context,
        null,
        context.getSurfaceInterfaceContext().getApiModel().hasMultipleServices(),
        context.getNamer().getCallingForms(context));
  }

  private OptionalArrayMethodView generateApiMethod(
      MethodContext methodContext,
      @Nullable InitCodeContext initCodeContext,
      boolean packageHasMultipleServices,
      List<CallingForm> callingForms) {
    if (methodContext.getMethodConfig().isPageStreaming()) {
      return generatePagedStreamingMethod(
          (GapicMethodContext) methodContext,
          initCodeContext,
          packageHasMultipleServices,
          callingForms);
    }
    if (methodContext.isLongRunningMethodContext()) {
      return generateLongRunningMethod(
          (GapicMethodContext) methodContext,
          initCodeContext,
          packageHasMultipleServices,
          callingForms);
    }
    return generateRequestMethod(
        (GapicMethodContext) methodContext,
        initCodeContext,
        packageHasMultipleServices,
        callingForms);
  }

  // TODO: After we migrate Node.js and PHP to use the public `generateApiMethod` and
  // `generateApiMethods`, we can remove PhpMethodViewGenerator and NodejsMethodViewGenerator
  // and make this method and the following two private.
  public OptionalArrayMethodView generateRequestMethod(
      GapicMethodContext context,
      InitCodeContext initContext,
      boolean packageHasMultipleServices,
      List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    OptionalArrayMethodView.Builder apiMethod = OptionalArrayMethodView.newBuilder();

    apiMethod.type(ClientMethodType.OptionalArrayMethod);
    generateMethodCommon(
        context, initContext, packageHasMultipleServices, method, apiMethod, callingForms);

    return apiMethod.build();
  }

  public OptionalArrayMethodView generateLongRunningMethod(
      GapicMethodContext context,
      InitCodeContext initContext,
      boolean packageHasMultipleServices,
      List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    OptionalArrayMethodView.Builder apiMethod = OptionalArrayMethodView.newBuilder();

    apiMethod.longRunningView(lroTransformer.generateDetailView(context));
    apiMethod.type(ClientMethodType.LongRunningOptionalArrayMethod);

    generateMethodCommon(
        context, initContext, packageHasMultipleServices, method, apiMethod, callingForms);

    return apiMethod.build();
  }

  public OptionalArrayMethodView generatePagedStreamingMethod(
      GapicMethodContext context,
      InitCodeContext initContext,
      boolean packageHasMultipleServices,
      List<CallingForm> callingForms) {
    MethodModel method = context.getMethodModel();
    OptionalArrayMethodView.Builder apiMethod = OptionalArrayMethodView.newBuilder();

    apiMethod.type(ClientMethodType.PagedOptionalArrayMethod);
    apiMethod.pageStreamingView(
        pageStreamingTransformer.generateDescriptor(context.getSurfaceInterfaceContext(), method));

    generateMethodCommon(
        context, initContext, packageHasMultipleServices, method, apiMethod, callingForms);

    return apiMethod.build();
  }

  private void generateMethodCommon(
      GapicMethodContext context,
      InitCodeContext initContext,
      boolean packageHasMultipleServices,
      MethodModel method,
      OptionalArrayMethodView.Builder apiMethod,
      List<CallingForm> callingForms) {
    SurfaceNamer namer = context.getNamer();

    apiMethod.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    apiMethod.topLevelAliasedApiClassName(
        namer.getTopLevelAliasedApiClassName(
            (context.getInterfaceConfig()), packageHasMultipleServices));
    apiMethod.versionAliasedApiClassName(
        namer.getVersionAliasedApiClassName(
            (context.getInterfaceConfig()), packageHasMultipleServices));
    apiMethod.apiVariableName(namer.getApiWrapperVariableName(context.getInterfaceConfig()));
    apiMethod.apiModuleName(namer.getApiWrapperModuleName());
    apiMethod.localPackageName(namer.getLocalPackageName());

    apiMethod.doc(generateMethodDoc(context));

    apiMethod.name(namer.getApiMethodName(method, context.getMethodConfig().getVisibility()));
    apiMethod.requestVariableName(namer.getRequestVariableName(method));
    apiMethod.requestTypeName(
        namer.getAndSaveTypeName(context.getTypeTable(), context.getMethodModel().getInputType()));
    apiMethod.responseTypeName(
        namer.getAndSaveTypeName(context.getTypeTable(), context.getMethodModel().getOutputType()));
    apiMethod.hasReturnValue(!ServiceMessages.s_isEmptyType(context.getMethod().getOutputType()));
    apiMethod.key(namer.getMethodKey(method));
    apiMethod.grpcMethodName(namer.getGrpcMethodName(method));
    apiMethod.rerouteToGrpcInterface(context.getMethodConfig().getRerouteToGrpcInterface());
    apiMethod.stubName(namer.getStubName(context.getTargetInterface()));

    apiMethod.methodParams(apiMethodParamTransformer.generateMethodParams(context));

    Iterable<FieldConfig> filteredFieldConfigs =
        removePageTokenFieldConfig(context, context.getMethodConfig().getOptionalFieldConfigs());
    List<RequestObjectParamView> requiredParams =
        generateRequestObjectParams(context, context.getMethodConfig().getRequiredFieldConfigs());
    List<RequestObjectParamView> optionalParams =
        generateRequestObjectParams(context, context.getMethodConfig().getOptionalFieldConfigs());
    List<RequestObjectParamView> optionalParamsNoPageToken =
        generateRequestObjectParams(context, filteredFieldConfigs);
    apiMethod.requiredRequestObjectParams(requiredParams);
    apiMethod.optionalRequestObjectParams(optionalParams);
    apiMethod.optionalRequestObjectParamsNoPageToken(optionalParamsNoPageToken);
    apiMethod.hasRequestParameters(
        !requiredParams.isEmpty() || !optionalParamsNoPageToken.isEmpty());

    GrpcStreamingType grpcStreamingType = context.getMethodConfig().getGrpcStreamingType();
    apiMethod.grpcStreamingType(grpcStreamingType);
    apiMethod.isSingularRequestMethod(
        grpcStreamingType.equals(GrpcStreamingType.NonStreaming)
            || grpcStreamingType.equals(GrpcStreamingType.ServerStreaming));

    apiMethod.packageName(namer.getPackageName());
    apiMethod.apiVersion(namer.getApiWrapperModuleVersion());

    apiMethod.oneofParams(context.getMethodConfig().getOneofNames(namer));
    apiMethod.headerRequestParams(
        headerRequestParamTransformer.generateHeaderRequestParams(context));

    InitCodeOutputType initCodeOutputType =
        context.getMethodModel().getRequestStreaming()
            ? InitCodeOutputType.SingleObject
            : InitCodeOutputType.FieldList;
    sampleTransformer.generateSamples(
        apiMethod,
        context,
        initContext,
        context.getMethodConfig().getRequiredFieldConfigs(),
        initCodeOutputType,
        callingForms);
  }

  private ApiMethodDocView generateMethodDoc(GapicMethodContext context) {
    ApiMethodDocView.Builder docBuilder = ApiMethodDocView.newBuilder();
    SurfaceNamer surfaceNamer = context.getNamer();
    MethodModel method = context.getMethodModel();
    MethodConfig methodConfig = context.getMethodConfig();

    docBuilder.mainDocLines(surfaceNamer.getDocLines(method, methodConfig));
    docBuilder.paramDocs(apiMethodParamTransformer.generateParamDocs(context));
    docBuilder.returnTypeName(surfaceNamer.getDynamicLangReturnTypeName(context));
    docBuilder.returnsDocLines(
        surfaceNamer.getReturnDocLines(
            context.getSurfaceInterfaceContext(), context, Synchronicity.Sync));
    if (methodConfig.isPageStreaming()) {
      docBuilder.pageStreamingResourceTypeName(
          surfaceNamer.getTypeNameDoc(
              context.getTypeTable(),
              methodConfig.getPageStreaming().getResourcesField().getType()));
    }
    docBuilder.throwsDocLines(surfaceNamer.getThrowsDocLines(methodConfig));

    return docBuilder.build();
  }

  private List<RequestObjectParamView> generateRequestObjectParams(
      GapicMethodContext context, Iterable<FieldConfig> fieldConfigs) {
    List<RequestObjectParamView> params = new ArrayList<>();
    for (FieldConfig fieldConfig : fieldConfigs) {
      params.add(generateRequestObjectParam(context, fieldConfig));
    }
    return params;
  }

  private Iterable<FieldConfig> removePageTokenFieldConfig(
      GapicMethodContext context, Iterable<FieldConfig> fieldConfigs) {
    MethodConfig methodConfig = context.getMethodConfig();
    if (methodConfig == null || !methodConfig.isPageStreaming()) {
      return fieldConfigs;
    }
    final FieldModel requestTokenField = methodConfig.getPageStreaming().getRequestTokenField();
    return Iterables.filter(
        fieldConfigs,
        new Predicate<FieldConfig>() {
          @Override
          public boolean apply(FieldConfig fieldConfig) {
            return !fieldConfig.getField().equals(requestTokenField);
          }
        });
  }

  private RequestObjectParamView generateRequestObjectParam(
      GapicMethodContext context, FieldConfig fieldConfig) {
    SurfaceNamer namer = context.getNamer();
    FeatureConfig featureConfig = context.getFeatureConfig();
    ModelTypeTable typeTable = context.getTypeTable();
    FieldModel field = fieldConfig.getField();

    Iterable<FieldModel> requiredFields = context.getMethodConfig().getRequiredFields();
    boolean isRequired = false;
    for (FieldModel f : requiredFields) {
      if (f.getSimpleName().equals(field.getSimpleName())) {
        isRequired = true;
      }
    }

    RequestObjectParamView.Builder param = RequestObjectParamView.newBuilder();
    param.name(namer.getVariableName(field));
    param.keyName(namer.getFieldKey(field));
    param.nameAsMethodName(namer.getFieldGetFunctionName(featureConfig, fieldConfig));
    param.typeName(typeTable.getAndSaveNicknameFor(field));
    param.elementTypeName(typeTable.getAndSaveNicknameForElementType(field));
    param.setCallName(namer.getFieldSetFunctionName(featureConfig, fieldConfig));
    param.getCallName(namer.getFieldGetFunctionName(featureConfig, fieldConfig));
    param.isMap(field.isMap());
    param.isArray(!field.isMap() && field.isRepeated());
    param.isPrimitive(field.isPrimitive());
    param.isOptional(!isRequired);
    if (!isRequired) {
      param.optionalDefault(namer.getOptionalFieldDefaultValue(fieldConfig, context));
    }
    return param.build();
  }
}
