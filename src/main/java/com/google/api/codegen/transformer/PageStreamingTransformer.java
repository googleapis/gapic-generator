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

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorClassView;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorView;
import com.google.api.codegen.viewmodel.PagedListResponseFactoryClassView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** PageStreamingTransformer generates view objects for page streaming from a service model. */
public class PageStreamingTransformer {

  public List<PageStreamingDescriptorView> generateDescriptors(InterfaceContext context) {
    List<PageStreamingDescriptorView> descriptors = new ArrayList<>();

    for (MethodModel method : context.getPageStreamingMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      PageStreamingDescriptorView.Builder descriptor = PageStreamingDescriptorView.newBuilder();
      descriptor.varName(context.getNamer().getPageStreamingDescriptorName(method));
      descriptor.requestTokenFieldName(context.getNamer().getRequestTokenFieldName(pageStreaming));
      descriptor.requestTokenGetMethodName(
          context.getNamer().getFieldGetFunctionName(pageStreaming.getRequestTokenField()));
      descriptor.requestTokenSetMethodName(
          context.getNamer().getFieldSetFunctionName(pageStreaming.getRequestTokenField()));
      if (pageStreaming.hasPageSizeField()) {
        descriptor.requestPageSizeFieldName(context.getNamer().getPageSizeFieldName(pageStreaming));
        descriptor.requestPageSizeGetMethodName(
            context.getNamer().getFieldGetFunctionName(pageStreaming.getPageSizeField()));
        descriptor.requestPageSizeSetMethodName(
            context.getNamer().getFieldSetFunctionName(pageStreaming.getPageSizeField()));
      }
      descriptor.responseTokenFieldName(
          context.getNamer().getResponseTokenFieldName(pageStreaming));
      descriptor.responseTokenGetMethodName(
          context.getNamer().getFieldGetFunctionName(pageStreaming.getResponseTokenField()));
      descriptor.resourcesFieldName(context.getNamer().getResourcesFieldName(pageStreaming));
      descriptor.resourcesGetMethodName(
          context.getNamer().getFieldGetFunctionName(pageStreaming.getResourcesField()));
      descriptor.methodName(context.getNamer().getMethodKey(method));

      descriptors.add(descriptor.build());
    }

    return descriptors;
  }

  public List<PageStreamingDescriptorClassView> generateDescriptorClasses(
      InterfaceContext context) {
    List<PageStreamingDescriptorClassView> descriptors = new ArrayList<>();

    for (MethodModel method : context.getPageStreamingMethods()) {
      descriptors.add(generateDescriptorClass(context.asRequestMethodContext(method)));
    }

    return descriptors;
  }

  private PageStreamingDescriptorClassView generateDescriptorClass(MethodContext context) {
    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    MethodModel method = context.getMethodModel();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();

    PageStreamingDescriptorClassView.Builder desc = PageStreamingDescriptorClassView.newBuilder();

    FieldModel resourceField = pageStreaming.getResourcesField();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();

    desc.name(namer.getPageStreamingDescriptorConstName(method));
    desc.typeName(namer.getAndSavePagedResponseTypeName(context, resourceFieldConfig));
    desc.requestTypeName(method.getAndSaveRequestTypeName(typeTable, namer));
    desc.responseTypeName(method.getAndSaveResponseTypeName(typeTable, namer));
    desc.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceField));

    desc.tokenTypeName(typeTable.getAndSaveNicknameFor(pageStreaming.getResponseTokenField()));
    desc.defaultTokenValue(
        typeTable.getSnippetZeroValueAndSaveNicknameFor(pageStreaming.getResponseTokenField()));

    desc.requestTokenSetFunction(
        namer.getFieldSetFunctionName(pageStreaming.getRequestTokenField()));
    if (pageStreaming.hasPageSizeField()) {
      desc.requestPageSizeSetFunction(
          namer.getFieldSetFunctionName(pageStreaming.getPageSizeField()));
      desc.requestPageSizeGetFunction(
          namer.getFieldGetFunctionName(pageStreaming.getPageSizeField()));
    }
    desc.responseTokenGetFunction(
        namer.getFieldGetFunctionName(pageStreaming.getResponseTokenField()));

    ImmutableList.Builder<String> resourcesFieldGetFunctionList = new ImmutableList.Builder<>();
    for (FieldModel field : resourceFieldConfig.getFieldPath()) {
      resourcesFieldGetFunctionList.add(namer.getFieldGetFunctionName(field));
    }
    desc.resourcesFieldGetFunctions(resourcesFieldGetFunctionList.build());

    return desc.build();
  }

  public List<PagedListResponseFactoryClassView> generateFactoryClasses(InterfaceContext context) {
    List<PagedListResponseFactoryClassView> factories = new ArrayList<>();

    for (MethodModel method : context.getPageStreamingMethods()) {
      factories.add(generateFactoryClass(context.asRequestMethodContext(method)));
    }

    return factories;
  }

  private PagedListResponseFactoryClassView generateFactoryClass(MethodContext context) {
    SurfaceNamer namer = context.getNamer();
    MethodModel method = context.getMethodModel();
    ImportTypeTable typeTable = context.getTypeTable();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    FieldModel resourceField = pageStreaming.getResourcesField();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();

    PagedListResponseFactoryClassView.Builder factory =
        PagedListResponseFactoryClassView.newBuilder();

    factory.name(namer.getPagedListResponseFactoryConstName(method));
    factory.requestTypeName(method.getAndSaveRequestTypeName(typeTable, namer));
    factory.responseTypeName(method.getAndSaveResponseTypeName(typeTable, namer));
    factory.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceField));
    factory.pagedListResponseTypeName(
        namer.getAndSavePagedResponseTypeName(context, resourceFieldConfig));
    factory.pageStreamingDescriptorName(namer.getPageStreamingDescriptorConstName(method));

    return factory.build();
  }
}
