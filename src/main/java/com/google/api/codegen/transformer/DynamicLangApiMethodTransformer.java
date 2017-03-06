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
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiMethodDocView;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.tools.framework.model.Field;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;

/**
 * DynamicLangApiMethodTransformer generates view objects from method definitions for dynamic
 * languages.
 */
public class DynamicLangApiMethodTransformer {
  private final ApiMethodParamTransformer apiMethodParamTransformer;
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final LongRunningTransformer lroTransformer = new LongRunningTransformer();

  public DynamicLangApiMethodTransformer(ApiMethodParamTransformer apiMethodParamTransformer) {
    this.apiMethodParamTransformer = apiMethodParamTransformer;
  }

  public OptionalArrayMethodView generateMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    OptionalArrayMethodView.Builder apiMethod = OptionalArrayMethodView.newBuilder();

    if (context.getMethodConfig().isPageStreaming()) {
      apiMethod.type(ClientMethodType.PagedOptionalArrayMethod);
    } else {
      apiMethod.type(ClientMethodType.OptionalArrayMethod);
    }
    apiMethod.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    apiMethod.apiVariableName(namer.getApiWrapperVariableName(context.getInterface()));
    apiMethod.apiModuleName(namer.getApiWrapperModuleName());
    InitCodeOutputType initCodeOutputType =
        context.getMethod().getRequestStreaming()
            ? InitCodeOutputType.SingleObject
            : InitCodeOutputType.FieldList;
    InitCodeView initCode =
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(),
            createInitCodeContext(
                context, context.getMethodConfig().getRequiredFieldConfigs(), initCodeOutputType));
    apiMethod.initCode(initCode);

    apiMethod.doc(generateMethodDoc(context));

    apiMethod.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    apiMethod.requestTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getInputType()));
    apiMethod.hasRequestParameters(initCode.lines().size() > 0);
    apiMethod.hasReturnValue(!ServiceMessages.s_isEmptyType(context.getMethod().getOutputType()));
    apiMethod.key(namer.getMethodKey(context.getMethod()));
    apiMethod.grpcMethodName(namer.getGrpcMethodName(context.getMethod()));
    apiMethod.stubName(namer.getStubName(context.getTargetInterface()));

    apiMethod.methodParams(apiMethodParamTransformer.generateMethodParams(context));

    apiMethod.requiredRequestObjectParams(
        generateRequestObjectParams(context, context.getMethodConfig().getRequiredFieldConfigs()));
    apiMethod.optionalRequestObjectParams(
        generateRequestObjectParams(context, context.getMethodConfig().getOptionalFieldConfigs()));
    Iterable<FieldConfig> filteredFieldConfigs =
        removePageTokenFieldConfig(context, context.getMethodConfig().getOptionalFieldConfigs());
    apiMethod.optionalRequestObjectParamsNoPageToken(
        generateRequestObjectParams(context, filteredFieldConfigs));

    GrpcStreamingType grpcStreamingType = context.getMethodConfig().getGrpcStreamingType();
    apiMethod.grpcStreamingType(grpcStreamingType);
    apiMethod.isSingularRequestMethod(
        grpcStreamingType.equals(GrpcStreamingType.NonStreaming)
            || grpcStreamingType.equals(GrpcStreamingType.ServerStreaming));

    apiMethod.longRunningView(
        context.getMethodConfig().isLongRunningOperation()
            ? lroTransformer.generateDetailView(context)
            : null);

    return apiMethod.build();
  }

  private ApiMethodDocView generateMethodDoc(MethodTransformerContext context) {
    ApiMethodDocView.Builder docBuilder = ApiMethodDocView.newBuilder();

    docBuilder.mainDocLines(
        context.getNamer().getDocLines(context.getMethod(), context.getMethodConfig()));
    docBuilder.paramDocs(apiMethodParamTransformer.generateParamDocs(context));
    docBuilder.returnTypeName(
        context
            .getNamer()
            .getDynamicLangReturnTypeName(context.getMethod(), context.getMethodConfig()));
    docBuilder.throwsDocLines(new ArrayList<String>());

    return docBuilder.build();
  }

  private List<RequestObjectParamView> generateRequestObjectParams(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    List<RequestObjectParamView> params = new ArrayList<>();
    for (FieldConfig fieldConfig : fieldConfigs) {
      params.add(generateRequestObjectParam(context, fieldConfig));
    }
    return params;
  }

  private Iterable<FieldConfig> removePageTokenFieldConfig(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    MethodConfig methodConfig = context.getMethodConfig();
    if (methodConfig == null || !methodConfig.isPageStreaming()) {
      return fieldConfigs;
    }
    final Field requestTokenField = methodConfig.getPageStreaming().getRequestTokenField();
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
      MethodTransformerContext context, FieldConfig fieldConfig) {
    SurfaceNamer namer = context.getNamer();
    FeatureConfig featureConfig = context.getFeatureConfig();
    ModelTypeTable typeTable = context.getTypeTable();
    Field field = fieldConfig.getField();

    RequestObjectParamView.Builder param = RequestObjectParamView.newBuilder();
    param.name(namer.getVariableName(field));
    param.nameAsMethodName(namer.getFieldGetFunctionName(featureConfig, fieldConfig));
    param.typeName(typeTable.getAndSaveNicknameFor(field.getType()));
    param.elementTypeName(typeTable.getAndSaveNicknameForElementType(field.getType()));
    param.setCallName(namer.getFieldSetFunctionName(featureConfig, fieldConfig));
    param.addCallName(namer.getFieldAddFunctionName(field));
    param.isMap(field.getType().isMap());
    param.isArray(!field.getType().isMap() && field.getType().isRepeated());
    return param.build();
  }

  private InitCodeContext createInitCodeContext(
      MethodTransformerContext context,
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
