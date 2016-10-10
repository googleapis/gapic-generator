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
package com.google.api.codegen.discovery.transformer.nodejs;

import java.util.ArrayList;
import java.util.List;

import com.google.api.codegen.discovery.config.FieldInfo;
import com.google.api.codegen.discovery.config.MethodInfo;
import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.viewmodel.SampleBodyView;
import com.google.api.codegen.discovery.viewmodel.SampleFieldView;
import com.google.api.codegen.discovery.viewmodel.SampleView;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.nodejs.NodeJSTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Method;

public class NodeJSSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private final static String TEMPLATE_FILENAME = "nodejs/sample.snip";

  @Override
  public ViewModel transform(Method method, SampleConfig sampleConfig) {
    SampleTypeTable sampleTypeTable =
        new SampleTypeTable(new NodeJSTypeTable(""), new NodeJSSampleTypeNameConverter());
    NodeJSSampleNamer nodeJsSampleNamer = new NodeJSSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(
            sampleConfig, sampleTypeTable, nodeJsSampleNamer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleBodyView sampleBodyView = createSampleBodyView(context);
    // Imports must be collected last.
    List<String> imports = new ArrayList<String>();
    return SampleView.newBuilder()
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.njs")
        .apiTitle(sampleConfig.apiTitle())
        .apiName(sampleConfig.apiName())
        .apiVersion(sampleConfig.apiVersion())
        .className("")
        .body(sampleBodyView)
        .imports(imports)
        .build();
  }

  private SampleBodyView createSampleBodyView(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    MethodInfo methodInfo = sampleConfig.methods().get(context.getMethodName());
    SampleNamer sampleNamer = context.getSampleNamer();
    SampleTypeTable sampleTypeTable = context.getTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(NodeJSTypeTable.RESERVED_IDENTIFIER_SET);

    SampleBodyView.Builder sampleBodyView = SampleBodyView.newBuilder();
    sampleBodyView.serviceVarName(
        symbolTable.getNewSymbol(sampleNamer.getServiceVarName(sampleConfig.apiTypeName())));
    sampleBodyView.serviceTypeName(
        sampleTypeTable.getAndSaveNicknameForServiceType(sampleConfig.apiTypeName()));
    sampleBodyView.methodVerb(methodInfo.verb());
    sampleBodyView.methodNameComponents(methodInfo.nameComponents());
    sampleBodyView.requestVarName(symbolTable.getNewSymbol(sampleNamer.getRequestVarName()));
    sampleBodyView.requestTypeName("");

    sampleBodyView.requestBodyVarName("");
    sampleBodyView.requestBodyTypeName("");
    sampleBodyView.resourceFieldName("");
    sampleBodyView.resourceGetterName("");
    sampleBodyView.resourceVarName("");
    sampleBodyView.resourceTypeName("");
    sampleBodyView.isResourceMap(false);
    sampleBodyView.handlePageVarName("");
    sampleBodyView.pageVarName("");

    if (methodInfo.isPageStreaming()) {
      FieldInfo fieldInfo = methodInfo.pageStreamingResourceField();
      if (fieldInfo == null) {
        throw new IllegalArgumentException(
            "method is page streaming, but the page streaming resource field is null.");
      }
      sampleBodyView.resourceFieldName(sampleNamer.getFieldVarName(fieldInfo.name()));
      sampleBodyView.isResourceMap(fieldInfo.type().isMap());

      sampleBodyView.handlePageVarName(
          symbolTable.getNewSymbol(sampleNamer.localVarName(Name.lowerCamel("handlePage"))));
      sampleBodyView.pageVarName(
          symbolTable.getNewSymbol(
              sampleNamer.localVarName(Name.lowerCamel(fieldInfo.name(), "page"))));
    }

    sampleBodyView.responseVarName(symbolTable.getNewSymbol(sampleNamer.getResponseVarName()));
    sampleBodyView.responseTypeName("");
    sampleBodyView.hasResponse(methodInfo.responseType() != null);

    // authVarName is assigned before the fields in case there's a conflict in
    // the symbol table.
    String authVarName = "";
    switch (sampleConfig.authType()) {
      case API_KEY:
        authVarName = "apiKey";
        break;
      default:
        authVarName = "authClient";
    }
    sampleBodyView.authVarName(
        symbolTable.getNewSymbol(sampleNamer.localVarName(Name.lowerCamel(authVarName))));

    List<SampleFieldView> fields = new ArrayList<>();
    for (FieldInfo field :
        context.getSampleConfig().methods().get(context.getMethodName()).fields().values()) {
      fields.add(
          SampleFieldView.newBuilder()
              .name(field.name())
              .typeName("")
              .defaultValue(sampleTypeTable.getZeroValueAndSaveNicknameFor(field.type()))
              .example(field.example())
              .description(field.description())
              .build());
    }
    sampleBodyView.fields(fields);

    sampleBodyView.hasRequestBody(methodInfo.requestBodyType() != null);
    sampleBodyView.requestBodyVarName("");
    sampleBodyView.requestBodyTypeName("");
    sampleBodyView.fieldVarNames(new ArrayList<String>());
    sampleBodyView.isPageStreaming(methodInfo.isPageStreaming());

    sampleBodyView.hasMediaUpload(methodInfo.hasMediaUpload());
    sampleBodyView.hasMediaDownload(methodInfo.hasMediaDownload());

    sampleBodyView.authType(sampleConfig.authType());
    sampleBodyView.authInstructionsUrl(sampleConfig.authInstructionsUrl());
    sampleBodyView.authScopes(methodInfo.authScopes());
    sampleBodyView.isAuthScopesSingular(methodInfo.authScopes().size() == 1);

    sampleBodyView.authFuncName(
        symbolTable.getNewSymbol(sampleNamer.staticFunctionName(Name.lowerCamel("authorize"))));
    sampleBodyView.googleImportVarName(
        symbolTable.getNewSymbol(sampleNamer.localVarName(Name.lowerCamel("google"))));
    return sampleBodyView.build();
  }
}
