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

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiMethodDocView;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.CallableMethodDetailView;
import com.google.api.codegen.viewmodel.DynamicLangDefaultableParamView;
import com.google.api.codegen.viewmodel.ListMethodDetailView;
import com.google.api.codegen.viewmodel.MapParamDocView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
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
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** ApiMethodTransformer generates view objects from method definitions. */
public class ApiMethodTransformer {
  private InitCodeTransformer initCodeTransformer;

  public ApiMethodTransformer() {
    this.initCodeTransformer = new InitCodeTransformer();
  }

  public StaticLangApiMethodView generatePagedFlattenedMethod(
      MethodTransformerContext context, ImmutableList<Field> fields) {
    return generatePagedFlattenedMethod(context, fields, ApiMethodType.PagedFlattenedMethod);
  }

  public StaticLangApiMethodView generatePagedFlattenedMethod(
      MethodTransformerContext context, ImmutableList<Field> fields, ApiMethodType type) {
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(context.getNamer().getApiMethodName(context.getMethod()));
    methodViewBuilder.callableName(context.getNamer().getCallableName(context.getMethod()));
    setListMethodFields(context, methodViewBuilder);
    setFlattenedMethodFields(context, fields, methodViewBuilder);

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generatePagedRequestObjectMethod(
      MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getApiMethodName(context.getMethod()));
    setListMethodFields(context, methodViewBuilder);
    setRequestObjectMethodFields(
        context, namer.getPagedCallableMethodName(context.getMethod()), methodViewBuilder);

    return methodViewBuilder.type(ApiMethodType.PagedRequestObjectMethod).build();
  }

  public StaticLangApiMethodView generatePagedCallableMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getPagedCallableMethodName(context.getMethod()));
    setListMethodFields(context, methodViewBuilder);
    setCallableMethodFields(
        context, namer.getPagedCallableName(context.getMethod()), methodViewBuilder);

    return methodViewBuilder.type(ApiMethodType.PagedCallableMethod).build();
  }

  public StaticLangApiMethodView generateUnpagedListCallableMethod(
      MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(context.getMethod()));
    setListMethodFields(context, methodViewBuilder);
    setCallableMethodFields(context, namer.getCallableName(context.getMethod()), methodViewBuilder);

    String getResourceListCallName =
        namer.getGetResourceListCallName(
            context.getMethodConfig().getPageStreaming().getResourcesField());
    UnpagedListCallableMethodDetailView unpagedListCallableDetails =
        UnpagedListCallableMethodDetailView.newBuilder()
            .resourceListGetFunction(getResourceListCallName)
            .build();
    methodViewBuilder.unpagedListCallableMethod(unpagedListCallableDetails);

    methodViewBuilder.responseTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType()));

    return methodViewBuilder.type(ApiMethodType.UnpagedListCallableMethod).build();
  }

  public StaticLangApiMethodView generateFlattenedMethod(
      MethodTransformerContext context, ImmutableList<Field> fields) {
    return generateFlattenedMethod(context, fields, ApiMethodType.FlattenedMethod);
  }
  
  public StaticLangApiMethodView generateFlattenedMethod(
      MethodTransformerContext context, ImmutableList<Field> fields, ApiMethodType type) {
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(context.getNamer().getApiMethodName(context.getMethod()));
    methodViewBuilder.callableName(context.getNamer().getCallableName(context.getMethod()));
    setFlattenedMethodFields(context, fields, methodViewBuilder);
    setStaticLangSyncReturnFields(context, methodViewBuilder);

    return methodViewBuilder.type(type).build();
  }

  public StaticLangApiMethodView generateRequestObjectMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getApiMethodName(context.getMethod()));
    setRequestObjectMethodFields(
        context, namer.getCallableMethodName(context.getMethod()), methodViewBuilder);
    setStaticLangSyncReturnFields(context, methodViewBuilder);

    return methodViewBuilder.type(ApiMethodType.RequestObjectMethod).build();
  }

  public StaticLangApiMethodView generateCallableMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangApiMethodView.Builder methodViewBuilder = StaticLangApiMethodView.newBuilder();

    setCommonFields(context, methodViewBuilder);
    methodViewBuilder.name(namer.getCallableMethodName(context.getMethod()));
    setCallableMethodFields(context, namer.getCallableName(context.getMethod()), methodViewBuilder);
    methodViewBuilder.responseTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getOutputType()));
    methodViewBuilder.hasReturnValue(
        !ServiceMessages.s_isEmptyType(context.getMethod().getOutputType()));

    return methodViewBuilder.type(ApiMethodType.CallableMethod).build();
  }

  private void setCommonFields(
      MethodTransformerContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    methodViewBuilder.apiRequestTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getInputType()));
    methodViewBuilder.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    methodViewBuilder.apiVariableName(namer.getApiWrapperVariableName(context.getInterface()));
    methodViewBuilder.settingsGetterName(namer.getSettingsFunctionName(context.getMethod()));
  }

  private void setListMethodFields(
      MethodTransformerContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    TypeRef resourceType = pageStreaming.getResourcesField().getType();
    String resourceTypeName = context.getTypeTable().getAndSaveNicknameForElementType(resourceType);
    methodViewBuilder.listMethod(
        ListMethodDetailView.newBuilder().resourceTypeName(resourceTypeName).build());
    methodViewBuilder.responseTypeName(
        context
            .getNamer()
            .getAndSavePagedResponseTypeName(
                context.getTypeTable(),
                context.getMethod().getInputType(),
                context.getMethod().getOutputType(),
                resourceType));
    methodViewBuilder.hasReturnValue(true);
  }

  private void setFlattenedMethodFields(
      MethodTransformerContext context,
      ImmutableList<Field> fields,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    methodViewBuilder.initCode(
        initCodeTransformer.generateInitCode(context.cloneWithEmptyTypeTable(), fields));
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(context.getMethod()))
            .paramDocs(getMethodParamDocs(context, fields))
            .throwsDocLines(namer.getThrowsDocLines())
            .build());

    List<RequestObjectParamView> params = new ArrayList<>();
    for (Field field : fields) {
      params.add(generateRequestObjectParam(context, field));
    }
    methodViewBuilder.methodParams(params).methodParamsWithExtras(params);
    methodViewBuilder.requestObjectParams(params);

    methodViewBuilder.pathTemplateChecks(generatePathTemplateChecks(context, fields));
  }

  private void setRequestObjectMethodFields(
      MethodTransformerContext context,
      String callableMethodName,
      StaticLangApiMethodView.Builder methodViewBuilder) {
    SurfaceNamer namer = context.getNamer();
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(namer.getDocLines(context.getMethod()))
            .paramDocs(
                Arrays.<ParamDocView>asList(
                    getRequestObjectParamDoc(context, context.getMethod().getInputType())))
            .throwsDocLines(namer.getThrowsDocLines())
            .build());
    methodViewBuilder.initCode(
        initCodeTransformer.generateRequestObjectInitCode(context.cloneWithEmptyTypeTable()));

    methodViewBuilder.methodParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.requestObjectParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<PathTemplateCheckView>());

    RequestObjectMethodDetailView.Builder detailBuilder =
        RequestObjectMethodDetailView.newBuilder();
    if (context.getMethodConfig().hasRequestObjectMethod()) {
      detailBuilder.accessModifier(context.getNamer().getPublicAccessModifier());
    } else {
      detailBuilder.accessModifier(context.getNamer().getPrivateAccessModifier());
    }
    detailBuilder.callableMethodName(callableMethodName);
    methodViewBuilder.requestObjectMethod(detailBuilder.build());
  }

  private void setCallableMethodFields(
      MethodTransformerContext context, String callableName, Builder methodViewBuilder) {
    methodViewBuilder.doc(
        ApiMethodDocView.newBuilder()
            .mainDocLines(context.getNamer().getDocLines(context.getMethod()))
            .paramDocs(new ArrayList<ParamDocView>())
            .throwsDocLines(new ArrayList<String>())
            .build());
    methodViewBuilder.initCode(
        initCodeTransformer.generateRequestObjectInitCode(context.cloneWithEmptyTypeTable()));

    methodViewBuilder.methodParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.requestObjectParams(new ArrayList<RequestObjectParamView>());
    methodViewBuilder.pathTemplateChecks(new ArrayList<PathTemplateCheckView>());

    String genericAwareResponseTypeFullName =
        context.getNamer().getGenericAwareResponseTypeName(context.getMethod().getOutputType());
    String genericAwareResponseType =
        context.getTypeTable().getAndSaveNicknameFor(genericAwareResponseTypeFullName);
    methodViewBuilder.callableMethod(
        CallableMethodDetailView.newBuilder()
            .genericAwareResponseType(genericAwareResponseType)
            .callableName(callableName)
            .build());
  }

  private void setStaticLangSyncReturnFields(
      MethodTransformerContext context, StaticLangApiMethodView.Builder methodViewBuilder) {
    String returnTypeFullName =
        context
            .getNamer()
            .getStaticLangReturnTypeName(context.getMethod(), context.getMethodConfig());
    methodViewBuilder.responseTypeName(
        context.getTypeTable().getAndSaveNicknameFor(returnTypeFullName));
    methodViewBuilder.hasReturnValue(
        !ServiceMessages.s_isEmptyType(context.getMethod().getOutputType()));
  }

  private List<PathTemplateCheckView> generatePathTemplateChecks(
      MethodTransformerContext context, ImmutableList<Field> fields) {
    List<PathTemplateCheckView> pathTemplateChecks = new ArrayList<>();
    for (Field field : fields) {
      ImmutableMap<String, String> fieldNamePatterns =
          context.getMethodConfig().getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        CollectionConfig collectionConfig = context.getCollectionConfig(entityName);
        if (collectionConfig == null) {
          throw new IllegalStateException("No collection config with id '" + entityName + "'");
        }
        PathTemplateCheckView.Builder check = PathTemplateCheckView.newBuilder();
        check.pathTemplateName(context.getNamer().getPathTemplateName(collectionConfig));
        check.paramName(context.getNamer().getVariableName(field));
        check.allowEmptyString(shouldAllowEmpty(context, field));
        check.validationMessageContext(context.getNamer().getApiMethodName(context.getMethod()));
        pathTemplateChecks.add(check.build());
      }
    }
    return pathTemplateChecks;
  }

  private boolean shouldAllowEmpty(MethodTransformerContext context, Field field) {
    for (Field requiredField : context.getMethodConfig().getRequiredFields()) {
      if (requiredField.equals(field)) {
        return false;
      }
    }
    return true;
  }

  public OptionalArrayMethodView generateDynamicLangApiMethod(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    OptionalArrayMethodView.Builder apiMethod = OptionalArrayMethodView.newBuilder();

    if (context.getMethodConfig().isPageStreaming()) {
      apiMethod.type(ApiMethodType.PagedOptionalArrayMethod);
    } else {
      apiMethod.type(ApiMethodType.OptionalArrayMethod);
    }
    apiMethod.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    apiMethod.apiVariableName(namer.getApiWrapperVariableName(context.getInterface()));
    apiMethod.apiModuleName(namer.getApiWrapperModuleName(context.getInterface()));
    apiMethod.initCode(
        initCodeTransformer.generateInitCode(
            context.cloneWithEmptyTypeTable(), context.getMethodConfig().getRequiredFields()));

    apiMethod.doc(generateOptionalArrayMethodDoc(context));

    apiMethod.name(namer.getApiMethodName(context.getMethod()));
    apiMethod.requestTypeName(
        context.getTypeTable().getAndSaveNicknameFor(context.getMethod().getInputType()));
    apiMethod.hasReturnValue(!ServiceMessages.s_isEmptyType(context.getMethod().getOutputType()));
    apiMethod.key(namer.getMethodKey(context.getMethod()));
    apiMethod.grpcMethodName(namer.getGrpcMethodName(context.getMethod()));
    apiMethod.stubName(namer.getStubName(context.getTargetInterface()));

    apiMethod.methodParams(generateOptionalArrayMethodParams(context));

    apiMethod.requiredRequestObjectParams(
        generateRequestObjectParams(context, context.getMethodConfig().getRequiredFields()));
    apiMethod.optionalRequestObjectParams(
        generateRequestObjectParams(context, context.getMethodConfig().getOptionalFields()));

    return apiMethod.build();
  }

  private ApiMethodDocView generateOptionalArrayMethodDoc(MethodTransformerContext context) {
    ApiMethodDocView.Builder docBuilder = ApiMethodDocView.newBuilder();

    docBuilder.mainDocLines(context.getNamer().getDocLines(context.getMethod()));
    List<ParamDocView> paramDocs =
        getMethodParamDocs(context, context.getMethodConfig().getRequiredFields());
    paramDocs.add(getOptionalArrayParamDoc(context, context.getMethodConfig().getOptionalFields()));
    docBuilder.paramDocs(paramDocs);
    docBuilder.returnTypeName(
        context
            .getNamer()
            .getDynamicLangReturnTypeName(context.getMethod(), context.getMethodConfig()));
    docBuilder.throwsDocLines(new ArrayList<String>());

    return docBuilder.build();
  }

  private List<DynamicLangDefaultableParamView> generateOptionalArrayMethodParams(
      MethodTransformerContext context) {
    List<DynamicLangDefaultableParamView> methodParams =
        generateDefaultableParams(context, context.getMethodConfig().getRequiredFields());

    // TODO create a map TypeRef here instead of an array
    // (not done yet because array is sufficient for PHP, and maps are more complex to construct)
    TypeRef arrayType = TypeRef.fromPrimitiveName("string").makeRepeated();

    DynamicLangDefaultableParamView.Builder optionalArgs =
        DynamicLangDefaultableParamView.newBuilder();
    optionalArgs.name(context.getNamer().varName(Name.from("optional", "args")));
    optionalArgs.defaultValue(context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType));
    methodParams.add(optionalArgs.build());

    return methodParams;
  }

  private List<DynamicLangDefaultableParamView> generateDefaultableParams(
      MethodTransformerContext context, Iterable<Field> fields) {
    List<DynamicLangDefaultableParamView> methodParams = new ArrayList<>();
    for (Field field : context.getMethodConfig().getRequiredFields()) {
      DynamicLangDefaultableParamView param =
          DynamicLangDefaultableParamView.newBuilder()
              .name(context.getNamer().getVariableName(field))
              .defaultValue("")
              .build();
      methodParams.add(param);
    }
    return methodParams;
  }

  private List<RequestObjectParamView> generateRequestObjectParams(
      MethodTransformerContext context, Iterable<Field> fields) {
    List<RequestObjectParamView> params = new ArrayList<>();
    for (Field field : fields) {
      params.add(generateRequestObjectParam(context, field));
    }
    return params;
  }

  private RequestObjectParamView generateRequestObjectParam(
      MethodTransformerContext context, Field field) {
    SurfaceNamer namer = context.getNamer();
    RequestObjectParamView.Builder param = RequestObjectParamView.newBuilder();
    param.name(namer.getVariableName(field));
    param.nameAsMethodName(namer.methodName(Name.lowerCamel(namer.getVariableName(field))));

    if (namer.shouldImportRequestObjectParamType(field)) {
      param.typeName(context.getTypeTable().getAndSaveNicknameFor(field.getType()));
    } else {
      param.typeName(
          namer.getNotImplementedString(
              "ApiMethodTransformer.generateRequestObjectParam - typeName"));
    }

    if (namer.shouldImportRequestObjectParamElementType(field)) {
      param.elementTypeName(
          context.getTypeTable().getAndSaveNicknameForElementType(field.getType()));
    } else {
      param.elementTypeName(
          namer.getNotImplementedString(
              "ApiMethodTransformer.generateRequestObjectParam - elementTypeName"));
    }

    param.setCallName(namer.getFieldSetFunctionName(field));
    param.isMap(field.getType().isMap());
    param.isArray(!field.getType().isMap() && field.getType().isRepeated());
    return param.build();
  }

  private List<ParamDocView> getMethodParamDocs(
      MethodTransformerContext context, Iterable<Field> fields) {
    List<ParamDocView> allDocs = new ArrayList<>();
    for (Field field : fields) {
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
      paramDoc.firstLine(docLines.get(0));
      paramDoc.remainingLines(docLines.subList(1, docLines.size()));

      allDocs.add(paramDoc.build());
    }
    return allDocs;
  }

  public SimpleParamDocView getRequestObjectParamDoc(
      MethodTransformerContext context, TypeRef typeRef) {
    return SimpleParamDocView.newBuilder()
        .paramName("request")
        .typeName(context.getTypeTable().getAndSaveNicknameFor(typeRef))
        .firstLine("The request object containing all of the parameters for the API call.")
        .remainingLines(Arrays.<String>asList())
        .build();
  }

  private ParamDocView getOptionalArrayParamDoc(
      MethodTransformerContext context, Iterable<Field> fields) {
    MapParamDocView.Builder paramDoc = MapParamDocView.newBuilder();

    Name optionalArgsName = Name.from("optional", "args");

    paramDoc.paramName(context.getNamer().varName(optionalArgsName));
    paramDoc.typeName(context.getNamer().getOptionalArrayTypeName());

    List<String> docLines = Arrays.asList("Optional.");

    paramDoc.firstLine(docLines.get(0));
    paramDoc.remainingLines(docLines.subList(1, docLines.size()));

    paramDoc.arrayKeyDocs(
        ImmutableList.<ParamDocView>builder()
            .addAll(getMethodParamDocs(context, fields))
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

    retrySettingsDoc.paramName(context.getNamer().varName(retrySettingsName));
    // TODO figure out a reliable way to line-wrap comments across all languages
    // instead of encoding it in the transformer
    String retrySettingsDocText =
        String.format(
            "Retry settings to use for this call. If present, then\n%s is ignored.",
            context.getNamer().varReference(timeoutMillisName));
    List<String> retrySettingsDocLines = context.getNamer().getDocLines(retrySettingsDocText);
    retrySettingsDoc.firstLine(retrySettingsDocLines.get(0));
    retrySettingsDoc.remainingLines(retrySettingsDocLines.subList(1, retrySettingsDocLines.size()));
    arrayKeyDocs.add(retrySettingsDoc.build());

    SimpleParamDocView.Builder timeoutDoc = SimpleParamDocView.newBuilder();
    timeoutDoc.typeName(context.getTypeTable().getAndSaveNicknameFor(TypeRef.of(Type.TYPE_INT32)));
    timeoutDoc.paramName(context.getNamer().varName(timeoutMillisName));
    // TODO figure out a reliable way to line-wrap comments across all languages
    // instead of encoding it in the transformer
    String timeoutMillisDocText =
        String.format(
            "Timeout to use for this call. Only used if %s\nis not set.",
            context.getNamer().varReference(retrySettingsName));
    List<String> timeoutMillisDocLines = context.getNamer().getDocLines(timeoutMillisDocText);
    timeoutDoc.firstLine(timeoutMillisDocLines.get(0));
    timeoutDoc.remainingLines(timeoutMillisDocLines.subList(1, timeoutMillisDocLines.size()));
    arrayKeyDocs.add(timeoutDoc.build());

    return arrayKeyDocs;
  }
}
