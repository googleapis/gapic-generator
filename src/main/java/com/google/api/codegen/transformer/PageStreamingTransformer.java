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
import com.google.api.codegen.viewmodel.PageStreamingDescriptorView;
import com.google.api.tools.framework.model.Method;

import java.util.ArrayList;
import java.util.List;

public class PageStreamingTransformer {

  public List<PageStreamingDescriptorView> generateDescriptors(TransformerContext context) {
    List<PageStreamingDescriptorView> descriptors = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (!methodConfig.isPageStreaming()) {
        continue;
      }
      context.getNamer().addPageStreamingDescriptorImports(context.getTypeTable());
      PageStreamingConfig pageStreaming = methodConfig.getPageStreaming();

      PageStreamingDescriptorView descriptor = new PageStreamingDescriptorView();
      descriptor.varName = context.getNamer().getPageStreamingDescriptorName(method);
      descriptor.requestTokenFieldName = pageStreaming.getRequestTokenField().getSimpleName();
      descriptor.responseTokenFieldName = pageStreaming.getResponseTokenField().getSimpleName();
      descriptor.resourcesFieldName = pageStreaming.getResourcesField().getSimpleName();
      descriptor.methodName = Name.upperCamel(method.getSimpleName()).toLowerCamel();

      descriptors.add(descriptor);
    }

    return descriptors;
  }
}
