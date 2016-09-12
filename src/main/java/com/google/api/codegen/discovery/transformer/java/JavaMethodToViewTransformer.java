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

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.ApiaryConfigToSampleConfigConverter;
import com.google.api.codegen.discovery.FieldInfo;
import com.google.api.codegen.discovery.SampleConfig;
import com.google.api.codegen.discovery.TypeInfo;
import com.google.api.codegen.discovery.transformer.MethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.viewmodel.SampleBodyView;
import com.google.api.codegen.discovery.viewmodel.SampleFieldView;
import com.google.api.codegen.discovery.viewmodel.SampleView;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.io.Files;
import com.google.protobuf.Method;

/*
 * Transforms a Model into the standard discovery surface in Java.
 */
public class JavaMethodToViewTransformer implements MethodToViewTransformer {

  private final static String TEMPLATE_FILENAME = "java/discovery_fragment.snip";

  public JavaMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, SampleConfig sampleConfig) {
    SampleTypeTable sampleTypeTable =
        new SampleTypeTable(
            new JavaTypeTable(),
            new JavaProtobufTypeNameConverter(
                sampleConfig.apiNameVersion(),
                sampleConfig.apiTypeName(),
                sampleConfig.methodInfo().nameComponents()));
    JavaSampleNamer namer = new JavaSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(sampleConfig, sampleTypeTable, namer);
    return createSampleView(context, method.getName());
  }

  private SampleView createSampleView(SampleTransformerContext context, String methodName) {
    addStaticImports(context);
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable typeTable = context.getTypeTable();

    String apiNameVersion[] = sampleConfig.apiNameVersion().split("\\.", 2);

    return SampleView.newBuilder()
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(methodName + ".frag.java")
        .apiTitle(sampleConfig.apiTitle())
        .apiName(apiNameVersion[0])
        .apiVersion(apiNameVersion[1])
        .body(createSampleBody(context))
        .imports(typeTable.getImports())
        .build();
  }

  public SampleBodyView createSampleBody(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleNamer sampleNamer = context.getNamer();
    SampleTypeTable sampleTypeTable = context.getTypeTable();
    SymbolTable symbolTable = new SymbolTable();

    SampleBodyView.Builder sampleBodyView = SampleBodyView.newBuilder();
    sampleBodyView.serviceVarName(symbolTable.getNewSymbol(sampleNamer.getServiceVarName()));
    sampleBodyView.serviceTypeName(sampleTypeTable.getAndSaveNicknameFor(sampleConfig));
    sampleBodyView.methodNameComponents(sampleConfig.methodInfo().nameComponents());
    sampleBodyView.requestVarName(symbolTable.getNewSymbol("request"));
    sampleBodyView.requestTypeName(
        sampleTypeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().requestType()));

    sampleBodyView.requestBodyVarName("");
    sampleBodyView.requestBodyTypeName("");
    sampleBodyView.resourceGetterName("");
    sampleBodyView.resourceTypeName("");
    sampleBodyView.isResourceMap(false);

    if (sampleConfig.methodInfo().isPageStreaming()) {
      FieldInfo fieldInfo = sampleConfig.methodInfo().pageStreamingResourceField();
      // TODO(garrettjones): Should I form this name this way?
      sampleBodyView.resourceGetterName(Name.lowerCamel("get", fieldInfo.name()).toLowerCamel());
      String nickname = sampleTypeTable.getAndSaveNicknameFor(fieldInfo.type());
      sampleBodyView.resourceTypeName(nickname);
      // Rename the type from Map<K, V> to Map.Entry<K, V> to match what we
      // expect in the iterating for-loop.
      if (fieldInfo.type().isMap()) {
        sampleBodyView.resourceTypeName(sampleNamer.getMapEntryTypeFromMapType(nickname));
      }
      sampleBodyView.isResourceMap(fieldInfo.type().isMap());
    }

    sampleBodyView.responseVarName("");
    sampleBodyView.responseTypeName("");
    sampleBodyView.hasOutput(sampleConfig.methodInfo().responseType() != null);
    if (sampleConfig.methodInfo().responseType() != null) {
      sampleBodyView.responseVarName(symbolTable.getNewSymbol("response"));
      sampleBodyView.responseTypeName(
          sampleTypeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().responseType()));
    }

    List<SampleFieldView> fields = new ArrayList<>();
    List<String> fieldVarNames = new ArrayList<>();
    for (Entry<String, FieldInfo> field : sampleConfig.methodInfo().fields().entrySet()) {
      SampleFieldView sampleFieldView = generateSampleField(field, sampleTypeTable, symbolTable);
      fields.add(sampleFieldView);
      fieldVarNames.add(sampleFieldView.name());
    }
    sampleBodyView.fields(fields);

    sampleBodyView.hasInputRequest(sampleConfig.methodInfo().requestBodyType() != null);
    if (sampleConfig.methodInfo().requestBodyType() != null) {
      String requestBodyVarName = symbolTable.getNewSymbol("requestBody");
      sampleBodyView.requestBodyVarName(requestBodyVarName);
      sampleBodyView.requestBodyTypeName(
          sampleTypeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().requestBodyType()));
      fieldVarNames.add(requestBodyVarName);
    }
    sampleBodyView.fieldVarNames(fieldVarNames);
    sampleBodyView.isPageStreaming(sampleConfig.methodInfo().isPageStreaming());

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
    SampleTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.client.googleapis.auth.oauth2.GoogleCredential");
    typeTable.saveNicknameFor("com.google.api.client.googleapis.javanet.GoogleNetHttpTransport");
    typeTable.saveNicknameFor("com.google.api.client.http.HttpTransport");
    typeTable.saveNicknameFor("com.google.api.client.json.jackson2.JacksonFactory");
    typeTable.saveNicknameFor("com.google.api.client.json.JsonFactory");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.security.GeneralSecurityException");
    typeTable.saveNicknameFor("java.util.Arrays");
  }
}
