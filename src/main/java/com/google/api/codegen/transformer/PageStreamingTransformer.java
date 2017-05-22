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
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorClassView;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorView;
import com.google.api.codegen.viewmodel.PagedListResponseFactoryClassView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import java.util.ArrayList;
import java.util.List;

/** PageStreamingTransformer generates view objects for page streaming from a service model. */
public class PageStreamingTransformer {

  public List<PageStreamingDescriptorView> generateDescriptors(GapicInterfaceContext context) {
    List<PageStreamingDescriptorView> descriptors = new ArrayList<>();

    for (Method method : context.getPageStreamingMethods()) {
      GapicMethodConfig methodConfig = context.getMethodConfig(method);
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      PageStreamingDescriptorView.Builder descriptor = PageStreamingDescriptorView.newBuilder();
      descriptor.varName(context.getNamer().getPageStreamingDescriptorName(method));
      descriptor.requestTokenFieldName(context.getNamer().getRequestTokenFieldName(pageStreaming));
      if (pageStreaming.hasPageSizeField()) {
        descriptor.requestPageSizeFieldName(context.getNamer().getPageSizeFieldName(pageStreaming));
      }
      descriptor.responseTokenFieldName(
          context.getNamer().getResponseTokenFieldName(pageStreaming));
      descriptor.resourcesFieldName(context.getNamer().getResourcesFieldName(pageStreaming));
      descriptor.methodName(context.getNamer().getMethodKey(method));

      descriptors.add(descriptor.build());
    }

    return descriptors;
  }

  public List<PageStreamingDescriptorClassView> generateDescriptorClasses(
      GapicInterfaceContext context) {
    List<PageStreamingDescriptorClassView> descriptors = new ArrayList<>();

    for (Method method : context.getPageStreamingMethods()) {
      descriptors.add(generateDescriptorClass(context.asRequestMethodContext(method)));
    }

    return descriptors;
  }

  private PageStreamingDescriptorClassView generateDescriptorClass(GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();

    PageStreamingDescriptorClassView.Builder desc = PageStreamingDescriptorClassView.newBuilder();

    Field resourceField = pageStreaming.getResourcesField();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    TypeRef resourceType = resourceField.getType();

    desc.name(namer.getPageStreamingDescriptorConstName(method));
    desc.typeName(namer.getAndSavePagedResponseTypeName(method, typeTable, resourceFieldConfig));
    desc.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    desc.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    desc.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceField.getType()));

    TypeRef tokenType = pageStreaming.getResponseTokenField().getType();
    desc.tokenTypeName(typeTable.getAndSaveNicknameFor(tokenType));
    desc.defaultTokenValue(context.getTypeTable().getSnippetZeroValueAndSaveNicknameFor(tokenType));

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
    desc.resourcesFieldGetFunction(
        namer.getFieldGetFunctionName(pageStreaming.getResourcesField()));

    return desc.build();
  }

  public List<PagedListResponseFactoryClassView> generateFactoryClasses(
      GapicInterfaceContext context) {
    List<PagedListResponseFactoryClassView> factories = new ArrayList<>();

    for (Method method : context.getPageStreamingMethods()) {
      factories.add(generateFactoryClass(context.asRequestMethodContext(method)));
    }

    return factories;
  }

  private PagedListResponseFactoryClassView generateFactoryClass(GapicMethodContext context) {
    SurfaceNamer namer = context.getNamer();
    ModelTypeTable typeTable = context.getTypeTable();
    Method method = context.getMethod();
    PageStreamingConfig pageStreaming = context.getMethodConfig().getPageStreaming();
    Field resourceField = pageStreaming.getResourcesField();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();

    PagedListResponseFactoryClassView.Builder factory =
        PagedListResponseFactoryClassView.newBuilder();

    factory.name(namer.getPagedListResponseFactoryConstName(method));
    factory.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    factory.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));
    factory.resourceTypeName(typeTable.getAndSaveNicknameForElementType(resourceField.getType()));
    factory.pagedListResponseTypeName(
        namer.getAndSavePagedResponseTypeName(method, typeTable, resourceFieldConfig));
    factory.pageStreamingDescriptorName(namer.getPageStreamingDescriptorConstName(method));

    return factory.build();
  }
}
