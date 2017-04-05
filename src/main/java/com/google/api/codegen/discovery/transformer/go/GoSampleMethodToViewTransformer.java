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

import com.google.api.codegen.discovery.config.AuthType;
import com.google.api.codegen.discovery.config.FieldInfo;
import com.google.api.codegen.discovery.config.MethodInfo;
import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.viewmodel.SampleAuthView;
import com.google.api.codegen.discovery.viewmodel.SampleFieldView;
import com.google.api.codegen.discovery.viewmodel.SamplePageStreamingView;
import com.google.api.codegen.discovery.viewmodel.SampleView;
import com.google.api.codegen.transformer.go.GoImportSectionTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

public class GoSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "go/sample.snip";

  private final GoImportSectionTransformer goImportTransformer = new GoImportSectionTransformer();

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
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(GoNameFormatter.RESERVED_IDENTIFIER_SET);
    addStaticImports(context, symbolTable);

    SampleView.Builder builder = SampleView.newBuilder();

    String clientVarName = symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("c")));

    // TODO(saicheems): Consider refactoring static GoSampleNamer calls so there
    // isn't Go specific logic in the transformer.
    String servicePackageName = GoSampleNamer.getServicePackageName(config.packagePrefix());
    String serviceVarName = symbolTable.getNewSymbol(namer.getServiceVarName(servicePackageName));
    String requestVarName = symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("req")));
    // For this and other type name assignments, we don't use TypeTable logic to
    // add to the import list. The main issue is that the TypeTable returns
    // fully qualified references ("logging.LogEntry"), and we don't always need
    // the fully qualified type. Since all imports are added in
    // addStaticImports, skipping the TypeTable can't result in missing imports.
    String requestTypeName = methodInfo.requestType().message().typeName();

    List<SampleFieldView> requiredFields = new ArrayList<>();
    List<SampleFieldView> optionalFields = new ArrayList<>();
    List<String> methodCallFieldVarNames = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView = createSampleFieldView(field, context, symbolTable);
      if (sampleFieldView.required()) {
        requiredFields.add(sampleFieldView);
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
      builder.requestBodyTypeName(methodInfo.requestBodyType().message().typeName());
      methodCallFieldVarNames.add(requestBodyVarName);

      for (FieldInfo fieldInfo : methodInfo.requestBodyType().message().fields().values()) {
        requestBodyFields.add(createSampleFieldView(fieldInfo, context, symbolTable));
      }
    }

    // The page streaming view model is generated close to last to avoid taking naming precedence in
    // the symbol table.
    boolean isPageStreaming = methodInfo.isPageStreaming();
    if (isPageStreaming) {
      builder.pageStreaming(createSamplePageStreamingView(context, symbolTable));
    }

    boolean hasResponse = methodInfo.responseType() != null;
    if (hasResponse) {
      builder.responseVarName(symbolTable.getNewSymbol(namer.getResponseVarName()));
      // This response type is only used in page streaming functions.
      builder.responseTypeName(methodInfo.responseType().message().typeName());
    }

    if (config.authType() != AuthType.APPLICATION_DEFAULT_CREDENTIALS) {
      builder.getClientFuncName(namer.privateMethodName(Name.lowerCamel("getClient")));
    }

    // Imports must be collected last.
    ImportSectionView importSection =
        goImportTransformer.generateImportSection(typeTable.getImports());

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.go")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .importSection(importSection)
        .auth(createSampleAuthView(context))
        .serviceVarName(serviceVarName)
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
        .isPageStreaming(isPageStreaming)
        .hasMediaUpload(methodInfo.hasMediaUpload())
        .hasMediaDownload(methodInfo.hasMediaDownload())
        .servicePackageName(servicePackageName)
        .clientVarName(clientVarName)
        .contextVarName(symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("ctx"))))
        .build();
  }

  private SampleAuthView createSampleAuthView(SampleTransformerContext context) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());

    List<String> scopeConsts = new ArrayList<>();
    // Pull scopes and reconstruct them as Go constants in the sample.
    // For example: "https://www.googleapis.com/auth/cloud-platform" to
    // "CloudPlatformScope"
    if (config.authType() != AuthType.APPLICATION_DEFAULT_CREDENTIALS) {
      for (String scope : methodInfo.authScopes()) {
        scopeConsts.add(GoSampleNamer.getAuthScopeConst(scope));
      }
    }

    return SampleAuthView.newBuilder()
        .type(config.authType())
        .instructionsUrl(config.authInstructionsUrl())
        .scopes(methodInfo.authScopes())
        .isScopesSingular(methodInfo.authScopes().size() == 1)
        .scopeConsts(scopeConsts)
        .build();
  }

  private SamplePageStreamingView createSamplePageStreamingView(
      SampleTransformerContext context, SymbolTable symbolTable) {
    MethodInfo methodInfo = context.getSampleConfig().methods().get(context.getMethodName());
    FieldInfo fieldInfo = methodInfo.pageStreamingResourceField();
    SampleNamer namer = context.getSampleNamer();

    SamplePageStreamingView.Builder builder = SamplePageStreamingView.newBuilder();

    builder.resourceFieldName(namer.publicFieldName(Name.lowerCamel(fieldInfo.name())));
    if (fieldInfo.type().isMap()) {
      // Assume that the value in the map is a message.
      if (!fieldInfo.type().mapValue().isMessage()) {
        throw new IllegalArgumentException("expected map value to be a message");
      }
      builder.resourceKeyVarName(
          symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("name"))));
      String resourceValueVarName =
          namer.localVarName(Name.upperCamel(fieldInfo.type().mapValue().message().typeName()));
      builder.resourceValueVarName(symbolTable.getNewSymbol(resourceValueVarName));
    } else {
      String resourceVarName =
          namer.getResourceVarName(
              fieldInfo.type().isMessage() ? fieldInfo.type().message().typeName() : "");
      builder.resourceVarName(symbolTable.getNewSymbol(resourceVarName));
    }
    builder.isResourceRepeated(fieldInfo.cardinality() == Cardinality.CARDINALITY_REPEATED);
    builder.isResourceMap(fieldInfo.type().isMap());
    builder.pageVarName(symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("page"))));
    return builder.build();
  }

  private SampleFieldView createSampleFieldView(
      FieldInfo field, SampleTransformerContext context, SymbolTable symbolTable) {
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    return SampleFieldView.newBuilder()
        .name(symbolTable.getNewSymbol(field.name()))
        .defaultValue(typeTable.getZeroValueAndSaveNicknameFor(field.type()))
        .example(field.example())
        .description(field.description())
        .setterFuncName(namer.getRequestBodyFieldSetterName(field.name()))
        .required(field.required())
        .isArray(field.type().isArray())
        .build();
  }

  private void addStaticImports(SampleTransformerContext context, SymbolTable symbolTable) {
    SampleConfig config = context.getSampleConfig();
    SampleTypeTable typeTable = context.getSampleTypeTable();

    // Since nearly any identifier can be shadowed in Go, we add every package
    // name to the symbol table.
    // GoTypeTable expects imports to be of the format:
    // "{packagePath};{localName};{typeName};{pointer}"

    if (config.methods().get(context.getMethodName()).responseType() != null) {
      saveNicknameAndSymbolFor("fmt;;;", "fmt", typeTable, symbolTable);
    }
    saveNicknameAndSymbolFor("log;;;", "log", typeTable, symbolTable);
    saveNicknameAndSymbolFor("golang.org/x/net/context;;;", "context", typeTable, symbolTable);
    switch (config.authType()) {
      case NONE:
        saveNicknameAndSymbolFor("net/http;;;", "http", typeTable, symbolTable);
        break;
      case APPLICATION_DEFAULT_CREDENTIALS:
        saveNicknameAndSymbolFor("golang.org/x/oauth2/google;;;", "google", typeTable, symbolTable);
        break;
      default:
        saveNicknameAndSymbolFor("errors;;;", "errors", typeTable, symbolTable);
        saveNicknameAndSymbolFor("net/http;;;", "http", typeTable, symbolTable);
    }
    saveNicknameAndSymbolFor(
        config.packagePrefix() + ";;;",
        GoSampleNamer.getServicePackageName(config.packagePrefix()),
        typeTable,
        symbolTable);
  }

  private void saveNicknameAndSymbolFor(
      String fullName, String shortName, SampleTypeTable typeTable, SymbolTable symbolTable) {
    typeTable.saveNicknameFor(fullName);
    symbolTable.getNewSymbol(shortName);
  }
}
