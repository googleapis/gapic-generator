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
import com.google.api.codegen.viewmodel.CallableMethodView;
import com.google.api.codegen.viewmodel.DynamicDefaultableParamView;
import com.google.api.codegen.viewmodel.FlattenedMethodView;
import com.google.api.codegen.viewmodel.MapParamDocView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.PagedCallableMethodView;
import com.google.api.codegen.viewmodel.PagedFlattenedMethodView;
import com.google.api.codegen.viewmodel.PagedRequestObjectMethodView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.PathTemplateCheckView;
import com.google.api.codegen.viewmodel.RequestObjectMethodView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.api.codegen.viewmodel.UnpagedListCallableMethodView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ApiMethodTransformer {
  private InitCodeTransformer initCodeTransformer;

  public ApiMethodTransformer() {
    this.initCodeTransformer = new InitCodeTransformer();
  }

  public PagedFlattenedMethodView generatePagedFlattenedMethod(
      TransformerContext context,
      Method method,
      MethodConfig methodConfig,
      ImmutableList<Field> fields) {
    SurfaceNamer namer = context.getNamer();
    PagedFlattenedMethodView apiMethod = new PagedFlattenedMethodView();

    apiMethod.initCode =
        initCodeTransformer.generateInitCode(context, method, methodConfig, fields);

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs = getMethodParamDocs(context, methodConfig, fields);
    doc.throwsDocLines = namer.getThrowsDocLines();

    apiMethod.doc = doc;

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        context
            .getTypeTable()
            .getAndSaveNicknameForElementType(pageStreaming.getResourcesField().getType());
    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.name = namer.getApiMethodName(method);

    List<RequestObjectParamView> params = new ArrayList<>();
    for (Field field : fields) {
      params.add(generateRequestObjectParam(context, field));
    }
    apiMethod.methodParams = params;
    apiMethod.requestObjectParams = params;

    apiMethod.pathTemplateChecks = generatePathTemplateChecks(context, methodConfig, fields);

    apiMethod.apiClassName = namer.getApiWrapperClassName(context.getInterface());
    apiMethod.apiVariableName = namer.getApiWrapperVariableName(context.getInterface());

    return apiMethod;
  }

  public PagedRequestObjectMethodView generatePagedRequestObjectMethod(
      TransformerContext context, Method method, MethodConfig methodConfig) {
    SurfaceNamer namer = context.getNamer();
    PagedRequestObjectMethodView apiMethod = new PagedRequestObjectMethodView();

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs =
        Arrays.<ParamDocView>asList(getRequestObjectParamDoc(context, method.getInputType()));
    doc.throwsDocLines = namer.getThrowsDocLines();

    apiMethod.doc = doc;

    if (methodConfig.hasRequestObjectMethod()) {
      apiMethod.accessModifier = namer.getPublicAccessModifier();
    } else {
      apiMethod.accessModifier = namer.getPrivateAccessModifier();
    }

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        context
            .getTypeTable()
            .getAndSaveNicknameForElementType(pageStreaming.getResourcesField().getType());
    apiMethod.name = namer.getApiMethodName(method);
    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.callableMethodName = namer.getPagedCallableMethodName(method);

    return apiMethod;
  }

  public PagedCallableMethodView generatePagedCallableMethod(
      TransformerContext context, Method method, MethodConfig methodConfig) {
    SurfaceNamer namer = context.getNamer();
    PagedCallableMethodView apiMethod = new PagedCallableMethodView();

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs = new ArrayList<>();
    doc.throwsDocLines = new ArrayList<>();

    apiMethod.doc = doc;

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        context
            .getTypeTable()
            .getAndSaveNicknameForElementType(pageStreaming.getResourcesField().getType());
    apiMethod.name = namer.getPagedCallableMethodName(method);
    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.callableName = namer.getPagedCallableName(method);

    return apiMethod;
  }

  public UnpagedListCallableMethodView generateUnpagedListCallableMethod(
      TransformerContext context, Method method, MethodConfig methodConfig) {
    SurfaceNamer namer = context.getNamer();
    UnpagedListCallableMethodView apiMethod = new UnpagedListCallableMethodView();

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs = new ArrayList<>();
    doc.throwsDocLines = new ArrayList<>();

    apiMethod.doc = doc;

    apiMethod.name = namer.getCallableMethodName(method);
    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.responseTypeName =
        context.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    apiMethod.callableName = namer.getCallableName(method);

    return apiMethod;
  }

  public FlattenedMethodView generateFlattenedMethod(
      TransformerContext context,
      Method method,
      MethodConfig methodConfig,
      ImmutableList<Field> fields) {
    SurfaceNamer namer = context.getNamer();
    FlattenedMethodView apiMethod = new FlattenedMethodView();

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs = getMethodParamDocs(context, methodConfig, fields);
    doc.throwsDocLines = namer.getThrowsDocLines();

    apiMethod.doc = doc;

    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.responseTypeName =
        context.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    apiMethod.name = namer.getApiMethodName(method);

    List<RequestObjectParamView> params = new ArrayList<>();
    for (Field field : fields) {
      params.add(generateRequestObjectParam(context, field));
    }
    apiMethod.methodParams = params;
    apiMethod.requestObjectParams = params;

    apiMethod.pathTemplateChecks = generatePathTemplateChecks(context, methodConfig, fields);

    apiMethod.hasReturnValue = new ServiceMessages().isEmptyType(method.getOutputType());

    return apiMethod;
  }

  public RequestObjectMethodView generateRequestObjectMethod(
      TransformerContext context, Method method, MethodConfig methodConfig) {
    SurfaceNamer namer = context.getNamer();
    RequestObjectMethodView apiMethod = new RequestObjectMethodView();

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs =
        Arrays.<ParamDocView>asList(getRequestObjectParamDoc(context, method.getInputType()));
    doc.throwsDocLines = namer.getThrowsDocLines();

    apiMethod.doc = doc;

    if (methodConfig.hasRequestObjectMethod()) {
      apiMethod.accessModifier = namer.getPublicAccessModifier();
    } else {
      apiMethod.accessModifier = namer.getPrivateAccessModifier();
    }

    apiMethod.name = namer.getApiMethodName(method);
    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.responseTypeName =
        context.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    apiMethod.callableMethodName = namer.getCallableMethodName(method);

    apiMethod.hasReturnValue = new ServiceMessages().isEmptyType(method.getOutputType());

    return apiMethod;
  }

  public CallableMethodView generateCallableMethod(
      TransformerContext context, Method method, MethodConfig methodConfig) {
    SurfaceNamer namer = context.getNamer();
    CallableMethodView apiMethod = new CallableMethodView();

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs = new ArrayList<>();
    doc.throwsDocLines = new ArrayList<>();

    apiMethod.doc = doc;

    apiMethod.name = namer.getPagedCallableMethodName(method);
    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.responseTypeName =
        context.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    apiMethod.callableName = namer.getPagedCallableName(method);

    return apiMethod;
  }

  public List<PathTemplateCheckView> generatePathTemplateChecks(
      TransformerContext context, MethodConfig methodConfig, ImmutableList<Field> fields) {
    List<PathTemplateCheckView> pathTemplateChecks = new ArrayList<>();
    for (Field field : fields) {
      ImmutableMap<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        CollectionConfig collectionConfig = context.getCollectionConfig(entityName);
        PathTemplateCheckView check = new PathTemplateCheckView();
        check.pathTemplateName = context.getNamer().getPathTemplateName(collectionConfig);
        check.paramName = context.getNamer().getVariableName(field);

        pathTemplateChecks.add(check);
      }
    }
    return pathTemplateChecks;
  }

  public OptionalArrayMethodView generateOptionalArrayMethod(
      TransformerContext context, Method method, MethodConfig methodConfig) {
    SurfaceNamer namer = context.getNamer();
    OptionalArrayMethodView apiMethod = new OptionalArrayMethodView();

    ApiMethodDocView doc = new ApiMethodDocView();

    doc.mainDocLines = namer.getDocLines(method);
    List<ParamDocView> paramDocs =
        getMethodParamDocs(context, methodConfig, methodConfig.getRequiredFields());
    paramDocs.add(
        getOptionalArrayParamDoc(context, methodConfig, methodConfig.getOptionalFields()));
    paramDocs.add(getCallSettingsParamDoc(context));
    doc.paramDocs = paramDocs;
    doc.returnTypeName =
        namer.getDynamicReturnTypeName(context.getTypeTable(), method, methodConfig);

    apiMethod.doc = doc;

    apiMethod.name = namer.getApiMethodName(method);
    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    apiMethod.key = namer.getMethodKey(method);
    apiMethod.grpcMethodName = namer.getGrpcMethodName(method);

    List<DynamicDefaultableParamView> methodParams = new ArrayList<>();
    for (Field field : methodConfig.getRequiredFields()) {
      methodParams.add(generateDefaultableParam(context, field));
    }

    // TODO create a map TypeRef here instead of an array
    // (not done yet because array is sufficient for PHP, and maps are more complex to construct)
    TypeRef arrayType = TypeRef.fromPrimitiveName("string").makeRepeated();

    DynamicDefaultableParamView optionalArgs = new DynamicDefaultableParamView();
    optionalArgs.name = namer.getVariableName(Name.from("optional", "args"));
    optionalArgs.defaultValue = context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType);
    methodParams.add(optionalArgs);

    DynamicDefaultableParamView callSettings = new DynamicDefaultableParamView();
    callSettings.name = namer.getVariableName(Name.from("call", "settings"));
    callSettings.defaultValue = context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType);
    methodParams.add(callSettings);

    apiMethod.methodParams = methodParams;

    List<RequestObjectParamView> requiredRequestObjectParams = new ArrayList<>();
    for (Field field : methodConfig.getRequiredFields()) {
      requiredRequestObjectParams.add(generateRequestObjectParam(context, field));
    }
    apiMethod.requiredRequestObjectParams = requiredRequestObjectParams;

    List<RequestObjectParamView> optionalRequestObjectParams = new ArrayList<>();
    for (Field field : methodConfig.getOptionalFields()) {
      optionalRequestObjectParams.add(generateRequestObjectParam(context, field));
    }
    apiMethod.optionalRequestObjectParams = optionalRequestObjectParams;

    return apiMethod;
  }

  public DynamicDefaultableParamView generateDefaultableParam(
      TransformerContext context, Field field) {
    DynamicDefaultableParamView param = new DynamicDefaultableParamView();
    param.name = context.getNamer().getVariableName(field);
    param.defaultValue = "";
    return param;
  }

  public RequestObjectParamView generateRequestObjectParam(
      TransformerContext context, Field field) {
    SurfaceNamer namer = context.getNamer();
    RequestObjectParamView param = new RequestObjectParamView();
    param.name = namer.getVariableName(field);
    if (namer.shouldImportRequestObjectParamType(field)) {
      param.elementTypeName =
          context.getTypeTable().getAndSaveNicknameForElementType(field.getType());
      param.typeName = context.getTypeTable().getAndSaveNicknameFor(field.getType());
    } else {
      param.elementTypeName = SurfaceNamer.NOT_IMPLEMENTED;
      param.typeName = SurfaceNamer.NOT_IMPLEMENTED;
    }
    param.setCallName = namer.getSetFunctionCallName(field.getType(), field.getSimpleName());
    param.isMap = field.getType().isMap();
    param.isArray = !field.getType().isMap() && field.getType().isRepeated();
    return param;
  }

  public List<ParamDocView> getMethodParamDocs(
      TransformerContext context, MethodConfig methodConfig, Iterable<Field> fields) {
    List<ParamDocView> allDocs = new ArrayList<>();
    for (Field field : fields) {
      SimpleParamDocView paramDoc = new SimpleParamDocView();
      paramDoc.paramName = context.getNamer().getVariableName(field);
      paramDoc.typeName = context.getTypeTable().getFullNameFor(field.getType());

      List<String> docLines = null;
      if (methodConfig.isPageStreaming()
          && methodConfig.getPageStreaming().hasPageSizeField()
          && field.equals(methodConfig.getPageStreaming().getPageSizeField())) {
        docLines =
            Arrays.asList(
                new String[] {
                  "The maximum number of resources contained in the underlying API",
                  "response. If page streaming is performed per-resource, this",
                  "parameter does not affect the return value. If page streaming is",
                  "performed per-page, this determines the maximum number of",
                  "resources in a page."
                });
      } else {
        docLines = context.getNamer().getDocLines(field);
      }

      paramDoc.firstLine = docLines.get(0);
      paramDoc.remainingLines = docLines.subList(1, docLines.size());

      allDocs.add(paramDoc);
    }
    return allDocs;
  }

  public SimpleParamDocView getRequestObjectParamDoc(TransformerContext context, TypeRef typeRef) {
    SimpleParamDocView paramDoc = new SimpleParamDocView();
    paramDoc.paramName = "request";
    paramDoc.typeName = context.getTypeTable().getAndSaveNicknameFor(typeRef);
    paramDoc.firstLine = "The request object containing all of the parameters for the API call.";
    paramDoc.remainingLines = Arrays.asList();
    return paramDoc;
  }

  public ParamDocView getOptionalArrayParamDoc(
      TransformerContext context, MethodConfig methodConfig, Iterable<Field> fields) {
    MapParamDocView paramDoc = new MapParamDocView();

    paramDoc.paramName = context.getNamer().getVariableName(Name.from("optional", "args"));
    paramDoc.typeName = context.getNamer().getOptionalArrayTypeName();

    List<String> docLines = null;
    if (!fields.iterator().hasNext()) {
      docLines =
          Arrays.asList(
              new String[] {
                "Optional. There are no optional parameters for this method yet;",
                // FIXME remove PHP-specific variable naming/syntax
                "          this $optionalArgs parameter reserves a spot for future ones."
              });
    } else {
      docLines = Arrays.asList("Optional.");
    }
    paramDoc.firstLine = docLines.get(0);
    paramDoc.remainingLines = docLines.subList(1, docLines.size());

    paramDoc.arrayKeyDocs = getMethodParamDocs(context, methodConfig, fields);

    return paramDoc;
  }

  public ParamDocView getCallSettingsParamDoc(TransformerContext context) {
    MapParamDocView paramDoc = new MapParamDocView();

    paramDoc.paramName = context.getNamer().getVariableName(Name.from("call", "settings"));
    paramDoc.typeName = context.getNamer().getOptionalArrayTypeName();
    paramDoc.firstLine = "Optional.";
    paramDoc.remainingLines = new ArrayList<>();

    List<ParamDocView> arrayKeyDocs = new ArrayList<>();
    SimpleParamDocView retrySettingsDoc = new SimpleParamDocView();
    retrySettingsDoc.typeName = context.getNamer().getRetrySettingsClassName();
    retrySettingsDoc.paramName = context.getNamer().getVariableName(Name.from("retry", "settings"));
    retrySettingsDoc.firstLine = "Retry settings to use for this call. If present, then";
    // FIXME remove PHP-specific variable naming/syntax
    retrySettingsDoc.remainingLines = Arrays.asList("$timeout is ignored.");
    arrayKeyDocs.add(retrySettingsDoc);

    SimpleParamDocView timeoutDoc = new SimpleParamDocView();
    timeoutDoc.typeName = context.getTypeTable().getAndSaveNicknameFor(TypeRef.of(Type.TYPE_INT32));
    timeoutDoc.paramName = context.getNamer().getVariableName(Name.from("timeout", "millis"));
    // FIXME remove PHP-specific variable naming/syntax
    timeoutDoc.firstLine = "Timeout to use for this call. Only used if $retrySettings";
    timeoutDoc.remainingLines = Arrays.asList("is not set.");
    arrayKeyDocs.add(timeoutDoc);

    paramDoc.arrayKeyDocs = arrayKeyDocs;

    return paramDoc;
  }
}
