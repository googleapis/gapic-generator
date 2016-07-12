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
import com.google.api.codegen.surface.SurfaceApiMethodDoc;
import com.google.api.codegen.surface.SurfaceDynamicDefaultableParam;
import com.google.api.codegen.surface.SurfaceMapParamDoc;
import com.google.api.codegen.surface.SurfaceOptionalArrayMethod;
import com.google.api.codegen.surface.SurfacePagedFlattenedMethod;
import com.google.api.codegen.surface.SurfacePagedRequestObjectMethod;
import com.google.api.codegen.surface.SurfaceParamDoc;
import com.google.api.codegen.surface.SurfacePathTemplateCheck;
import com.google.api.codegen.surface.SurfaceRequestObjectParam;
import com.google.api.codegen.surface.SurfaceSimpleParamDoc;
import com.google.api.codegen.util.Name;
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

  public SurfacePagedFlattenedMethod generatePagedFlattenedMethod(
      ModelToSurfaceContext context,
      Method method,
      MethodConfig methodConfig,
      ImmutableList<Field> fields) {
    IdentifierNamer namer = context.getNamer();
    SurfacePagedFlattenedMethod apiMethod = new SurfacePagedFlattenedMethod();

    apiMethod.initCode =
        initCodeTransformer.generateInitCode(context, method, methodConfig, fields);

    SurfaceApiMethodDoc doc = new SurfaceApiMethodDoc();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs = getMethodParamDocs(context, methodConfig, fields);
    doc.throwsDocLines = namer.getThrowsDocLines();

    apiMethod.doc = doc;

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        context
            .getTypeTable()
            .getAndSaveNicknameForElementType(pageStreaming.getResourcesField().getType());
    apiMethod.name = namer.getApiMethodName(method);

    List<SurfaceRequestObjectParam> params = new ArrayList<>();
    for (Field field : fields) {
      params.add(generateRequestObjectParam(context, field));
    }
    apiMethod.methodParams = params;
    apiMethod.requestObjectParams = params;

    List<SurfacePathTemplateCheck> pathTemplateChecks = new ArrayList<>();
    for (Field field : fields) {
      ImmutableMap<String, String> fieldNamePatterns = methodConfig.getFieldNamePatterns();
      String entityName = fieldNamePatterns.get(field.getSimpleName());
      if (entityName != null) {
        CollectionConfig collectionConfig = context.getCollectionConfig(entityName);
        SurfacePathTemplateCheck check = new SurfacePathTemplateCheck();
        check.pathTemplateName = namer.getPathTemplateName(collectionConfig);
        check.paramName = namer.getVariableName(field);

        pathTemplateChecks.add(check);
      }
    }
    apiMethod.pathTemplateChecks = pathTemplateChecks;

    apiMethod.requestTypeName = context.getTypeTable().getAndSaveNicknameFor(method.getInputType());

    apiMethod.apiClassName = namer.getApiWrapperClassName(context.getInterface());
    apiMethod.apiVariableName = namer.getApiWrapperVariableName(context.getInterface());

    return apiMethod;
  }

  public SurfacePagedRequestObjectMethod generatePagedRequestObjectMethod(
      ModelToSurfaceContext context, Method method, MethodConfig methodConfig) {
    IdentifierNamer namer = context.getNamer();
    SurfacePagedRequestObjectMethod apiMethod = new SurfacePagedRequestObjectMethod();

    SurfaceApiMethodDoc doc = new SurfaceApiMethodDoc();

    doc.mainDocLines = namer.getDocLines(method);
    doc.paramDocs =
        Arrays.<SurfaceParamDoc>asList(getRequestObjectParamDoc(context, method.getInputType()));
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

    return apiMethod;
  }

  public SurfaceOptionalArrayMethod generateOptionalArrayMethod(
      ModelToSurfaceContext context, Method method, MethodConfig methodConfig) {
    IdentifierNamer namer = context.getNamer();
    SurfaceOptionalArrayMethod apiMethod = new SurfaceOptionalArrayMethod();

    SurfaceApiMethodDoc doc = new SurfaceApiMethodDoc();

    doc.mainDocLines = namer.getDocLines(method);
    List<SurfaceParamDoc> paramDocs =
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

    List<SurfaceDynamicDefaultableParam> methodParams = new ArrayList<>();
    for (Field field : methodConfig.getRequiredFields()) {
      methodParams.add(generateDefaultableParam(context, field));
    }

    // TODO create a map TypeRef here instead of an array
    // (not done yet because array is sufficient for PHP, and maps are more complex to construct)
    TypeRef arrayType = TypeRef.fromPrimitiveName("string").makeRepeated();

    SurfaceDynamicDefaultableParam optionalArgs = new SurfaceDynamicDefaultableParam();
    optionalArgs.name = namer.getVariableName(Name.from("optional", "args"));
    optionalArgs.defaultValue = context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType);
    methodParams.add(optionalArgs);

    SurfaceDynamicDefaultableParam callSettings = new SurfaceDynamicDefaultableParam();
    callSettings.name = namer.getVariableName(Name.from("call", "settings"));
    callSettings.defaultValue = context.getTypeTable().getZeroValueAndSaveNicknameFor(arrayType);
    methodParams.add(callSettings);

    apiMethod.methodParams = methodParams;

    List<SurfaceRequestObjectParam> requiredRequestObjectParams = new ArrayList<>();
    for (Field field : methodConfig.getRequiredFields()) {
      requiredRequestObjectParams.add(generateRequestObjectParam(context, field));
    }
    apiMethod.requiredRequestObjectParams = requiredRequestObjectParams;

    List<SurfaceRequestObjectParam> optionalRequestObjectParams = new ArrayList<>();
    for (Field field : methodConfig.getOptionalFields()) {
      optionalRequestObjectParams.add(generateRequestObjectParam(context, field));
    }
    apiMethod.optionalRequestObjectParams = optionalRequestObjectParams;

    return apiMethod;
  }

  public SurfaceDynamicDefaultableParam generateDefaultableParam(
      ModelToSurfaceContext context, Field field) {
    SurfaceDynamicDefaultableParam param = new SurfaceDynamicDefaultableParam();
    param.name = context.getNamer().getVariableName(field);
    param.defaultValue = "";
    return param;
  }

  public SurfaceRequestObjectParam generateRequestObjectParam(
      ModelToSurfaceContext context, Field field) {
    IdentifierNamer namer = context.getNamer();
    SurfaceRequestObjectParam param = new SurfaceRequestObjectParam();
    param.name = namer.getVariableName(field);
    if (namer.shouldImportRequestObjectParamType(field)) {
      param.elementTypeName =
          context.getTypeTable().getAndSaveNicknameForElementType(field.getType());
      param.typeName = context.getTypeTable().getAndSaveNicknameFor(field.getType());
    } else {
      param.elementTypeName = IdentifierNamer.NOT_IMPLEMENTED;
      param.typeName = IdentifierNamer.NOT_IMPLEMENTED;
    }
    param.setCallName = namer.getSetFunctionCallName(field.getType(), field.getSimpleName());
    param.isMap = field.getType().isMap();
    param.isArray = !field.getType().isMap() && field.getType().isRepeated();
    return param;
  }

  public List<SurfaceParamDoc> getMethodParamDocs(
      ModelToSurfaceContext context, MethodConfig methodConfig, Iterable<Field> fields) {
    List<SurfaceParamDoc> allDocs = new ArrayList<>();
    for (Field field : fields) {
      SurfaceSimpleParamDoc paramDoc = new SurfaceSimpleParamDoc();
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

  public SurfaceSimpleParamDoc getRequestObjectParamDoc(
      ModelToSurfaceContext context, TypeRef typeRef) {
    SurfaceSimpleParamDoc paramDoc = new SurfaceSimpleParamDoc();
    paramDoc.paramName = "request";
    paramDoc.typeName = context.getTypeTable().getAndSaveNicknameFor(typeRef);
    paramDoc.firstLine = "The request object containing all of the parameters for the API call.";
    paramDoc.remainingLines = Arrays.asList();
    return paramDoc;
  }

  public SurfaceParamDoc getOptionalArrayParamDoc(
      ModelToSurfaceContext context, MethodConfig methodConfig, Iterable<Field> fields) {
    SurfaceMapParamDoc paramDoc = new SurfaceMapParamDoc();

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

  public SurfaceParamDoc getCallSettingsParamDoc(ModelToSurfaceContext context) {
    SurfaceMapParamDoc paramDoc = new SurfaceMapParamDoc();

    paramDoc.paramName = context.getNamer().getVariableName(Name.from("call", "settings"));
    paramDoc.typeName = context.getNamer().getOptionalArrayTypeName();
    paramDoc.firstLine = "Optional.";
    paramDoc.remainingLines = new ArrayList<>();

    List<SurfaceParamDoc> arrayKeyDocs = new ArrayList<>();
    SurfaceSimpleParamDoc retrySettingsDoc = new SurfaceSimpleParamDoc();
    retrySettingsDoc.typeName = context.getNamer().getRetrySettingsClassName();
    retrySettingsDoc.paramName = context.getNamer().getVariableName(Name.from("retry", "settings"));
    retrySettingsDoc.firstLine = "Retry settings to use for this call. If present, then";
    // FIXME remove PHP-specific variable naming/syntax
    retrySettingsDoc.remainingLines = Arrays.asList("$timeout is ignored.");
    arrayKeyDocs.add(retrySettingsDoc);

    SurfaceSimpleParamDoc timeoutDoc = new SurfaceSimpleParamDoc();
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
