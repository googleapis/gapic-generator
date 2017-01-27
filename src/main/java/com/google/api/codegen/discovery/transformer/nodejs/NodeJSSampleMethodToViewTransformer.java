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

import com.google.api.codegen.discovery.config.FieldInfo;
import com.google.api.codegen.discovery.config.MethodInfo;
import com.google.api.codegen.discovery.config.SampleConfig;
import com.google.api.codegen.discovery.transformer.SampleMethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.transformer.js.JSSampleNamer;
import com.google.api.codegen.discovery.transformer.js.JSSampleTypeNameConverter;
import com.google.api.codegen.discovery.viewmodel.SampleAuthView;
import com.google.api.codegen.discovery.viewmodel.SampleFieldView;
import com.google.api.codegen.discovery.viewmodel.SamplePageStreamingView;
import com.google.api.codegen.discovery.viewmodel.SampleView;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.js.JSNameFormatter;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

public class NodeJSSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "nodejs/sample.snip";

  /**
   * The set of param names reserved by the google-api-nodejs-client generator.
   *
   * <p>See
   * https://github.com/google/google-api-nodejs-client/blob/482b7f7b972ad95047ea8d8ca6daabce9b1b4008/lib/generator.js#L58
   */
  private static final ImmutableSet<String> RESERVED_PARAM_NAMES =
      ImmutableSet.of("resource", "media", "auth");

  public NodeJSSampleMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, SampleConfig sampleConfig) {
    SampleTypeTable sampleTypeTable =
        new SampleTypeTable(new JSTypeTable(""), new JSSampleTypeNameConverter());
    JSSampleNamer jsSampleNamer = new JSSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(
            sampleConfig, sampleTypeTable, jsSampleNamer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(JSNameFormatter.RESERVED_IDENTIFIER_SET);

    SampleView.Builder builder = SampleView.newBuilder();

    String serviceVarName = symbolTable.getNewSymbol(namer.getServiceVarName(config.apiTypeName()));
    String serviceTypeName = typeTable.getAndSaveNicknameForServiceType(config.apiTypeName());
    String requestVarName = symbolTable.getNewSymbol(namer.getRequestVarName());

    if (methodInfo.isPageStreaming()) {
      builder.pageStreaming(createSamplePageStreamingView(context, symbolTable));
    }

    // Created before the fields in case there are naming conflicts in the symbol table.
    SampleAuthView sampleAuthView = createSampleAuthView(context, symbolTable);

    List<SampleFieldView> requiredFields = new ArrayList<>();
    List<SampleFieldView> optionalFields = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView = createSampleFieldView(field, typeTable, true);
      if (sampleFieldView.required()) {
        requiredFields.add(sampleFieldView);
      } else {
        optionalFields.add(sampleFieldView);
      }
    }

    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    List<SampleFieldView> requestBodyFields = new ArrayList<>();
    if (hasRequestBody) {
      for (FieldInfo fieldInfo : methodInfo.requestBodyType().message().fields().values()) {
        requestBodyFields.add(createSampleFieldView(fieldInfo, typeTable, false));
      }
    }

    boolean hasResponse = methodInfo.responseType() != null;
    if (hasResponse) {
      builder.responseVarName(symbolTable.getNewSymbol(namer.getResponseVarName()));
    }

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.njs")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .auth(sampleAuthView)
        .serviceVarName(serviceVarName)
        .serviceTypeName(serviceTypeName)
        .methodVerb(methodInfo.verb())
        .methodNameComponents(methodInfo.nameComponents())
        .requestVarName(requestVarName)
        .hasRequestBody(hasRequestBody)
        .requestBodyFields(requestBodyFields)
        .hasResponse(hasResponse)
        .requiredFields(requiredFields)
        .optionalFields(optionalFields)
        .isPageStreaming(methodInfo.isPageStreaming())
        .hasMediaUpload(methodInfo.hasMediaUpload())
        .hasMediaDownload(methodInfo.hasMediaDownload())
        .googleImportVarName(
            symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("google"))))
        .build();
  }

  private SampleAuthView createSampleAuthView(
      SampleTransformerContext context, SymbolTable symbolTable) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();

    String authVarName = "";
    switch (config.authType()) {
      case API_KEY:
        authVarName = "apiKey";
        break;
      default:
        authVarName = "authClient";
    }

    return SampleAuthView.newBuilder()
        .type(config.authType())
        .instructionsUrl(config.authInstructionsUrl())
        .scopes(methodInfo.authScopes())
        .isScopesSingular(methodInfo.authScopes().size() == 1)
        .authFuncName(
            symbolTable.getNewSymbol(namer.staticFunctionName(Name.lowerCamel("authorize"))))
        .authVarName(symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel(authVarName))))
        .build();
  }

  private SamplePageStreamingView createSamplePageStreamingView(
      SampleTransformerContext context, SymbolTable symbolTable) {
    MethodInfo methodInfo = context.getSampleConfig().methods().get(context.getMethodName());
    FieldInfo fieldInfo = methodInfo.pageStreamingResourceField();
    SampleNamer namer = context.getSampleNamer();

    SamplePageStreamingView.Builder builder = SamplePageStreamingView.newBuilder();

    if (fieldInfo.type().isMap()) {
      builder.resourceKeyVarName(
          symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("name"))));
    }
    builder.resourceFieldName(namer.getFieldVarName(fieldInfo.name()));
    builder.isResourceRepeated(fieldInfo.cardinality() == Cardinality.CARDINALITY_REPEATED);
    builder.isResourceMap(fieldInfo.type().isMap());
    builder.handlePageVarName(
        symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel("handlePage"))));
    builder.pageVarName(
        symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel(fieldInfo.name(), "page"))));
    builder.pageTokenName(methodInfo.requestPageTokenName());
    builder.nextPageTokenName(methodInfo.responsePageTokenName());
    return builder.build();
  }

  private SampleFieldView createSampleFieldView(
      FieldInfo field, SampleTypeTable typeTable, boolean escapeReservedParamNames) {
    String name = field.name();
    if (escapeReservedParamNames && RESERVED_PARAM_NAMES.contains(name)) {
      name = name + "_";
    }
    return SampleFieldView.newBuilder()
        .name(name)
        .defaultValue(typeTable.getZeroValueAndSaveNicknameFor(field.type()))
        .example(field.example())
        .description(field.description())
        .required(field.required())
        .build();
  }
}
