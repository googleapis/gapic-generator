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

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;

public class CSharpSurfaceNamer extends SurfaceNamer {

  private static ImmutableSet<String> keywords =
      ImmutableSet.<String>builder()
          .add("abstract")
          .add("as")
          .add("base")
          .add("bool")
          .add("break")
          .add("byte")
          .add("case")
          .add("catch")
          .add("char")
          .add("checked")
          .add("class")
          .add("const")
          .add("continue")
          .add("decimal")
          .add("default")
          .add("delegate")
          .add("do")
          .add("double")
          .add("else")
          .add("enum")
          .add("event")
          .add("explicit")
          .add("extern")
          .add("false")
          .add("finally")
          .add("fixed")
          .add("float")
          .add("for")
          .add("foreach")
          .add("goto")
          .add("if")
          .add("implicit")
          .add("in")
          .add("int")
          .add("interface")
          .add("internal")
          .add("is")
          .add("lock")
          .add("long")
          .add("namespace")
          .add("new")
          .add("null")
          .add("object")
          .add("operator")
          .add("out")
          .add("override")
          .add("params")
          .add("private")
          .add("protected")
          .add("public")
          .add("readonly")
          .add("ref")
          .add("return")
          .add("sbyte")
          .add("sealed")
          .add("short")
          .add("sizeof")
          .add("stackalloc")
          .add("static")
          .add("string")
          .add("struct")
          .add("switch")
          .add("this")
          .add("throw")
          .add("true")
          .add("try")
          .add("typeof")
          .add("uint")
          .add("ulong")
          .add("unchecked")
          .add("unsafe")
          .add("ushort")
          .add("using")
          .add("virtual")
          .add("void")
          .add("volatile")
          .add("while")
          .build();

  private static String prefixKeyword(String name) {
    return keywords.contains(name) ? "@" + name : name;
  }

  public CSharpSurfaceNamer(String packageName) {
    super(
        new CSharpNameFormatter(),
        new ModelTypeFormatterImpl(new CSharpModelTypeNameConverter(packageName)),
        new CSharpTypeTable(packageName),
        packageName);
  }

  @Override
  public String localVarName(Name name) {
    return prefixKeyword(super.localVarName(name));
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(Interface service) {
    return getPackageName() + "." + getApiWrapperClassName(service);
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
      return "System.Threading.Tasks.Task";
    }
    return "System.Threading.Tasks.Task<"
        + getModelTypeFormatter().getFullNameFor(method.getOutputType())
        + ">";
  }

  @Override
  public String getStaticLangCallerAsyncReturnTypeName(Method method, MethodConfig methodConfig) {
    // Same as sync because of 'await'
    return getStaticLangReturnTypeName(method, methodConfig);
  }

  @Override
  public String getApiWrapperClassName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  @Override
  public String getApiSnippetsClassName(Interface interfaze) {
    return className(Name.upperCamel("Generated", interfaze.getSimpleName(), "ClientSnippets"));
  }

  @Override
  public String getCallableName(Method method) {
    return privateFieldName(Name.upperCamel("Call", method.getSimpleName()));
  }

  @Override
  public String getModifyMethodName(Method method) {
    return "Modify_"
        + privateMethodName(
            Name.upperCamel(getModelTypeFormatter().getNicknameFor(method.getInputType())));
  }

  @Override
  public String getPathTemplateName(
      Interface service, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "template"));
  }

  @Override
  public String getFieldGetFunctionName(TypeRef type, Name identifier) {
    return privateMethodName(identifier);
  }

  @Override
  public String getExamplePackageName() {
    return getPackageName() + ".Snippets";
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      Method method, ModelTypeTable typeTable, Field resourceField) {

    String inputTypeName = typeTable.getAndSaveNicknameForElementType(method.getInputType());
    String outputTypeName = typeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = typeTable.getAndSaveNicknameForElementType(resourceField.getType());
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedEnumerable", inputTypeName, outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveAsyncPagedResponseTypeName(
      Method method, ModelTypeTable typeTable, Field resourceField) {

    String inputTypeName = typeTable.getAndSaveNicknameForElementType(method.getInputType());
    String outputTypeName = typeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = typeTable.getAndSaveNicknameForElementType(resourceField.getType());
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedAsyncEnumerable", inputTypeName, outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveCallerPagedResponseTypeName(
      Method method, ModelTypeTable typeTable, Field resourceField) {

    String outputTypeName = typeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = typeTable.getAndSaveNicknameForElementType(resourceField.getType());
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.IPagedEnumerable", outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveCallerAsyncPagedResponseTypeName(
      Method method, ModelTypeTable typeTable, Field resourceField) {

    String outputTypeName = typeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = typeTable.getAndSaveNicknameForElementType(resourceField.getType());
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.IPagedAsyncEnumerable", outputTypeName, resourceTypeName);
  }

  @Override
  public String getGrpcContainerTypeName(Interface service) {
    return className(Name.upperCamel(service.getSimpleName()));
  }

  @Override
  public String getReroutedGrpcClientVarName(MethodConfig methodConfig) {
    String reroute = methodConfig.getRerouteToGrpcInterface();
    if (reroute == null) {
      return "GrpcClient";
    } else {
      List<String> reroutes = Splitter.on('.').splitToList(reroute);
      return Name.anyCamel("grpc", reroutes.get(reroutes.size() - 1), "client").toLowerCamel();
    }
  }

  @Override
  public String getReroutedGrpcMethodName(MethodConfig methodConfig) {
    List<String> reroutes = Splitter.on('.').splitToList(methodConfig.getRerouteToGrpcInterface());
    return Name.anyCamel("create", reroutes.get(reroutes.size() - 1), "client").toUpperCamel();
  }

  @Override
  public String getGrpcServiceClassName(Interface service) {
    return className(Name.upperCamel(service.getSimpleName()))
        + "."
        + className(Name.upperCamel(service.getSimpleName(), "Client"));
  }

  @Override
  public String getApiWrapperVariableName(Interface interfaze) {
    return localVarName(Name.upperCamel(interfaze.getSimpleName(), "Client"));
  }

  @Override
  public String getApiWrapperClassImplName(Interface interfaze) {
    return className(Name.upperCamel(interfaze.getSimpleName(), "ClientImpl"));
  }

  @Override
  public String getPageStreamingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()));
  }

  @Override
  public String getParamName(String var) {
    return localVarName(Name.from(var).join("id"));
  }

  @Override
  public String getParamDocName(String var) {
    return super.localVarName(Name.from(var));
  }

  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(identifier);
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
