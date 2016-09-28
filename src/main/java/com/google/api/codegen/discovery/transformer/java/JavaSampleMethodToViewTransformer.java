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

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.google.api.codegen.discovery.config.AuthType;
import com.google.api.codegen.discovery.config.FieldInfo;
import com.google.api.codegen.discovery.config.MethodInfo;
import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.config.TypeInfo;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.viewmodel.SampleBodyView;
import com.google.api.codegen.discovery.viewmodel.SampleFieldView;
import com.google.api.codegen.discovery.viewmodel.SampleView;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Method;

/*
 * Transforms a Method and SampleConfig into the standard discovery surface for
 * Java.
 */
public class JavaSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private final static String TEMPLATE_FILENAME = "java/sample.snip";

  public JavaSampleMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, SampleConfig sampleConfig) {
    SampleTypeTable sampleTypeTable =
        new SampleTypeTable(
            new JavaTypeTable(""), new JavaSampleTypeNameConverter(sampleConfig.packagePrefix()));
    JavaSampleNamer namer = new JavaSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(sampleConfig, sampleTypeTable, namer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    addStaticImports(context);
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable typeTable = context.getTypeTable();

    SampleBodyView body = createSampleBody(context);
    // Imports must be collected last.
    List<String> imports = new ArrayList<String>();
    imports.addAll(typeTable.getImports().keySet());
    return SampleView.newBuilder()
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.java")
        .apiTitle(sampleConfig.apiTitle())
        .apiName(sampleConfig.apiName())
        .apiVersion(sampleConfig.apiVersion())
        .className(context.getSampleNamer().getSampleClassName(sampleConfig.apiTypeName()))
        .body(body)
        .imports(imports)
        .build();
  }

  public SampleBodyView createSampleBody(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    MethodInfo methodInfo = sampleConfig.methods().get(context.getMethodName());
    SampleNamer sampleNamer = context.getSampleNamer();
    SampleTypeTable sampleTypeTable = context.getTypeTable();
    SymbolTable symbolTable = new SymbolTable();

    SampleBodyView.Builder sampleBodyView = SampleBodyView.newBuilder();
    sampleBodyView.serviceVarName(symbolTable.getNewSymbol(sampleNamer.getServiceVarName()));
    sampleBodyView.serviceTypeName(
        sampleTypeTable.getAndSaveNicknameForServiceType(sampleConfig.apiTypeName()));
    sampleBodyView.methodNameComponents(methodInfo.nameComponents());
    sampleBodyView.requestVarName(symbolTable.getNewSymbol(sampleNamer.getRequestVarName()));
    sampleBodyView.requestTypeName(
        sampleTypeTable.getAndSaveNicknameForRequestType(
            sampleConfig.apiTypeName(), methodInfo.requestType()));

    sampleBodyView.requestBodyVarName("");
    sampleBodyView.requestBodyTypeName("");
    sampleBodyView.resourceGetterName("");
    sampleBodyView.resourceTypeName("");
    sampleBodyView.isResourceMap(false);

    if (methodInfo.isPageStreaming()) {
      FieldInfo fieldInfo = methodInfo.pageStreamingResourceField();
      if (fieldInfo == null) {
        throw new IllegalArgumentException(
            "method is page streaming, but the page streaming resource field is null.");
      }
      sampleBodyView.resourceGetterName(sampleNamer.getResourceGetterName(fieldInfo.name()));
      sampleBodyView.resourceTypeName(
          sampleTypeTable.getAndSaveNickNameForElementType(fieldInfo.type()));
      sampleBodyView.isResourceMap(fieldInfo.type().isMap());
    }

    sampleBodyView.responseVarName("");
    sampleBodyView.responseTypeName("");
    sampleBodyView.hasOutput(methodInfo.responseType() != null);
    if (methodInfo.responseType() != null) {
      sampleBodyView.responseVarName(symbolTable.getNewSymbol(sampleNamer.getResponseVarName()));
      sampleBodyView.responseTypeName(
          sampleTypeTable.getAndSaveNicknameFor(methodInfo.responseType()));
    }

    List<SampleFieldView> fields = new ArrayList<>();
    List<String> fieldVarNames = new ArrayList<>();
    for (Entry<String, FieldInfo> field : methodInfo.fields().entrySet()) {
      SampleFieldView sampleFieldView = generateSampleField(field, sampleTypeTable, symbolTable);
      fields.add(sampleFieldView);
      fieldVarNames.add(sampleFieldView.name());
    }
    sampleBodyView.fields(fields);

    sampleBodyView.hasInputRequest(methodInfo.requestBodyType() != null);
    if (methodInfo.requestBodyType() != null) {
      String requestBodyVarName = symbolTable.getNewSymbol(sampleNamer.getRequestBodyVarName());
      sampleBodyView.requestBodyVarName(requestBodyVarName);
      sampleBodyView.requestBodyTypeName(
          sampleTypeTable.getAndSaveNicknameFor(methodInfo.requestBodyType()));
      fieldVarNames.add(requestBodyVarName);
    }
    sampleBodyView.fieldVarNames(fieldVarNames);
    sampleBodyView.isPageStreaming(methodInfo.isPageStreaming());

    sampleBodyView.authType(sampleConfig.authType());
    sampleBodyView.authInstructionsUrl(sampleConfig.authInstructionsUrl());
    sampleBodyView.authScopes(methodInfo.authScopes());
    return sampleBodyView.build();
  }

  public SampleFieldView generateSampleField(
      Entry<String, FieldInfo> field, SampleTypeTable sampleTypeTable, SymbolTable symbolTable) {
    TypeInfo typeInfo = field.getValue().type();
    return SampleFieldView.newBuilder()
        .name(symbolTable.getNewSymbol(field.getKey()))
        .typeName(sampleTypeTable.getAndSaveNicknameFor(typeInfo))
        .defaultValue(sampleTypeTable.getZeroValueAndSaveNicknameFor(typeInfo))
        .description(field.getValue().description())
        .build();
  }

  private void addStaticImports(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable typeTable = context.getTypeTable();
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
