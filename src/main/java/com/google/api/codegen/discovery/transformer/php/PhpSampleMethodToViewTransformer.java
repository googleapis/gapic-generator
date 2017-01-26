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
package com.google.api.codegen.discovery.transformer.php;

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
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

public class PhpSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "php/sample.snip";

  public PhpSampleMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, SampleConfig config) {
    SampleTypeTable typeTable =
        new SampleTypeTable(
            new PhpTypeTable(""), new PhpSampleTypeNameConverter(config.apiTypeName()));
    SampleNamer namer = new PhpSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(config, typeTable, namer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(PhpTypeTable.RESERVED_IDENTIFIER_SET);

    SampleView.Builder builder = SampleView.newBuilder();

    String serviceVarName = symbolTable.getNewSymbol(namer.getServiceVarName(config.apiTypeName()));
    String serviceTypeName = typeTable.getServiceTypeName(config.apiTypeName()).getNickname();

    if (methodInfo.isPageStreaming()) {
      builder.pageStreaming(createSamplePageStreamingView(context, symbolTable));
    }

    // Created before the fields in case there are naming conflicts in the symbol table.
    SampleAuthView sampleAuthView = createSampleAuthView(context);

    List<SampleFieldView> requiredFields = new ArrayList<>();
    List<String> methodCallFieldVarNames = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView =
          SampleFieldView.newBuilder()
              .name(namer.localVarName(Name.lowerCamel(field.name())))
              .defaultValue(typeTable.getZeroValueAndSaveNicknameFor(field.type()))
              .example(field.example())
              .description(field.description())
              .build();
      requiredFields.add(sampleFieldView);
      methodCallFieldVarNames.add(sampleFieldView.name());
    }

    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    if (hasRequestBody) {
      String requestBodyVarName = symbolTable.getNewSymbol(namer.getRequestBodyVarName());
      builder.requestBodyVarName(requestBodyVarName);
      builder.requestBodyTypeName(
          typeTable.getTypeName(methodInfo.requestBodyType()).getNickname());
      methodCallFieldVarNames.add(requestBodyVarName);
    }

    boolean hasResponse = methodInfo.responseType() != null;
    if (hasResponse) {
      builder.responseVarName(symbolTable.getNewSymbol(namer.getResponseVarName()));
    }

    String optParamsVarName = "";
    if (methodInfo.isPageStreaming() || methodInfo.hasMediaDownload()) {
      optParamsVarName = namer.localVarName(Name.lowerCamel("optParams"));
      methodCallFieldVarNames.add(optParamsVarName);
    }

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.php")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .appName(namer.getSampleApplicationName(config.apiCanonicalName()))
        .auth(sampleAuthView)
        .serviceVarName(serviceVarName)
        .serviceTypeName(serviceTypeName)
        .methodVerb(methodInfo.verb())
        .methodNameComponents(methodInfo.nameComponents())
        .hasRequestBody(hasRequestBody)
        .hasResponse(hasResponse)
        .requiredFields(requiredFields)
        .methodCallFieldVarNames(methodCallFieldVarNames)
        .isPageStreaming(methodInfo.isPageStreaming())
        .hasMediaUpload(methodInfo.hasMediaUpload())
        .hasMediaDownload(methodInfo.hasMediaDownload())
        .clientVarName(namer.localVarName(Name.lowerCamel("client")))
        .optParamsVarName(optParamsVarName)
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
    builder.pageVarName(
        symbolTable.getNewSymbol(namer.localVarName(Name.lowerCamel(fieldInfo.name()))));
    builder.pageTokenName(methodInfo.requestPageTokenName());
    builder.nextPageTokenName(Name.lowerCamel(methodInfo.responsePageTokenName()).toUpperCamel());
    return builder.build();
  }
}
