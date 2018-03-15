/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelTypeFormatterImpl;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeName;
import com.google.api.codegen.util.TypeNameConverter;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.csharp.CSharpCommentReformatter;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
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
        packageName,
        packageName);
  }

  @Override
  public SurfaceNamer cloneWithPackageName(String packageName) {
    return new CSharpSurfaceNamer(packageName);
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
  public String injectRandomStringGeneratorCode(String randomString) {
    String delimiter = ",";
    String[] split =
        CommonRenderingUtil.stripQuotes(randomString)
            .replace(
                InitFieldConfig.RANDOM_TOKEN, delimiter + InitFieldConfig.RANDOM_TOKEN + delimiter)
            .split(delimiter);
    StringBuilder result = new StringBuilder();
    result.append('"');
    boolean requireInterpolation = false;
    for (String token : split) {
      if (token.length() > 0) {
        if (token.equals(InitFieldConfig.RANDOM_TOKEN)) {
          requireInterpolation = true;
          result.append("{Guid.NewGuid()}");
        } else {
          result.append(token);
        }
      }
    }
    result.append('"');
    return requireInterpolation ? "$" + result.toString() : result.toString();
  }

  @Override
  public String getStaticLangReturnTypeName(MethodContext methodContext) {
    MethodModel method = methodContext.getMethodModel();
    if (method.isOutputTypeEmpty()) {
      return "void";
    }
    return method.getOutputTypeName(methodContext.getTypeTable()).getFullName();
  }

  @Override
  public String getStaticLangAsyncReturnTypeName(MethodContext methodContext) {
    MethodModel method = methodContext.getMethodModel();
    if (method.isOutputTypeEmpty()) {
      return "System.Threading.Tasks.Task";
    }
    return "System.Threading.Tasks.Task<"
        + method.getOutputTypeName(methodContext.getTypeTable()).getFullName()
        + ">";
  }

  @Override
  public String getStaticLangCallerAsyncReturnTypeName(MethodContext methodContext) {
    // Same as sync because of 'await'
    return getStaticLangReturnTypeName(methodContext);
  }

  @Override
  public String getApiSnippetsClassName(InterfaceConfig interfaceConfig) {
    return publicClassName(
        Name.anyCamel("Generated", getInterfaceName(interfaceConfig), "ClientSnippets"));
  }

  @Override
  public String getCallableName(MethodModel method) {
    return privateFieldName(Name.upperCamel("Call", method.getSimpleName()));
  }

  @Override
  public String getModifyMethodName(MethodContext methodContext) {
    return "Modify_"
        + privateMethodName(
            Name.upperCamel(
                methodContext
                    .getMethodModel()
                    .getInputTypeName(methodContext.getTypeTable())
                    .getNickname()));
  }

  @Override
  public String getPathTemplateName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return inittedConstantName(Name.from(resourceNameConfig.getEntityName(), "template"));
  }

  @Override
  public String getResourceNameFieldGetFunctionName(FieldConfig fieldConfig) {
    FieldModel type = fieldConfig.getField();
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
  public String getFieldGetFunctionName(FieldModel field) {
    return privateMethodName(Name.from(field.getSimpleName()));
  }

  @Override
  public String getFieldGetFunctionName(FieldModel type, Name identifier) {
    return privateMethodName(Name.from(type.getSimpleName()));
  }

  @Override
  public String getExamplePackageName() {
    return getPackageName() + ".Snippets";
  }

  @Override
  protected Name getAnyResourceTypeName() {
    return Name.anyCamel("IResourceName");
  }

  private String getResourceTypeName(ImportTypeTable typeTable, FieldConfig resourceFieldConfig) {
    if (resourceFieldConfig.getResourceNameConfig() == null) {
      return typeTable.getAndSaveNicknameForElementType(resourceFieldConfig.getField());
    } else {
      return getAndSaveElementResourceTypeName(typeTable, resourceFieldConfig);
    }
  }

  @Override
  public String getFormatFunctionName(
      InterfaceConfig interfaceConfig, SingleResourceNameConfig resourceNameConfig) {
    return getResourceTypeName(resourceNameConfig);
  }

  @Override
  public String getResourceEnumName(ResourceNameConfig resourceNameConfig) {
    return getResourceTypeNameObject(resourceNameConfig).toUpperCamel();
  }

  @Override
  public String getAndSavePagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourceFieldConfig) {
    ImportTypeTable typeTable = methodContext.getTypeTable();
    String inputTypeName =
        methodContext
            .getMethodModel()
            .getAndSaveRequestTypeName(methodContext.getTypeTable(), methodContext.getNamer());
    String outputTypeName =
        methodContext
            .getMethodModel()
            .getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
    String resourceTypeName = getResourceTypeName(typeTable, resourceFieldConfig);
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedEnumerable", inputTypeName, outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveAsyncPagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourceFieldConfig) {
    ImportTypeTable typeTable = methodContext.getTypeTable();
    String inputTypeName =
        methodContext
            .getMethodModel()
            .getAndSaveRequestTypeName(methodContext.getTypeTable(), methodContext.getNamer());
    String outputTypeName =
        methodContext
            .getMethodModel()
            .getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
    String resourceTypeName = getResourceTypeName(typeTable, resourceFieldConfig);
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedAsyncEnumerable", inputTypeName, outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveCallerPagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourceFieldConfig) {
    ImportTypeTable typeTable = methodContext.getTypeTable();
    String outputTypeName =
        methodContext
            .getMethodModel()
            .getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
    String resourceTypeName = getResourceTypeName(typeTable, resourceFieldConfig);
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedEnumerable", outputTypeName, resourceTypeName);
  }

  @Override
  public String getAndSaveCallerAsyncPagedResponseTypeName(
      MethodContext methodContext, FieldConfig resourceFieldConfig) {
    ImportTypeTable typeTable = methodContext.getTypeTable();
    String outputTypeName =
        methodContext
            .getMethodModel()
            .getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
    String resourceTypeName = getResourceTypeName(typeTable, resourceFieldConfig);
    return typeTable.getAndSaveNicknameForContainer(
        "Google.Api.Gax.PagedAsyncEnumerable", outputTypeName, resourceTypeName);
  }

  @Override
  public String getGrpcContainerTypeName(InterfaceModel apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName()));
  }

  @Override
  public String getReroutedGrpcClientVarName(MethodConfig methodConfig) {
    String reroute = methodConfig.getRerouteToGrpcInterface();
    if (reroute == null) {
      return "GrpcClient";
    } else {
      List<String> reroutes = Splitter.on('.').splitToList(reroute);
      return Name.anyCamelKeepUpperAcronyms("grpc", reroutes.get(reroutes.size() - 1), "client")
          .toLowerCamel();
    }
  }

  @Override
  public String getReroutedGrpcMethodName(MethodConfig methodConfig) {
    List<String> reroutes = Splitter.on('.').splitToList(methodConfig.getRerouteToGrpcInterface());
    return Name.anyCamelKeepUpperAcronyms("create", reroutes.get(reroutes.size() - 1), "client")
        .toUpperCamel();
  }

  @Override
  public String getReroutedGrpcTypeName(ImportTypeTable typeTable, MethodConfig methodConfig) {
    List<String> reroutes = Splitter.on('.').splitToList(methodConfig.getRerouteToGrpcInterface());
    if (reroutes.size() > 2
        && reroutes.get(0).equals("google")
        && !reroutes.get(1).equals("cloud")) {
      reroutes = new ArrayList<String>(reroutes);
      reroutes.add(1, "cloud");
    }
    String rerouteLast = reroutes.get(reroutes.size() - 1);
    String name =
        Name.anyCamelKeepUpperAcronyms(rerouteLast).toUpperCamel()
            + "+"
            + Name.anyCamelKeepUpperAcronyms(rerouteLast, "client").toUpperCamel();
    List<String> names = new ArrayList<>();
    for (String reroute : reroutes) {
      names.add(Name.anyCamelKeepUpperAcronyms(reroute).toUpperCamel());
    }
    String prefix = Joiner.on(".").join(names.subList(0, names.size() - 1));
    String fullName = prefix + "." + name;
    return typeTable.getAndSaveNicknameFor(fullName);
  }

  @Override
  public String getGrpcServiceClassName(InterfaceModel apiInterface) {
    return publicClassName(Name.upperCamel(apiInterface.getSimpleName()))
        + "."
        + publicClassName(Name.upperCamel(apiInterface.getSimpleName(), "Client"));
  }

  @Override
  public String getApiWrapperClassImplName(InterfaceConfig interfaceConfig) {
    return publicClassName(Name.upperCamel(getInterfaceName(interfaceConfig), "ClientImpl"));
  }

  @Override
  public String getPageStreamingDescriptorConstName(MethodModel method) {
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
  public String getFieldSetFunctionName(TypeModel type, Name identifier) {
    return publicMethodName(identifier);
  }

  @Override
  public String getAndSaveOperationResponseTypeName(
      MethodModel method, ImportTypeTable typeTable, MethodConfig methodConfig) {
    String responseTypeName =
        typeTable.getFullNameFor(methodConfig.getLongRunningConfig().getReturnType());
    String metaTypeName =
        typeTable.getFullNameFor(methodConfig.getLongRunningConfig().getMetadataType());
    return typeTable.getAndSaveNicknameForContainer(
        "Google.LongRunning.Operation", responseTypeName, metaTypeName);
  }

  @Override
  public String getAsyncGrpcMethodName(MethodModel method) {
    return getGrpcMethodName(method) + "Async";
  }

  @Override
  public String getGrpcStreamingApiReturnTypeName(
      MethodContext methodContext, ImportTypeTable typeTable) {
    MethodModel method = methodContext.getMethodModel();
    if (method.getRequestStreaming() && method.getResponseStreaming()) {
      // Bidirectional streaming
      return typeTable.getAndSaveNicknameForContainer(
          "Grpc.Core.AsyncDuplexStreamingCall",
          method.getAndSaveRequestTypeName(typeTable, methodContext.getNamer()),
          method.getAndSaveResponseTypeName(typeTable, methodContext.getNamer()));
    } else if (method.getRequestStreaming()) {
      // Client streaming
      return typeTable.getAndSaveNicknameForContainer(
          "Grpc.Core.AsyncClientStreamingCall",
          method.getAndSaveRequestTypeName(typeTable, methodContext.getNamer()),
          method.getAndSaveResponseTypeName(typeTable, methodContext.getNamer()));
    } else if (method.getResponseStreaming()) {
      // Server streaming
      return typeTable.getAndSaveNicknameForContainer(
          "Grpc.Core.AsyncServerStreamingCall",
          method.getOutputTypeName(methodContext.getTypeTable()).getFullName());
    } else {
      throw new IllegalArgumentException("Expected some sort of streaming here.");
    }
  }

  @Override
  public List<String> getReturnDocLines(
      TransformationContext context, MethodContext methodContext, Synchronicity synchronicity) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    if (methodConfig.isPageStreaming()) {
      FieldModel resourceType = methodConfig.getPageStreaming().getResourcesField();
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
      switch (methodConfig.getGrpcStreamingType()) {
        case ServerStreaming:
          return ImmutableList.of("The server stream.");
        case BidiStreaming:
          return ImmutableList.of("The client-server stream.");
        default:
          throw new IllegalStateException(
              "Invalid streaming: " + methodConfig.getGrpcStreamingType());
      }
    } else {
      boolean hasReturn = !methodConfig.getMethodModel().isOutputTypeEmpty();
      switch (synchronicity) {
        case Sync:
          return hasReturn ? ImmutableList.of("The RPC response.") : ImmutableList.of();
        case Async:
          return ImmutableList.of(
              hasReturn
                  ? "A Task containing the RPC response."
                  : "A Task that completes when the RPC has completed.");
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
  public String makePrimitiveTypeNullable(String typeName, FieldModel type) {
    return isPrimitive(type) ? typeName + "?" : typeName;
  }

  @Override
  public boolean isPrimitive(TypeModel typeModel) {
    if (!(typeModel instanceof ProtoTypeRef)) {
      return typeModel.isPrimitive();
    }
    TypeRef type = ((ProtoTypeRef) typeModel).getProtoType();
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
  public boolean isPrimitive(FieldModel type) {
    return isPrimitive(type.getType());
  }

  @Override
  public String getOptionalFieldDefaultValue(FieldConfig fieldConfig, MethodContext context) {
    // Need to provide defaults for primitives, enums, strings, and repeated (including maps)
    FieldModel type = fieldConfig.getField();
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

  /** The test case name for the given method. */
  @Override
  public String getTestCaseName(SymbolTable symbolTable, MethodModel method) {
    Name testCaseName = symbolTable.getNewSymbol(method.asName());
    return publicMethodName(testCaseName);
  }

  @Override
  public String getAsyncTestCaseName(SymbolTable symbolTable, MethodModel method) {
    Name testCaseName = symbolTable.getNewSymbol(method.asName().join("async"));
    return publicMethodName(testCaseName);
  }
}
