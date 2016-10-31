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
import com.google.api.codegen.transformer.go.GoImportTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.go.GoNameFormatter;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

public class GoSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "go/sample.snip";

  private final GoImportTransformer goImportTransformer = new GoImportTransformer();

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
    List<String> methodNameComponents = new ArrayList<String>();
    for (String nameComponent : methodInfo.nameComponents()) {
      methodNameComponents.add(namer.publicFieldName(Name.lowerCamel(nameComponent)));
    }
    String requestVarName = symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("req")));
    // For this and other type name assignments, we don't use TypeTable logic to
    // add to the import list. The main issue is that the TypeTable returns
    // fully qualified references ("logging.LogEntry"), and we don't always need
    // the fully qualified type. Since all imports are added in
    // addStaticImports, skipping the TypeTable can't result in missing imports.
    String requestTypeName = methodInfo.requestType().message().typeName();

    List<SampleFieldView> fields = new ArrayList<>();
    List<String> fieldVarNames = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView = createSampleFieldView(field, typeTable, symbolTable);
      fields.add(sampleFieldView);
      fieldVarNames.add(sampleFieldView.name());
    }

    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    if (hasRequestBody) {
      String requestBodyVarName = symbolTable.getNewSymbol(namer.getRequestBodyVarName());
      builder.requestBodyVarName(requestBodyVarName);
      builder.requestBodyTypeName(methodInfo.requestBodyType().message().typeName());
      fieldVarNames.add(requestBodyVarName);
    }

    if (methodInfo.isPageStreaming()) {
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
    List<ImportTypeView> imports =
        new ArrayList<>(goImportTransformer.generateImports(typeTable.getImports()));

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.go")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .imports(imports)
        .auth(createSampleAuthView(context))
        .serviceVarName(serviceVarName)
        .methodVerb(methodInfo.verb())
        .methodNameComponents(methodNameComponents)
        .requestVarName(requestVarName)
        .requestTypeName(requestTypeName)
        .hasRequestBody(hasRequestBody)
        .hasResponse(hasResponse)
        .fields(fields)
        .fieldVarNames(fieldVarNames)
        .isPageStreaming(methodInfo.isPageStreaming())
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
    if (fieldInfo == null) {
      throw new IllegalArgumentException("pageStreamingResourceField cannot be null");
    }

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
    builder.isResourceMap(fieldInfo.type().isMap());
    builder.pageVarName(symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("page"))));
    return builder.build();
  }

  private SampleFieldView createSampleFieldView(
      FieldInfo field, SampleTypeTable typeTable, SymbolTable symbolTable) {
    return SampleFieldView.newBuilder()
        .name(symbolTable.getNewSymbol(field.name()))
        .defaultValue(typeTable.getZeroValueAndSaveNicknameFor(field.type()))
        .example(field.example())
        .description(field.description())
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
