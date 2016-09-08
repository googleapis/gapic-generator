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

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.ApiaryConfigToSampleConfigConverter;
import com.google.api.codegen.discovery.FieldInfo;
import com.google.api.codegen.discovery.SampleConfig;
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
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

/*
 * Transforms a Model into the standard discovery surface in Java.
 */
public class JavaMethodToViewTransformer implements MethodToViewTransformer {

  private final static String TEMPLATE_FILENAME = "java/discovery_fragment.snip";

  public JavaMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, ApiaryConfig apiaryConfig) {
    SampleConfig sampleConfig = ApiaryConfigToSampleConfigConverter.convert(method, apiaryConfig);
    JavaSampleNamer namer = new JavaSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(sampleConfig, createTypeTable(), namer);
    return getSample(context);
  }

  /*
   * Returns a new Java TypeTable.
   */
  private SampleTypeTable createTypeTable() {
    return new SampleTypeTable(new JavaTypeTable(), new JavaProtobufTypeNameConverter());
  }

  private SampleView getSample(SampleTransformerContext context) {
    addStaticImports(context);
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable typeTable = context.getTypeTable();

    SampleView.Builder sampleView = SampleView.newBuilder();
    sampleView.templateFileName(TEMPLATE_FILENAME);
    sampleView.outputPath(sampleConfig.methodInfo().name() + ".frag.java");
    sampleView.apiTitle(sampleConfig.apiTitle());
    sampleView.apiName(sampleConfig.apiName());
    sampleView.apiVersion(sampleConfig.apiVersion());
    sampleView.body(generateSampleBody(context));
    sampleView.imports(typeTable.getImports());

    return sampleView.build();
  }

  public SampleBodyView generateSampleBody(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleNamer sampleNamer = context.getNamer();
    SampleTypeTable sampleTypeTable = context.getTypeTable();
    SymbolTable symbolTable = new SymbolTable();

    SampleBodyView.Builder sampleBodyView = SampleBodyView.newBuilder();
    sampleBodyView.serviceVarName(symbolTable.getNewSymbol(sampleNamer.getServiceVarName()));
    sampleBodyView.serviceTypeName(sampleTypeTable.getAndSaveNicknameFor(sampleConfig));
    sampleBodyView.resources(sampleConfig.methodInfo().resources());
    sampleBodyView.inputVarName(symbolTable.getNewSymbol("request"));
    sampleBodyView.inputTypeName(
        sampleTypeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().inputType()));

    sampleBodyView.inputRequestVarName("");
    sampleBodyView.inputRequestTypeName("");
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

    sampleBodyView.outputVarName("");
    sampleBodyView.outputTypeName("");
    sampleBodyView.hasOutput(sampleConfig.methodInfo().outputType() != null);
    if (sampleConfig.methodInfo().outputType() != null) {
      sampleBodyView.outputVarName(symbolTable.getNewSymbol("response"));
      sampleBodyView.outputTypeName(
          sampleTypeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().outputType()));
    }

    List<SampleFieldView> fields = new ArrayList<>();
    List<String> fieldVarNames = new ArrayList<>();
    for (FieldInfo fieldInfo : sampleConfig.methodInfo().fields()) {
      SampleFieldView sampleFieldView =
          generateSampleField(fieldInfo, sampleTypeTable, symbolTable);
      fields.add(sampleFieldView);
      fieldVarNames.add(sampleFieldView.name());
    }
    sampleBodyView.fields(fields);

    sampleBodyView.hasInputRequest(sampleConfig.methodInfo().inputRequestType() != null);
    if (sampleConfig.methodInfo().inputRequestType() != null) {
      String inputRequestVarName = symbolTable.getNewSymbol("requestBody");
      sampleBodyView.inputRequestVarName(inputRequestVarName);
      System.out.println(sampleConfig.methodInfo().name());
      sampleBodyView.inputRequestTypeName(
          sampleTypeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().inputRequestType()));
      fieldVarNames.add(inputRequestVarName);
    }
    sampleBodyView.fieldVarNames(fieldVarNames);
    sampleBodyView.isPageStreaming(sampleConfig.methodInfo().isPageStreaming());

    return sampleBodyView.build();
  }

  public SampleFieldView generateSampleField(
      FieldInfo fieldInfo, SampleTypeTable sampleTypeTable, SymbolTable symbolTable) {
    return SampleFieldView.newBuilder()
        .name(symbolTable.getNewSymbol(fieldInfo.name()))
        .typeName(sampleTypeTable.getAndSaveNicknameFor(fieldInfo.type()))
        .defaultValue(sampleTypeTable.getZeroValueAndSaveNicknameFor(fieldInfo.type()))
        .description(fieldInfo.description())
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
