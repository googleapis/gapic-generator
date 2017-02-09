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
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.MapParamDocView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DynamicLangApiMethodTransformer generates view objects from method definitions for dynamic
 * languages.
 */
public class DynamicLangApiMethodTransformer {
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final LongRunningTransformer lroTransformer = new LongRunningTransformer();

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

    apiMethod.methodParams(generateMethodParams(context));

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
    List<ParamDocView> paramDocs =
        getMethodParamDocs(context, context.getMethodConfig().getRequiredFieldConfigs());
    paramDocs.add(
        getOptionalArrayParamDoc(context, context.getMethodConfig().getOptionalFieldConfigs()));
    docBuilder.paramDocs(paramDocs);
    docBuilder.returnTypeName(
        context
            .getNamer()
            .getDynamicLangReturnTypeName(context.getMethod(), context.getMethodConfig()));
    docBuilder.throwsDocLines(new ArrayList<String>());

    return docBuilder.build();
  }

  private List<DynamicLangDefaultableParamView> generateMethodParams(
      MethodTransformerContext context) {
    List<DynamicLangDefaultableParamView> methodParams =
        generateDefaultableParams(context, context.getMethodConfig().getRequiredFields());

    // TODO create a map TypeRef here instead of an array
    // (not done yet because array is sufficient for PHP, and maps are more complex to construct)
    TypeRef arrayType = TypeRef.fromPrimitiveName("string").makeRepeated();

    DynamicLangDefaultableParamView.Builder optionalArgs =
        DynamicLangDefaultableParamView.newBuilder();
    optionalArgs.name(context.getNamer().localVarName(Name.from("optional", "args")));
    optionalArgs.defaultValue(context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType));
    methodParams.add(optionalArgs.build());

    return methodParams;
  }

  private List<DynamicLangDefaultableParamView> generateDefaultableParams(
      MethodTransformerContext context, Iterable<Field> fields) {
    List<DynamicLangDefaultableParamView> methodParams = new ArrayList<>();
    if (!context.getMethod().getRequestStreaming()) {
      for (Field field : context.getMethodConfig().getRequiredFields()) {
        DynamicLangDefaultableParamView param =
            DynamicLangDefaultableParamView.newBuilder()
                .name(context.getNamer().getVariableName(field))
                .defaultValue("")
                .build();
        methodParams.add(param);
      }
    }
    return methodParams;
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

  private List<ParamDocView> getMethodParamDocs(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    List<ParamDocView> allDocs = new ArrayList<>();
    if (context.getMethod().getRequestStreaming()) {
      return allDocs;
    }
    for (FieldConfig fieldConfig : fieldConfigs) {
      Field field = fieldConfig.getField();
      SimpleParamDocView.Builder paramDoc = SimpleParamDocView.newBuilder();
      paramDoc.paramName(context.getNamer().getVariableName(field));
      paramDoc.typeName(context.getTypeTable().getAndSaveNicknameFor(field.getType()));

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
    return allDocs;
  }

  private ParamDocView getOptionalArrayParamDoc(
      MethodTransformerContext context, Iterable<FieldConfig> fieldConfigs) {
    MapParamDocView.Builder paramDoc = MapParamDocView.newBuilder();

    Name optionalArgsName = Name.from("optional", "args");

    paramDoc.paramName(context.getNamer().localVarName(optionalArgsName));
    paramDoc.typeName(context.getNamer().getOptionalArrayTypeName());

    List<String> docLines = Arrays.asList("Optional.");

    paramDoc.firstLine(docLines.get(0));
    paramDoc.remainingLines(docLines.subList(1, docLines.size()));

    paramDoc.arrayKeyDocs(
        ImmutableList.<ParamDocView>builder()
            .addAll(getMethodParamDocs(context, fieldConfigs))
            .addAll(getCallSettingsParamDocList(context))
            .build());

    return paramDoc.build();
  }

  private List<ParamDocView> getCallSettingsParamDocList(MethodTransformerContext context) {
    List<ParamDocView> arrayKeyDocs = new ArrayList<>();
    SimpleParamDocView.Builder retrySettingsDoc = SimpleParamDocView.newBuilder();
    retrySettingsDoc.typeName(context.getNamer().getRetrySettingsTypeName());

    Name retrySettingsName = Name.from("retry", "settings");
    Name timeoutMillisName = Name.from("timeout", "millis");

    if (context.getNamer().methodHasRetrySettings(context.getMethodConfig())) {
      retrySettingsDoc.paramName(context.getNamer().localVarName(retrySettingsName));
      // TODO figure out a reliable way to line-wrap comments across all languages
      // instead of encoding it in the transformer
      String retrySettingsDocText =
          String.format(
              "Retry settings to use for this call. If present, then\n%s is ignored.",
              context.getNamer().varReference(timeoutMillisName));
      List<String> retrySettingsDocLines = context.getNamer().getDocLines(retrySettingsDocText);
      retrySettingsDoc.lines(retrySettingsDocLines);
      arrayKeyDocs.add(retrySettingsDoc.build());
    }

    if (context.getNamer().methodHasTimeoutSettings(context.getMethodConfig())) {
      SimpleParamDocView.Builder timeoutDoc = SimpleParamDocView.newBuilder();
      timeoutDoc.typeName(
          context.getTypeTable().getAndSaveNicknameFor(TypeRef.of(Type.TYPE_INT32)));
      timeoutDoc.paramName(context.getNamer().localVarName(timeoutMillisName));
      // TODO figure out a reliable way to line-wrap comments across all languages
      // instead of encoding it in the transformer
      String timeoutMillisDocText = "Timeout to use for this call.";
      if (context.getNamer().methodHasRetrySettings(context.getMethodConfig())) {
        timeoutMillisDocText +=
            String.format(
                " Only used if %s\nis not set.",
                context.getNamer().varReference(retrySettingsName));
      }
      List<String> timeoutMillisDocLines = context.getNamer().getDocLines(timeoutMillisDocText);
      timeoutDoc.lines(timeoutMillisDocLines);
      arrayKeyDocs.add(timeoutDoc.build());
    }

    return arrayKeyDocs;
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
