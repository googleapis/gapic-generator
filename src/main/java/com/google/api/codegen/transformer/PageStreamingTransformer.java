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

import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.PageStreamingConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorClassView;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorView;
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

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (!methodConfig.isPageStreaming()) {
        continue;
      }
      context.getNamer().addPageStreamingDescriptorImports(context.getTypeTable());
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      PageStreamingDescriptorView.Builder descriptor = PageStreamingDescriptorView.newBuilder();
      descriptor.varName(context.getNamer().getPageStreamingDescriptorName(method));
      descriptor.requestTokenFieldName(pageStreaming.getRequestTokenField().getSimpleName());
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
    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (!methodConfig.isPageStreaming()) {
        continue;
      }
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

    PageStreamingDescriptorClassView.Builder desc = PageStreamingDescriptorClassView.newBuilder();

    desc.name(namer.getPageStreamingDescriptorConstName(method));
    desc.requestTypeName(typeTable.getAndSaveNicknameFor(method.getInputType()));
    desc.responseTypeName(typeTable.getAndSaveNicknameFor(method.getOutputType()));

    TypeRef resourceType = pageStreaming.getResourcesField().getType();
    desc.resourceTypeName(context.getTypeTable().getAndSaveNicknameForElementType(resourceType));

    TypeRef tokenType = pageStreaming.getResponseTokenField().getType();
    desc.tokenTypeName(typeTable.getAndSaveNicknameFor(tokenType));

    desc.defaultTokenValue(context.getTypeTable().getZeroValueAndSaveNicknameFor(tokenType));

    desc.fnSetRequestToken(namer.getSetFunctionCallName(pageStreaming.getRequestTokenField()));
    desc.fnGetResponseToken(namer.getGetFunctionCallName(pageStreaming.getResponseTokenField()));
    desc.fnGetResourcesField(namer.getGetFunctionCallName(pageStreaming.getResourcesField()));

    return desc.build();
  }
}
