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
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldType;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.csharp.CSharpCommentReformatter;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
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
        new CSharpCommentReformatter(),
        packageName);
  }

  @Override
  public String localVarName(Name name) {
    return prefixKeyword(super.localVarName(name));
  }

  @Override
  public String getFullyQualifiedApiWrapperClassName(InterfaceConfig interfaceConfig) {
    return getPackageName() + "." + getApiWrapperClassName(interfaceConfig);
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
  public String getApiSnippetsClassName(Interface apiInterface) {
    return publicClassName(
        Name.upperCamel("Generated", apiInterface.getSimpleName(), "ClientSnippets"));
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
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "template"));
  }

  @Override
  public String getResourceNameFieldGetFunctionName(FieldConfig fieldConfig) {
    FieldType type = fieldConfig.getField();
    String fieldName = fieldConfig.getField().getSimpleName();
    Name identifier = Name.from(fieldName);
    Name resourceName;

    if (type.isMap()) {
      return getNotImplementedString("SurfaceNamer.getResourceNameFieldGetFunctionName:map-type");
    }

    // Omit the identifier when the field is called name (or names for the repeated case)
    boolean requireIdentifier =
        !((type.isRepeated() && fieldName.toLowerCase().equals("names"))
            || (!type.isRepeated() && fieldName.toLowerCase().equals("name")));
    boolean requireAs =
        requireIdentifier || (fieldConfig.getResourceNameType() == ResourceNameType.ANY);
    boolean requirePlural = type.isRepeated();

    if (fieldConfig.getResourceNameType() == ResourceNameType.ANY) {
      resourceName = Name.from("resource_name");
    } else {
      resourceName = getResourceTypeNameObject(fieldConfig.getMessageResourceNameConfig());
    }

    Name name = Name.from();
    if (requireIdentifier) {
      name = name.join(identifier);
    }
    if (requireAs) {
      name = name.join("as");
    }
    String functionName = publicMethodName(name.join(resourceName));
    if (requirePlural) {
      functionName += "s";
    }
    return functionName;
  }

  @Override
  public String getResourceNameFieldSetFunctionName(FieldConfig fieldConfig) {
    return getResourceNameFieldGetFunctionName(fieldConfig);
  }

  @Override
  public String getFieldGetFunctionName(FieldType field) {
    return privateMethodName(Name.from(field.getSimpleName()));
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
  protected Name getAnyResourceTypeName() {
    return Name.anyCamel("IResourceName");
  }

  private String getResourceTypeName(ModelTypeTable typeTable, FieldConfig resourceFieldConfig) {
    if (resourceFieldConfig.getResourceNameConfig() == null) {
      return typeTable.getAndSaveNicknameForElementType(resourceFieldConfig.getField());
    } else {
      return getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    }
  }

  @Override
  public String getFormatFunctionName(
      Interface apiInterface, SingleResourceNameConfig resourceNameConfig) {
    return getResourceTypeName(resourceNameConfig);
  }

  @Override
  public String getResourceEnumName(ResourceNameConfig resourceNameConfig) {
    return getResourceTypeNameObject(resourceNameConfig).toUpperCamel();
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      Method method, ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    ModelTypeTable modelTypeTable = (ModelTypeTable) typeTable;

    String inputTypeName = modelTypeTable.getAndSaveNicknameForElementType(method.getInputType());
    String outputTypeName = modelTypeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = getResourceTypeName(modelTypeTable, resourceFieldConfig);
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedEnumerable", inputTypeName, outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveAsyncPagedResponseTypeName(
      Method method, ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    ModelTypeTable modelTypeTable = (ModelTypeTable) typeTable;

    String inputTypeName = modelTypeTable.getAndSaveNicknameForElementType(method.getInputType());
    String outputTypeName = modelTypeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = getResourceTypeName(modelTypeTable, resourceFieldConfig);
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedAsyncEnumerable", inputTypeName, outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveCallerPagedResponseTypeName(
      Method method, ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    ModelTypeTable modelTypeTable = (ModelTypeTable) typeTable;

    String outputTypeName = modelTypeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = getResourceTypeName(modelTypeTable, resourceFieldConfig);
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedEnumerable", outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveCallerAsyncPagedResponseTypeName(
      Method method, ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    ModelTypeTable modelTypeTable = (ModelTypeTable) typeTable;

    String outputTypeName = modelTypeTable.getAndSaveNicknameForElementType(method.getOutputType());
    String resourceTypeName = getResourceTypeName(modelTypeTable, resourceFieldConfig);
    return modelTypeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedAsyncEnumerable", outputTypeName, resourceTypeName);
  }

  @Override
  public String getGrpcContainerTypeName(Interface apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName()));
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
  public String getGrpcServiceClassName(Interface apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName()))
        + "."
        + publicClassName(Name.upperCamel(apiInterface.getSimpleName(), "Client"));
  }

  @Override
  public String getApiWrapperClassImplName(Interface apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName(), "ClientImpl"));
  }

  @Override
  public String getPageStreamingDescriptorConstName(Method method) {
    return inittedConstantName(Name.upperCamel(method.getSimpleName()));
  }

  private Name addId(Name name) {
    if (name.toUpperCamel().endsWith("Id")) {
      return name;
    } else {
      return name.join("id");
    }
  }

  @Override
  public String getParamName(String var) {
    return localVarName(addId(Name.from(var)));
  }

  @Override
  public String getPropertyName(String var) {
    return publicMethodName(addId(Name.from(var)));
  }

  @Override
  public String getParamDocName(String var) {
    // 'super' to prevent '@' being prefixed to keywords
    String name = super.localVarName(Name.from(var));
    // Remove "id" suffix if present, as the C# code template always adds an ID suffix.
    if (name.toLowerCase().endsWith("id")) {
      return name.substring(0, name.length() - 2);
    } else {
      return name;
    }
  }

  @Override
  public String getFieldSetFunctionName(TypeRef type, Name identifier) {
    return publicMethodName(identifier);
  }

  @Override
  public String getAndSaveOperationResponseTypeName(
      Method method, ImportTypeTable typeTable, MethodConfig methodConfig) {
    // TODO(andrealin): Remove casting.
    String responseTypeName =
        ((ModelTypeTable) typeTable)
            .getFullNameFor(methodConfig.getLongRunningConfig().getReturnType());
    String metaTypeName =
        ((ModelTypeTable) typeTable)
            .getFullNameFor(methodConfig.getLongRunningConfig().getMetadataType());
    return typeTable.getAndSaveNicknameForContainer(
        "Google.LongRunning.Operation", responseTypeName, metaTypeName);
  }

  @Override
  public String getGrpcStreamingApiReturnTypeName(Method method, ImportTypeTable typeTable) {
    if (method.getRequestStreaming() && method.getResponseStreaming()) {
      // Bidirectional streaming
      return typeTable.getAndSaveNicknameForContainer(
          "Grpc.Core.AsyncDuplexStreamingCall",
          ((ModelTypeTable) typeTable).getFullNameFor(method.getInputType()),
          ((ModelTypeTable) typeTable).getFullNameFor(method.getOutputType()));
    } else if (method.getRequestStreaming()) {
      // Client streaming
      return typeTable.getAndSaveNicknameForContainer(
          "Grpc.Core.AsyncClientStreamingCall",
          ((ModelTypeTable) typeTable).getFullNameFor(method.getInputType()),
          ((ModelTypeTable) typeTable).getFullNameFor(method.getOutputType()));
    } else if (method.getResponseStreaming()) {
      // Server streaming
      return typeTable.getAndSaveNicknameForContainer(
          "Grpc.Core.AsyncServerStreamingCall",
          ((ModelTypeTable) typeTable).getFullNameFor(method.getOutputType()));
    } else {
      throw new IllegalArgumentException("Expected some sort of streaming here.");
    }
  }

  @Override
  public List<String> getReturnDocLines(
      InterfaceContext context, MethodConfig methodConfig, Synchronicity synchronicity) {
    if (methodConfig.isPageStreaming()) {
      FieldType resourceType = methodConfig.getPageStreaming().getResourcesField();
      String resourceTypeName =
          context.getImportTypeTable().getAndSaveNicknameForElementType(resourceType);
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
    } else if (methodConfig.isGrpcStreaming()) {
      return ImmutableList.of("The client-server stream.");
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

  @Override
  public String getResourceOneofCreateMethod(ImportTypeTable typeTable, FieldConfig fieldConfig) {
    String result = super.getResourceOneofCreateMethod(typeTable, fieldConfig);
    return result.replaceFirst("IEnumerable<(\\w*)>(\\..*)", "$1$2");
  }

  @Override
  public String makePrimitiveTypeNullable(String typeName, FieldType type) {
    return isPrimitive(type) ? typeName + "?" : typeName;
  }

  @Override
  public boolean isPrimitive(TypeRef type) {
    if (type.isRepeated()) {
      return false;
    }
    switch (type.getKind()) {
      case TYPE_BOOL:
      case TYPE_DOUBLE:
      case TYPE_ENUM:
      case TYPE_FIXED32:
      case TYPE_FIXED64:
      case TYPE_FLOAT:
      case TYPE_INT32:
      case TYPE_INT64:
      case TYPE_SFIXED32:
      case TYPE_SFIXED64:
      case TYPE_SINT32:
      case TYPE_SINT64:
      case TYPE_UINT32:
      case TYPE_UINT64:
        return true;
      default:
        return false;
    }
  }

  @Override
  public boolean isPrimitive(FieldType type) {
    return isPrimitive(type.getProtoTypeRef());
  }

  @Override
  public String getOptionalFieldDefaultValue(FieldConfig fieldConfig, GapicMethodContext context) {
    // Need to provide defaults for primitives, enums, strings, and repeated (including maps)
    FieldType type = fieldConfig.getField();
    if (context.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
      if (type.isRepeated()) {
        TypeName elementTypeName =
            new TypeName(
                getResourceTypeNameObject(fieldConfig.getResourceNameConfig()).toUpperCamel());
        TypeNameConverter typeNameConverter = getTypeNameConverter();
        TypeName enumerableTypeName = typeNameConverter.getTypeName("System.Linq.Enumerable");
        TypeName emptyTypeName =
            new TypeName(
                enumerableTypeName.getFullName(),
                enumerableTypeName.getNickname(),
                "%s.Empty<%i>",
                elementTypeName);
        return TypedValue.create(emptyTypeName, "%s()")
            .getValueAndSaveTypeNicknameIn((CSharpTypeTable) typeNameConverter);
      } else {
        return null;
      }
    } else {
      if (type.isPrimitive() || type.isEnum() || type.isRepeated()) {
        return context.getTypeTable().getImplZeroValueAndSaveNicknameFor(type);
      } else {
        return null;
      }
    }
  }
}
