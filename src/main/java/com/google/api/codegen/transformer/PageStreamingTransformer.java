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

import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorClassView;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorView;
import com.google.api.codegen.viewmodel.PageStreamingFactoryClassView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.List;

/**
 * PageStreamingTransformer generates view objects for page streaming from a service model.
 */
public class PageStreamingTransformer {

  public List<PageStreamingDescriptorView> generateDescriptors(SurfaceTransformerContext context) {
    List<PageStreamingDescriptorView> descriptors = new ArrayList<>();

    for (Method method : context.getPageStreamingMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      context.getNamer().addPageStreamingDescriptorImports(context.getTypeTable());
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      PageStreamingDescriptorView.Builder descriptor = PageStreamingDescriptorView.newBuilder();
      descriptor.varName(context.getNamer().getPageStreamingDescriptorName(method));
      descriptor.requestTokenFieldName(pageStreaming.getRequestTokenField().getSimpleName());
      if (pageStreaming.hasPageSizeField()) {
        descriptor.requestPageSizeFieldName(pageStreaming.getPageSizeField().getSimpleName());
      }
      descriptor.responseTokenFieldName(pageStreaming.getResponseTokenField().getSimpleName());
      descriptor.resourcesFieldName(pageStreaming.getResourcesField().getSimpleName());
      descriptor.methodName(Name.upperCamel(method.getSimpleName()).toLowerCamel());

      descriptors.add(descriptor.build());
    }

    return descriptors;
  }

  public List<PageStreamingDescriptorClassView> generateDescriptorClasses(
      SurfaceTransformerContext context) {
    List<PageStreamingDescriptorClassView> descriptors = new ArrayList<>();

    context.getNamer().addPageStreamingDescriptorImports(context.getTypeTable());
    for (Method method : context.getPageStreamingMethods()) {
      descriptors.add(generateDescriptorClass(context.asMethodContext(method)));
    }

    return descriptors;
  }

  private PageStreamingDescriptorClassView generateDescriptorClass(
      MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    FeatureConfig featureConfig = context.getFeatureConfig();

    PageStreamingDescriptorClassView.Builder desc = PageStreamingDescriptorClassView.newBuilder();

    Field resourceField = pageStreaming.getResourcesField();
    TypeRef resourceType = resourceField.getType();

    desc.name(namer.getPageStreamingDescriptorConstName(method));
    desc.typeName(
        namer.getAndSavePagedResponseTypeName(
            method,
            featureConfig,
            typeTable,
            method.getInputType(),
            method.getOutputType(),
            resourceField));
    desc.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    desc.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    desc.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceField.getType()));

    TypeRef tokenType = pageStreaming.getResponseTokenField().getType();
    desc.tokenTypeName(typeTable.getAndSaveNicknameFor(tokenType));
    desc.defaultTokenValue(context.getTypeTable().getZeroValueAndSaveNicknameFor(tokenType));

    // The resource fields are "repeated" in the proto.
    // We `makeOptional` so that we get the zero value of the resource,
    // not the zero value of the array/list of resources.
    desc.resourceZeroValue(
        context.getTypeTable().getZeroValueAndSaveNicknameFor(resourceType.makeOptional()));

    desc.requestTokenSetFunction(
        namer.getFieldSetFunctionName(featureConfig, pageStreaming.getRequestTokenField()));
    if (pageStreaming.hasPageSizeField()) {
      desc.requestPageSizeSetFunction(
          namer.getFieldSetFunctionName(featureConfig, pageStreaming.getPageSizeField()));
      desc.requestPageSizeGetFunction(
          namer.getFieldGetFunctionName(featureConfig, pageStreaming.getPageSizeField()));
    }
    desc.responseTokenGetFunction(
        namer.getFieldGetFunctionName(featureConfig, pageStreaming.getResponseTokenField()));
    desc.resourcesFieldGetFunction(
        namer.getFieldGetFunctionName(featureConfig, pageStreaming.getResourcesField()));

    return desc.build();
  }

  public List<PageStreamingFactoryClassView> generateFactoryClasses(
      SurfaceTransformerContext context) {
    List<PageStreamingFactoryClassView> factories = new ArrayList<>();

    context.getNamer().addPageStreamingFactoryImports(context.getTypeTable());
    for (Method method : context.getPageStreamingMethods()) {
      factories.add(generateFactoryClass(context.asMethodContext(method)));
    }

    return factories;
  }

  private PageStreamingFactoryClassView generateFactoryClass(MethodTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    FeatureConfig featureConfig = context.getFeatureConfig();

    PageStreamingFactoryClassView.Builder fact = PageStreamingFactoryClassView.newBuilder();

    Field resourceField = pageStreaming.getResourcesField();

    fact.name(namer.getPageStreamingDescriptorConstName(method));
    fact.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    fact.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    fact.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceField.getType()));
    fact.pagedListResponseTypeName(
        namer.getAndSavePagedResponseTypeName(
            method,
            featureConfig,
            typeTable,
            method.getInputType(),
            method.getOutputType(),
            resourceField));

    return fact.build();
  }
}
