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
package com.google.api.codegen.discovery.transformer.go;

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
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Method;

public class GoSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private final static String TEMPLATE_FILENAME = "go/sample.snip";

  public GoSampleMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, SampleConfig sampleConfig) {
    SampleTypeTable sampleTypeTable =
        new SampleTypeTable(
            new GoTypeTable(), new GoSampleTypeNameConverter(sampleConfig.packagePrefix()));
    GoSampleNamer goSampleNamer = new GoSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(
            sampleConfig, sampleTypeTable, goSampleNamer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    addStaticImports(context);
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable sampleTypeTable = context.getTypeTable();

    SampleBodyView sampleBodyView = createSampleBodyView(context);
    List<String> imports = new ArrayList<String>();
    imports.addAll(GoTypeTable.formatImports(sampleTypeTable.getImports()));
    return SampleView.newBuilder()
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.go")
        .apiTitle(sampleConfig.apiTitle())
        .apiName(sampleConfig.apiName())
        .apiVersion(sampleConfig.apiVersion())
        .className(context.getSampleNamer().getSampleClassName(sampleConfig.apiTypeName()))
        .body(sampleBodyView)
        .imports(imports)
        .build();
  }

  public SampleBodyView createSampleBodyView(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    MethodInfo methodInfo = sampleConfig.methods().get(context.getMethodName());
    SampleNamer sampleNamer = context.getSampleNamer();
    SampleTypeTable sampleTypeTable = context.getTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(GoTypeTable.RESERVED_IDENTIFIER_SET);

    SampleBodyView.Builder sampleBodyView = SampleBodyView.newBuilder();
    String servicePackageName = GoSampleNamer.getServicePackageName(sampleConfig.packagePrefix());
    sampleBodyView.servicePackageName(servicePackageName);

    sampleBodyView.serviceVarName(
        symbolTable.getNewSymbol(sampleNamer.getServiceVarName(servicePackageName)));
    sampleBodyView.serviceTypeName("");
    sampleBodyView.methodVerb(methodInfo.verb());
    List<String> methodNameComponents = new ArrayList<String>();
    for (String nameComponent : methodInfo.nameComponents()) {
      methodNameComponents.add(sampleNamer.publicFieldName(Name.lowerCamel(nameComponent)));
    }
    sampleBodyView.methodNameComponents(methodNameComponents);
    sampleBodyView.requestVarName("req");
    sampleBodyView.requestTypeName("");

    sampleBodyView.requestBodyVarName("");
    sampleBodyView.requestBodyTypeName("");
    sampleBodyView.resourceFieldName("");
    sampleBodyView.resourceGetterName("");
    sampleBodyView.resourceVarName("");
    sampleBodyView.resourceTypeName("");
    sampleBodyView.isResourceMap(false);

    if (methodInfo.isPageStreaming()) {
      FieldInfo fieldInfo = methodInfo.pageStreamingResourceField();
      if (fieldInfo == null) {
        throw new IllegalArgumentException(
            "method is page streaming, but the page streaming resource field is null.");
      }
      sampleBodyView.resourceFieldName(
          sampleNamer.publicFieldName(Name.lowerCamel(fieldInfo.name())));
      if (fieldInfo.type().isMap()) {
        // Assume that the value in the map is a message.
        String resourceVarName =
            sampleNamer.localVarName(
                Name.upperCamel(fieldInfo.type().mapValue().message().typeName()));
        sampleBodyView.resourceVarName(symbolTable.getNewSymbol(resourceVarName));
      } else {
        sampleBodyView.resourceTypeName(
            sampleTypeTable.getAndSaveNickNameForElementType(fieldInfo.type()));
        String resourceVarName =
            sampleNamer.getResourceVarName(
                fieldInfo.type().isMessage() ? fieldInfo.type().message().typeName() : "");
        sampleBodyView.resourceVarName(symbolTable.getNewSymbol(resourceVarName));
      }
      sampleBodyView.isResourceMap(fieldInfo.type().isMap());
    }

    sampleBodyView.responseVarName("");
    sampleBodyView.responseTypeName("");
    sampleBodyView.hasResponse(methodInfo.responseType() != null);
    if (methodInfo.responseType() != null) {
      sampleBodyView.responseVarName(symbolTable.getNewSymbol(sampleNamer.getResponseVarName()));
    }

    // Assigned before fields for precedence in the symbol table.
    sampleBodyView.clientVarName(
        symbolTable.getNewSymbol(sampleNamer.localVarName(Name.lowerCamel("c"))));

    List<SampleFieldView> fields = new ArrayList<>();
    List<String> fieldVarNames = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      fields.add(
          SampleFieldView.newBuilder()
              .name(field.name())
              .typeName("")
              .defaultValue(sampleTypeTable.getZeroValueAndSaveNicknameFor(field.type()))
              .example(field.example())
              .description(field.description())
              .build());
      fieldVarNames.add(field.name());
    }
    sampleBodyView.fields(fields);

    sampleBodyView.hasRequestBody(methodInfo.requestBodyType() != null);
    if (methodInfo.requestBodyType() != null) {
      String requestBodyVarName = symbolTable.getNewSymbol(sampleNamer.getRequestBodyVarName());
      sampleBodyView.requestBodyVarName(requestBodyVarName);
      sampleBodyView.requestBodyTypeName(
          sampleTypeTable.getAndSaveNicknameFor(methodInfo.requestBodyType()));
      fieldVarNames.add(requestBodyVarName);
    }
    sampleBodyView.fieldVarNames(fieldVarNames);
    sampleBodyView.isPageStreaming(methodInfo.isPageStreaming());

    sampleBodyView.hasMediaUpload(methodInfo.hasMediaUpload());
    sampleBodyView.hasMediaDownload(methodInfo.hasMediaDownload());

    sampleBodyView.authType(sampleConfig.authType());
    sampleBodyView.authInstructionsUrl(sampleConfig.authInstructionsUrl());
    sampleBodyView.authScopes(methodInfo.authScopes());
    sampleBodyView.isAuthScopesSingular(methodInfo.authScopes().size() == 1);

    List<String> authScopeConsts = new ArrayList<String>();
    for (String authScope : methodInfo.authScopes()) {
      authScopeConsts.add(sampleNamer.getAuthScopeConst(authScope));
    }
    sampleBodyView.authScopeConsts(authScopeConsts);
    return sampleBodyView.build();
  }

  private void addStaticImports(SampleTransformerContext context) {
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable typeTable = context.getTypeTable();

    typeTable.saveNicknameFor("log;;;");
    typeTable.saveNicknameFor("golang.org/x/net/context;;;");
    typeTable.saveNicknameFor("google.golang.org/x/oauth2/google;;;");
    typeTable.saveNicknameFor(sampleConfig.packagePrefix() + ";;;");
  }
}
