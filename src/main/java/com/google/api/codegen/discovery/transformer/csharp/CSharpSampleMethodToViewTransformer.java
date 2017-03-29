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
package com.google.api.codegen.discovery.transformer.csharp;

import com.google.api.codegen.discovery.config.AuthType;
import com.google.api.codegen.discovery.config.FieldInfo;
import com.google.api.codegen.discovery.config.MethodInfo;
import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.viewmodel.SampleAuthView;
import com.google.api.codegen.discovery.viewmodel.SampleFieldView;
import com.google.api.codegen.discovery.viewmodel.SamplePageStreamingView;
import com.google.api.codegen.discovery.viewmodel.SampleView;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypedValue;
import com.google.api.codegen.util.csharp.CSharpNameFormatter;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Joiner;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

/*
 * Transforms a Method and SampleConfig into the standard discovery surface for
 * C#.
 */
public class CSharpSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "csharp/sample.snip";

  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();

  @Override
  public ViewModel transform(Method method, SampleConfig sampleConfig) {
    SampleTypeTable sampleTypeTable =
        new SampleTypeTable(
            new CSharpTypeTable(""),
            new CSharpSampleTypeNameConverter(
                sampleConfig.packagePrefix(),
                sampleConfig.methods().get(method.getName()).nameComponents()));
    CSharpSampleNamer csharpSampleNamer = new CSharpSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(
            sampleConfig, sampleTypeTable, csharpSampleNamer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    addStaticImports(context);
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(CSharpNameFormatter.RESERVED_IDENTIFIER_SET);

    SampleView.Builder builder = SampleView.newBuilder();

    String serviceVarName = symbolTable.getNewSymbol(namer.getServiceVarName(config.apiTypeName()));
    String serviceTypeName = typeTable.getAndSaveNicknameForServiceType(config.apiTypeName());
    String requestVarName = symbolTable.getNewSymbol(namer.getRequestVarName());
    String requestTypeName =
        typeTable.getAndSaveNicknameForRequestType(config.apiTypeName(), methodInfo.requestType());

    List<String> methodCallFieldVarNames = new ArrayList<>();
    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    List<SampleFieldView> requestBodyFields = new ArrayList<>();
    if (hasRequestBody) {
      String requestBodyVarName = symbolTable.getNewSymbol(namer.getRequestBodyVarName());
      builder.requestBodyVarName(requestBodyVarName);
      builder.requestBodyTypeName(typeTable.getAndSaveNicknameFor(methodInfo.requestBodyType()));
      methodCallFieldVarNames.add(requestBodyVarName);

      for (FieldInfo fieldInfo : methodInfo.requestBodyType().message().fields().values()) {
        requestBodyFields.add(createSampleFieldView(methodInfo, fieldInfo, context, symbolTable));
      }
    }

    List<SampleFieldView> requiredFields = new ArrayList<>();
    List<SampleFieldView> optionalFields = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView =
          createSampleFieldView(methodInfo, field, context, symbolTable);
      if (sampleFieldView.required()) {
        requiredFields.add(sampleFieldView);
        methodCallFieldVarNames.add(sampleFieldView.name());
      } else {
        optionalFields.add(sampleFieldView);
      }
    }

    // The page streaming view model is generated close to last to avoid taking naming precedence in
    // the symbol table.
    if (methodInfo.isPageStreaming()) {
      builder.pageStreaming(createSamplePageStreamingView(context, symbolTable));
    }

    boolean hasResponse = methodInfo.responseType() != null;
    if (hasResponse) {
      builder.responseVarName(symbolTable.getNewSymbol(namer.getResponseVarName()));
      builder.responseTypeName(typeTable.getAndSaveNicknameFor(methodInfo.responseType()));
      typeTable.saveNicknameFor("System.Console");
    }

    String dataNamespace = "";
    if (hasRequestBody || hasResponse) {
      dataNamespace = Joiner.on('.').join(config.packagePrefix(), "Data");
    }

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.cs")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .appName(namer.getSampleApplicationName(config.apiCanonicalName()))
        .className(namer.getSampleClassName(config.apiCanonicalName()))
        .auth(createSampleAuthView(context))
        .serviceVarName(serviceVarName)
        .serviceTypeName(serviceTypeName)
        .methodVerb(methodInfo.verb())
        .methodNameComponents(methodInfo.nameComponents())
        .requestVarName(requestVarName)
        .requestTypeName(requestTypeName)
        .hasRequestBody(hasRequestBody)
        .requestBodyFields(requestBodyFields)
        .hasResponse(hasResponse)
        .requiredFields(requiredFields)
        .optionalFields(optionalFields)
        .methodCallFieldVarNames(methodCallFieldVarNames)
        .isPageStreaming(methodInfo.isPageStreaming())
        .hasMediaUpload(methodInfo.hasMediaUpload())
        .hasMediaDownload(methodInfo.hasMediaDownload())
        .dataNamespace(dataNamespace)
        .namespaceName(CSharpSampleNamer.getNamespaceName(config.apiCanonicalName()))
        .importSection(importSectionTransformer.generateImportSection(typeTable.getImports()))
        .build();
  }

  private SampleAuthView createSampleAuthView(SampleTransformerContext context) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());

    if (config.authType() == AuthType.APPLICATION_DEFAULT_CREDENTIALS) {
      context.getSampleTypeTable().saveNicknameFor("Google.Apis.Auth.OAuth2.GoogleCredential");
      context.getSampleTypeTable().saveNicknameFor("System.Threading.Tasks.Task");
    }
    if (config.authType() == AuthType.OAUTH_3L) {
      context.getSampleTypeTable().saveNicknameFor("Google.Apis.Auth.OAuth2.UserCredential");
    }
    return SampleAuthView.newBuilder()
        .type(config.authType())
        .instructionsUrl(config.authInstructionsUrl())
        .scopes(methodInfo.authScopes())
        .isScopesSingular(methodInfo.authScopes().size() == 1)
        .build();
  }

  private SamplePageStreamingView createSamplePageStreamingView(
      SampleTransformerContext context, SymbolTable symbolTable) {
    MethodInfo methodInfo = context.getSampleConfig().methods().get(context.getMethodName());
    FieldInfo fieldInfo = methodInfo.pageStreamingResourceField();
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();

    SamplePageStreamingView.Builder builder = SamplePageStreamingView.newBuilder();

    builder.resourceFieldName(Name.lowerCamel(fieldInfo.name()).toUpperCamel());
    String resourceTypeName = typeTable.getAndSaveNickNameForElementType(fieldInfo.type());
    builder.resourceElementTypeName(resourceTypeName);
    String resourceVarName =
        namer.getResourceVarName(fieldInfo.type().isMessage() ? resourceTypeName : "");
    builder.resourceVarName(symbolTable.getNewSymbol(resourceVarName));
    builder.isResourceRepeated(fieldInfo.cardinality() == Cardinality.CARDINALITY_REPEATED);
    builder.isResourceMap(fieldInfo.type().isMap());

    builder.isResourceSetterInRequestBody(methodInfo.isPageStreamingResourceSetterInRequestBody());
    builder.pageTokenName(Name.lowerCamel(methodInfo.requestPageTokenName()).toUpperCamel());
    builder.nextPageTokenName(Name.lowerCamel(methodInfo.responsePageTokenName()).toUpperCamel());
    return builder.build();
  }

  private SampleFieldView createSampleFieldView(
      MethodInfo methodInfo,
      FieldInfo field,
      SampleTransformerContext context,
      SymbolTable symbolTable) {
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    TypeInfo typeInfo = field.type();

    String defaultValue = "";
    String typeName = "";
    // TODO(saicheems): Ugly hack to get around enum naming in C# for the time being.
    // Longer explanation in CSharpSampleTypeNameConverter.
    if (typeInfo.kind() == Field.Kind.TYPE_ENUM) {
      TypedValue typedValue =
          CSharpSampleTypeNameConverter.getEnumZeroValue(
              typeTable.getAndSaveNicknameForRequestType("", methodInfo.requestType()),
              field.name());
      typeName = typedValue.getTypeName().getNickname();
      defaultValue = String.format(typedValue.getValuePattern(), typeName);
    } else {
      defaultValue = typeTable.getZeroValueAndSaveNicknameFor(typeInfo);
      typeName = typeTable.getAndSaveNicknameFor(typeInfo);
    }
    return SampleFieldView.newBuilder()
        .name(symbolTable.getNewSymbol(field.name()))
        .typeName(typeName)
        .defaultValue(defaultValue)
        .example(field.example())
        .description(field.description())
        .setterFuncName(namer.getRequestBodyFieldSetterName(field.name()))
        .required(field.required())
        .build();
  }

  private void addStaticImports(SampleTransformerContext context) {
    context.getSampleTypeTable().saveNicknameFor("Google.Apis.Services.BaseClientService");
  }
}
