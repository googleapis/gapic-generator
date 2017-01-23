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
package com.google.api.codegen.discovery.transformer.java;

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
import com.google.api.codegen.transformer.StandardImportTypeTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

/*
 * Transforms a Method and SampleConfig into the standard discovery surface for
 * Java.
 */
public class JavaSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "java/sample.snip";

  private final StandardImportTypeTransformer importTypeTransformer =
      new StandardImportTypeTransformer();

  @Override
  public ViewModel transform(Method method, SampleConfig sampleConfig) {
    SampleTypeTable sampleTypeTable =
        new SampleTypeTable(
            new JavaTypeTable(""),
            new JavaSampleTypeNameConverter(
                sampleConfig.apiName(), sampleConfig.apiVersion(), sampleConfig.versionModule()));
    JavaSampleNamer javaSampleNamer = new JavaSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(
            sampleConfig, sampleTypeTable, javaSampleNamer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    addStaticImports(context);
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(JavaNameFormatter.RESERVED_IDENTIFIER_SET);

    SampleView.Builder builder = SampleView.newBuilder();

    String serviceVarName = symbolTable.getNewSymbol(namer.getServiceVarName(config.apiTypeName()));
    String serviceTypeName = typeTable.getAndSaveNicknameForServiceType(config.apiTypeName());
    String requestVarName = symbolTable.getNewSymbol(namer.getRequestVarName());
    // We don't store this type in the type table because its nickname is fully
    // qualified. If we use the getAndSaveNickname helper, the nickname returned
    // is always the last segment of the import path. Since the request type is
    // derived from the service type, skipping the type table can't cause any
    // issues.
    String requestTypeName =
        typeTable.getRequestTypeName(config.apiTypeName(), methodInfo.requestType()).getNickname();

    List<SampleFieldView> requiredFields = new ArrayList<>();
    List<SampleFieldView> optionalFields = new ArrayList<>();
    List<String> methodCallFieldVarNames = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView = createSampleFieldView(field, context, symbolTable);
      requiredFields.add(sampleFieldView);
      if (sampleFieldView.required()) {
        methodCallFieldVarNames.add(sampleFieldView.name());
      } else {
        optionalFields.add(sampleFieldView);
      }
    }

    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    List<SampleFieldView> requestBodyFields = new ArrayList<>();
    if (hasRequestBody) {
      String requestBodyVarName = symbolTable.getNewSymbol(namer.getRequestBodyVarName());
      builder.requestBodyVarName(requestBodyVarName);
      builder.requestBodyTypeName(typeTable.getAndSaveNicknameFor(methodInfo.requestBodyType()));
      methodCallFieldVarNames.add(requestBodyVarName);

      for (FieldInfo fieldInfo : methodInfo.requestBodyType().message().fields().values()) {
        requestBodyFields.add(createSampleFieldView(fieldInfo, context, symbolTable));
      }
    }

    if (methodInfo.isPageStreaming()) {
      builder.pageStreaming(createSamplePageStreamingView(context, symbolTable));
    }

    boolean hasResponse = methodInfo.responseType() != null;
    if (hasResponse) {
      builder.responseVarName(symbolTable.getNewSymbol(namer.getResponseVarName()));
      builder.responseTypeName(typeTable.getAndSaveNicknameFor(methodInfo.responseType()));
    }

    // Imports must be collected last.
    List<ImportTypeView> imports =
        new ArrayList<>(importTypeTransformer.generateImports(typeTable.getImports()));

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.java")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .appName(namer.getSampleApplicationName(config.apiCanonicalName()))
        .className(namer.getSampleClassName(config.apiTypeName()))
        .imports(imports)
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
        .createServiceFuncName(namer.createServiceFuncName(config.apiTypeName()))
        .build();
  }

  private SampleAuthView createSampleAuthView(SampleTransformerContext context) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());

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

    builder.resourceGetterName(namer.getResourceGetterName(fieldInfo.name()));
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
      FieldInfo field, SampleTransformerContext context, SymbolTable symbolTable) {
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();

    TypeInfo typeInfo = field.type();
    String defaultValue = typeTable.getZeroValueAndSaveNicknameFor(typeInfo);
    return SampleFieldView.newBuilder()
        .name(symbolTable.getNewSymbol(field.name()))
        .typeName(typeTable.getAndSaveNicknameFor(typeInfo))
        .defaultValue(defaultValue)
        .example(field.example())
        .description(field.description())
        .setterFuncName(namer.getRequestBodyFieldSetterName(field.name()))
        .required(field.required())
        .build();
  }

  private void addStaticImports(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    typeTable.saveNicknameFor("com.google.api.client.googleapis.auth.oauth2.GoogleCredential");
    typeTable.saveNicknameFor("com.google.api.client.googleapis.javanet.GoogleNetHttpTransport");
    typeTable.saveNicknameFor("com.google.api.client.http.HttpTransport");
    typeTable.saveNicknameFor("com.google.api.client.json.jackson2.JacksonFactory");
    typeTable.saveNicknameFor("com.google.api.client.json.JsonFactory");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.security.GeneralSecurityException");
    if (sampleConfig.authType() == AuthType.APPLICATION_DEFAULT_CREDENTIALS) {
      typeTable.saveNicknameFor("java.util.Arrays");
    }
  }
}
