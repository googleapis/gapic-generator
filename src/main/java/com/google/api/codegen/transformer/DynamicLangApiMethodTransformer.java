/* Copyright 2016 Google Inc
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
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.List;

/**
 * DynamicLangApiMethodTransformer generates view objects from method definitions in dynamic
 * languages.
 */
public class DynamicLangApiMethodTransformer {
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final LongRunningTransformer lroTransformer = new LongRunningTransformer();
  private final ParamDocTransformer paramDocTransformer;

  public DynamicLangApiMethodTransformer(ParamDocTransformer paramDocTransformer) {
    this.paramDocTransformer = paramDocTransformer;
  }

  public OptionalArrayMethodView generateApiMethod(MethodTransformerContext context) {
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

    apiMethod.doc(generateDoc(context));

    apiMethod.name(
        namer.getApiMethodName(context.getMethod(), context.getMethodConfig().getVisibility()));
    apiMethod.requestTypeName(
        namer.getRequestTypeName(context.getTypeTable(), context.getMethod().getInputType()));
    apiMethod.hasReturnValue(!ServiceMessages.s_isEmptyType(context.getMethod().getOutputType()));
    apiMethod.key(namer.getMethodKey(context.getMethod()));
    apiMethod.grpcMethodName(namer.getGrpcMethodName(context.getMethod()));
    apiMethod.stubName(namer.getStubName(context.getTargetInterface()));

    apiMethod.methodParams(generateMethodParams(context));

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
    apiMethod.hasRequiredParameters(!requiredParams.isEmpty());

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

  private ApiMethodDocView generateDoc(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    ApiMethodDocView.Builder doc = ApiMethodDocView.newBuilder();
    doc.mainDocLines(namer.getDocLines(context.getMethod(), context.getMethodConfig()));
    doc.paramDocs(paramDocTransformer.generateParamDocs(context));
    doc.returnTypeName(
        namer.getDynamicLangReturnTypeName(context.getMethod(), context.getMethodConfig()));
    doc.returnsDocLines(
        namer.getReturnDocLines(
            context.getSurfaceTransformerContext(), context.getMethodConfig(), Synchronicity.Sync));
    doc.throwsDocLines(namer.getThrowsDocLines());
    return doc.build();
  }

  private List<DynamicLangDefaultableParamView> generateMethodParams(
      MethodTransformerContext context) {
    ImmutableList.Builder<DynamicLangDefaultableParamView> methodParams = ImmutableList.builder();
    methodParams.addAll(generateDefaultableParams(context));

    // TODO create a map TypeRef here instead of an array
    // (not done yet because array is sufficient for PHP, and maps are more complex to construct)
    TypeRef arrayType = TypeRef.fromPrimitiveName("string").makeRepeated();

    DynamicLangDefaultableParamView.Builder optionalArgs =
        DynamicLangDefaultableParamView.newBuilder();
    optionalArgs.name(context.getNamer().localVarName(Name.from("optional", "args")));
    optionalArgs.defaultValue(context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType));
    methodParams.add(optionalArgs.build());

    return methodParams.build();
  }

  private List<DynamicLangDefaultableParamView> generateDefaultableParams(
      MethodTransformerContext context) {
    if (context.getMethod().getRequestStreaming()) {
      return ImmutableList.<DynamicLangDefaultableParamView>of();
    }

    ImmutableList.Builder<DynamicLangDefaultableParamView> methodParams = ImmutableList.builder();
    for (Field field : context.getMethodConfig().getRequiredFields()) {
      DynamicLangDefaultableParamView param =
          DynamicLangDefaultableParamView.newBuilder()
              .name(context.getNamer().getVariableName(field))
              .defaultValue("")
              .build();
      methodParams.add(param);
    }
    return methodParams.build();
  }

  private List<RequestObjectParamView> generateRequestObjectParams(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    ImmutableList.Builder<RequestObjectParamView> params = ImmutableList.builder();
    for (FieldConfig fieldConfig : fieldConfigs) {
      params.add(generateRequestObjectParam(context, fieldConfig));
    }
    return params.build();
  }

  private RequestObjectParamView generateRequestObjectParam(
      MethodTransformerContext context, FieldConfig fieldConfig) {
    SurfaceNamer namer = context.getNamer();
    FeatureConfig featureConfig = context.getFeatureConfig();
    ModelTypeTable typeTable = context.getTypeTable();
    Field field = fieldConfig.getField();

    String typeName =
        namer.getNotImplementedString(
            "DynamicLangApiMethodTransformer.generateRequestObjectParam - typeName");
    String elementTypeName =
        namer.getNotImplementedString(
            "DynamicLangApiMethodTransformer.generateRequestObjectParam - elementTypeName");

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
      }
      if (namer.shouldImportRequestObjectParamElementType(field)) {
        elementTypeName = typeTable.getAndSaveNicknameForElementType(field.getType());
      }
    }

    String setCallName = namer.getFieldSetFunctionName(featureConfig, fieldConfig);
    String addCallName = namer.getFieldAddFunctionName(field);
    String transformParamFunctionName = null;
    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)
        && fieldConfig.hasDifferentMessageResourceNameConfig()) {
      transformParamFunctionName = namer.getResourceOneofCreateMethod(typeTable, fieldConfig);
    }

    RequestObjectParamView.Builder param = RequestObjectParamView.newBuilder();
    param.name(namer.getVariableName(field));
    param.keyName(namer.getFieldKey(field));
    param.nameAsMethodName(namer.getFieldGetFunctionName(featureConfig, fieldConfig));
    param.typeName(typeName);
    param.elementTypeName(elementTypeName);
    param.setCallName(setCallName);
    param.addCallName(addCallName);
    param.transformParamFunctionName(transformParamFunctionName);
    param.isMap(field.getType().isMap());
    param.isArray(!field.getType().isMap() && field.getType().isRepeated());
    return param.build();
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
}
