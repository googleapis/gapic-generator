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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class CSharpSurfaceNamer extends SurfaceNamer {

  public CSharpSurfaceNamer(String implicitPackageName) {
    super(
        new CSharpNameFormatter(),
        new ModelTypeFormatterImpl(new CSharpModelTypeNameConverter(implicitPackageName)),
        new CSharpTypeTable(implicitPackageName));
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(Interface service, String packageName) {
    return packageName + "." + getApiWrapperClassName(service);
  }

  @Override
  public String getStaticLangReturnTypeName(Method method, MethodConfig methodConfig) {
    if (ServiceMessages.s_isEmptyType(method.getOutputType())) {
      return "void";
    }
    return getModelTypeFormatter().getFullNameFor(method.getOutputType());
  }

  @Override
  public String getStaticLangAsyncReturnTypeName(Method method, MethodConfig methodConfig) {
    if (ServiceMessages.s_isEmptyType(method.getOutputType())) {
      return "Task";
    }
    return "Task<" + getModelTypeFormatter().getFullNameFor(method.getOutputType()) + ">";
  }

  @Override
  public String getApiWrapperClassName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  @Override
  public String getCallableName(Method method) {
    // TODO: Use the 'privateFieldName' method when it's available (from Go MVVM PR)
    return "_" + varName(Name.upperCamel("Call", method.getSimpleName()));
  }

  @Override
  public String getPathTemplateName(CollectionConfig collectionConfig) {
    return inittedConstantName(Name.from(collectionConfig.getEntityName(), "template"));
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return methodName(identifier);
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      ModelTypeTable typeTable, TypeRef... parameterizedTypes) {
    String[] typeList = new String[parameterizedTypes.length];
    for (int i = 0; i < typeList.length; i++) {
      typeList[i] = typeTable.getFullNameForElementType(parameterizedTypes[i]);
    }
    return typeTable.getAndSaveNicknameForContainer("Google.Api.Gax.PagedEnumerable", typeList);
  }

  @Override
  public String getGrpcContainerTypeName(Interface service) {
    return className(Name.upperCamel(service.getSimpleName()));
  }

  @Override
  public String getGrpcServiceClassName(Interface service) {
    return className(Name.upperCamel(service.getSimpleName()))
        + "."
        + className(Name.upperCamel(service.getSimpleName(), "Client"));
  }

  @Override
  public String getApiWrapperClassImplName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "ClientImpl"));
  }

  @Override
  public String getAsyncApiMethodName(Method method) {
    return getApiMethodName(method) + "Async";
  }

  @Override
  public String getPageStreamingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()));
  }

  @Override
  public String getParamName(String var) {
    return varName(Name.from(var).join("id"));
  }

  @Override
  public String retryFilterMethodName(String key) {
    return methodName(Name.from(key).join("retry").join("filter"));
  }

  /** The method name of the retry backoff for the given key */
  @Override
  public String retryBackoffMethodName(String key) {
    return methodName(Name.from("get").join(key).join("retry").join("backoff"));
  }

  /** The method name of the timeout backoff for the given key */
  @Override
  public String timeoutBackoffMethodName(String key) {
    return methodName(Name.from("get").join(key).join("timeout").join("backoff"));
  }

  @Override
  public List<String> getReturnDocLines(
      SurfaceTransformerContext context, MethodConfig methodConfig, Synchronicity synchronicity) {
    if (methodConfig.isPageStreaming()) {
      TypeRef resourceType = methodConfig.getPageStreaming().getResourcesField().getType();
      String resourceTypeName =
          context.getTypeTable().getAndSaveNicknameForElementType(resourceType);
      switch (synchronicity) {
        case Sync:
          return ImmutableList.of(
              "A pageable sequence of <see cref=\"" + resourceTypeName + "\"/> resources.");
        case Async:
          return ImmutableList.of(
              "A pageable asynchronous sequence of <see cref=\""
                  + resourceTypeName
                  + "\"/> resources.");
      }
    } else {
      switch (synchronicity) {
        case Sync:
          return ImmutableList.of("The RPC response.");
        case Async:
          return ImmutableList.of("A Task containing the RPC response.");
      }
    }
    throw new IllegalStateException("Invalid Synchronicity: " + synchronicity);
  }
}
