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
import com.google.api.codegen.surface.SurfaceOptionalArrayMethod;
import com.google.api.codegen.surface.SurfacePagedFlattenedMethod;
import com.google.api.codegen.surface.SurfacePagedRequestObjectMethod;
import com.google.api.codegen.surface.SurfacePathTemplateCheck;
import com.google.api.codegen.surface.SurfaceRequestObjectParam;
import com.google.api.codegen.util.Name;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

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
    doc.paramDocLines = getMethodParamDocLines(context, fields);
    doc.throwsDocLines = namer.getThrowsDocLines();

    apiMethod.doc = doc;

    PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();
    apiMethod.resourceTypeName =
        context
            .getTypeTable()
            .importAndGetShortestNameForElementType(pageStreaming.getResourcesField().getType());
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

    apiMethod.requestTypeName =
        context.getTypeTable().importAndGetShortestName(method.getInputType());

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
    doc.paramDocLines = getRequestObjectDocLines(context, method.getInputType());
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
            .importAndGetShortestNameForElementType(pageStreaming.getResourcesField().getType());
    apiMethod.name = namer.getApiMethodName(method);
    apiMethod.requestTypeName =
        context.getTypeTable().importAndGetShortestName(method.getInputType());

    return apiMethod;
  }

  public SurfaceOptionalArrayMethod generateOptionalArrayMethod(
      ModelToSurfaceContext context, Method method, MethodConfig methodConfig) {
    IdentifierNamer namer = context.getNamer();
    SurfaceOptionalArrayMethod apiMethod = new SurfaceOptionalArrayMethod();

    apiMethod.name = namer.getApiMethodName(method);
    apiMethod.requestTypeName =
        context.getTypeTable().importAndGetShortestName(method.getInputType());
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
    optionalArgs.defaultValue = context.getTypeTable().importAndGetZeroValue(arrayType);
    methodParams.add(optionalArgs);

    SurfaceDynamicDefaultableParam callSettings = new SurfaceDynamicDefaultableParam();
    callSettings.name = namer.getVariableName(Name.from("call", "settings"));
    callSettings.defaultValue = context.getTypeTable().importAndGetZeroValue(arrayType);
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
          context.getTypeTable().importAndGetShortestNameForElementType(field.getType());
      param.typeName = context.getTypeTable().importAndGetShortestName(field.getType());
    } else {
      param.elementTypeName = IdentifierNamer.NOT_POPULATED;
      param.typeName = IdentifierNamer.NOT_POPULATED;
    }
    param.setCallName = namer.getSetFunctionCallName(field.getType(), field.getSimpleName());
    param.isMap = field.getType().isMap();
    param.isArray = !field.getType().isMap() && field.getType().isRepeated();
    return param;
  }

  private List<String> getMethodParamDocLines(
      ModelToSurfaceContext context, Iterable<Field> fields) {
    List<String> allDocLines = new ArrayList<>();
    for (Field field : fields) {
      List<String> docLines = context.getNamer().getDocLines(field);

      String firstLine = context.getNamer().getParamDocPrefix(field) + docLines.get(0);
      docLines.set(0, firstLine);

      allDocLines.addAll(docLines);
    }
    return allDocLines;
  }

  private List<String> getRequestObjectDocLines(ModelToSurfaceContext context, TypeRef typeRef) {
    String docLinePrefix = context.getNamer().getParamDocPrefix("request", typeRef);
    String docLine =
        docLinePrefix + "The request object containing all of the parameters for the API call.";
    return Arrays.asList(docLine);
  }
}
