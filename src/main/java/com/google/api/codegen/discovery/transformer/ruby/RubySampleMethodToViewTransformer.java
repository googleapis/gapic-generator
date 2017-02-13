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
package com.google.api.codegen.discovery.transformer.ruby;

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
import com.google.api.codegen.util.ruby.RubyNameFormatter;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

public class RubySampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "ruby/sample.snip";

  public RubySampleMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, SampleConfig config) {
    SampleTypeTable typeTable =
        new SampleTypeTable(new RubyTypeTable(""), new RubySampleTypeNameConverter());
    SampleNamer namer = new RubySampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(config, typeTable, namer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    SymbolTable symbolTable = SymbolTable.fromSeed(RubyNameFormatter.RESERVED_IDENTIFIER_SET);

    SampleView.Builder builder = SampleView.newBuilder();

    String serviceVarName = symbolTable.getNewSymbol(namer.getServiceVarName(config.apiTypeName()));
    String serviceTypeName = typeTable.getServiceTypeName(config.apiTypeName()).getNickname();
    String serviceTypeNamespace =
        RubySampleNamer.getServiceTypeNamespace(config.apiName(), config.apiVersion());

    if (methodInfo.isPageStreaming()) {
      builder.pageStreaming(createSamplePageStreamingView(context, symbolTable));
    }

    // Created before the fields in case there are naming conflicts in the symbol table.
    SampleAuthView sampleAuthView = createSampleAuthView(context);

    List<SampleFieldView> requiredFields = new ArrayList<>();
    List<SampleFieldView> optionalFields = new ArrayList<>();
    List<String> methodCallFieldVarNames = new ArrayList<>();
    List<String> optionalMethodCallFieldVarNames = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView = createSampleFieldView(field, context, symbolTable);
      if (sampleFieldView.required()) {
        requiredFields.add(sampleFieldView);
        methodCallFieldVarNames.add(sampleFieldView.name());
      } else {
        optionalFields.add(sampleFieldView);
        optionalMethodCallFieldVarNames.add(
            namer.getFieldVarName(field.name()) + ": " + sampleFieldView.name());
      }
    }

    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    List<SampleFieldView> requestBodyFields = new ArrayList<>();
    if (hasRequestBody) {
      String requestBodyVarName = symbolTable.getNewSymbol(namer.getRequestBodyVarName());
      builder.requestBodyVarName(requestBodyVarName);
      builder.requestBodyTypeName(
          typeTable.getTypeName(methodInfo.requestBodyType()).getNickname());
      methodCallFieldVarNames.add(requestBodyVarName);

      for (FieldInfo fieldInfo : methodInfo.requestBodyType().message().fields().values()) {
        requestBodyFields.add(createSampleFieldView(fieldInfo, context, symbolTable));
      }
    }

    // Ensure that optional method parameters appear last.
    methodCallFieldVarNames.addAll(optionalMethodCallFieldVarNames);

    boolean hasResponse = methodInfo.responseType() != null;
    if (hasResponse) {
      builder.responseVarName(symbolTable.getNewSymbol(namer.getResponseVarName()));
    }

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.rb")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .auth(sampleAuthView)
        .serviceVarName(serviceVarName)
        .serviceTypeName(serviceTypeName)
        .methodVerb(methodInfo.verb())
        .methodNameComponents(methodInfo.nameComponents())
        .hasRequestBody(hasRequestBody)
        .requestBodyFields(requestBodyFields)
        .hasResponse(hasResponse)
        .requiredFields(requiredFields)
        .optionalFields(optionalFields)
        .methodCallFieldVarNames(methodCallFieldVarNames)
        .isPageStreaming(methodInfo.isPageStreaming())
        .hasMediaUpload(methodInfo.hasMediaUpload())
        .hasMediaDownload(methodInfo.hasMediaDownload())
        .serviceRequirePath(config.packagePrefix())
        .serviceTypeNamespace(serviceTypeNamespace)
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

    String resourceFieldName = namer.publicFieldName(Name.lowerCamel(fieldInfo.name()));
    builder.resourceFieldName(resourceFieldName.equals("items") ? "" : resourceFieldName);
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
    builder.isResourceSetterInRequestBody(methodInfo.isPageStreamingResourceSetterInRequestBody());
    builder.pageTokenName(Name.lowerCamel(methodInfo.requestPageTokenName()).toLowerUnderscore());
    String nextPageTokenName =
        Name.lowerCamel(methodInfo.responsePageTokenName()).toLowerUnderscore();
    builder.nextPageTokenName(nextPageTokenName.equals("next_page_token") ? "" : nextPageTokenName);
    return builder.build();
  }

  private SampleFieldView createSampleFieldView(
      FieldInfo field, SampleTransformerContext context, SymbolTable symbolTable) {
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    String name = namer.getFieldVarName(field.name());
    return SampleFieldView.newBuilder()
        .name(symbolTable.getNewSymbol(name))
        .defaultValue(typeTable.getZeroValueAndSaveNicknameFor(field.type()))
        .example(field.example())
        .description(field.description())
        .setterFuncName(name)
        .required(field.required())
        .build();
  }
}
