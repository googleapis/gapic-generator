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
package com.google.api.codegen.discovery.transformer.py;

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
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.py.PythonNameFormatter;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Strings;
import com.google.common.collect.Iterables;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

public class PythonSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "py/sample.snip";

  public PythonSampleMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, SampleConfig config) {
    SampleTypeTable typeTable =
        new SampleTypeTable(new PythonTypeTable(""), new PythonSampleTypeNameConverter());
    SampleNamer namer = new PythonSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(config, typeTable, namer, method.getName());
    return createSampleView(context);
  }

  private SampleView createSampleView(SampleTransformerContext context) {
    SampleConfig config = context.getSampleConfig();
    MethodInfo methodInfo = config.methods().get(context.getMethodName());
    SampleNamer namer = context.getSampleNamer();
    SymbolTable symbolTable = SymbolTable.fromSeed(PythonNameFormatter.RESERVED_IDENTIFIER_SET);

    SampleView.Builder builder = SampleView.newBuilder();

    String serviceVarName = symbolTable.getNewSymbol(namer.getServiceVarName(config.apiTypeName()));

    // Created before the fields in case there are naming conflicts in the symbol table.
    String credentialsVarName =
        symbolTable.getNewSymbol(
            config.authType() == AuthType.API_KEY ? "developerKey" : "credentials");
    SampleAuthView sampleAuthView = createSampleAuthView(context);

    List<SampleFieldView> requiredFields = new ArrayList<>();
    List<SampleFieldView> optionalFields = new ArrayList<>();
    List<String> methodParamAssignments = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      SampleFieldView sampleFieldView = createSampleFieldView(field, context, symbolTable, true);
      if (sampleFieldView.required()) {
        requiredFields.add(sampleFieldView);
      } else {
        optionalFields.add(sampleFieldView);
      }
      // Ex: "fooBar=foo_bar"
      methodParamAssignments.add(field.name() + "=" + sampleFieldView.name());
    }

    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    List<SampleFieldView> requestBodyFields = new ArrayList<>();
    if (hasRequestBody) {
      String requestBodyVarName =
          symbolTable.getNewSymbol(
              namer.getRequestBodyVarName(methodInfo.requestBodyType().message().typeName()));
      builder.requestBodyVarName(requestBodyVarName);

      for (FieldInfo fieldInfo : methodInfo.requestBodyType().message().fields().values()) {
        // Request body fields are excluded from the symbol table because the
        // field names are the keys of a map.
        requestBodyFields.add(createSampleFieldView(fieldInfo, context, symbolTable, false));
      }

      methodParamAssignments.add("body=" + requestBodyVarName);
    }

    // The page streaming view model is generated close to last to avoid taking naming precedence in
    // the symbol table.
    if (methodInfo.isPageStreaming()) {
      builder.pageStreaming(createSamplePageStreamingView(context, symbolTable));
    }

    boolean hasResponse = methodInfo.responseType() != null;
    if (hasResponse) {
      builder.responseVarName(symbolTable.getNewSymbol(namer.getResponseVarName()));
    }

    List<String> discoveryBuildParams = new ArrayList<>();
    discoveryBuildParams.add(String.format("'%s'", config.apiName()));
    discoveryBuildParams.add(String.format("'%s'", config.apiVersion()));
    switch (config.authType()) {
      case NONE:
        break;
      case API_KEY:
        discoveryBuildParams.add("developerKey=" + credentialsVarName);
        break;
      default:
        discoveryBuildParams.add("credentials=" + credentialsVarName);
        break;
    }
    String discoveryDocUrl = config.discoveryDocUrl();
    if (!Strings.isNullOrEmpty(discoveryDocUrl)) {
      discoveryBuildParams.add(String.format("discoveryServiceUrl='%s'", discoveryDocUrl));
    }

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.py")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .auth(sampleAuthView)
        .serviceVarName(serviceVarName)
        .methodVerb(methodInfo.verb())
        .methodNameComponents(methodInfo.nameComponents())
        .requestVarName(namer.getRequestVarName())
        .hasRequestBody(hasRequestBody)
        .requestBodyFields(requestBodyFields)
        .hasResponse(hasResponse)
        .requiredFields(requiredFields)
        .optionalFields(optionalFields)
        .isPageStreaming(methodInfo.isPageStreaming())
        .hasMediaUpload(methodInfo.hasMediaUpload())
        .hasMediaDownload(methodInfo.hasMediaDownload())
        .credentialsVarName(credentialsVarName)
        .discoveryBuildParams(discoveryBuildParams)
        .lastMethodNameComponent(Iterables.getLast(methodInfo.nameComponents()))
        .methodParamAssigments(methodParamAssignments)
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

    builder.resourceFieldName(fieldInfo.name());
    if (fieldInfo.type().isMap()) {
      // Assume that the value in the map is a message.
      if (!fieldInfo.type().mapValue().isMessage()) {
        throw new IllegalArgumentException("expected map value to be a message");
      }
      builder.resourceKeyVarName(symbolTable.getNewSymbol("name"));
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
    builder.pageVarName(symbolTable.getNewSymbol(fieldInfo.name()));
    builder.isResourceSetterInRequestBody(methodInfo.isPageStreamingResourceSetterInRequestBody());
    builder.pageTokenName(methodInfo.requestPageTokenName());
    builder.nextPageTokenName(methodInfo.responsePageTokenName());
    return builder.build();
  }

  private SampleFieldView createSampleFieldView(
      FieldInfo field,
      SampleTransformerContext context,
      SymbolTable symbolTable,
      boolean disambiguateName) {
    SampleNamer namer = context.getSampleNamer();
    SampleTypeTable typeTable = context.getSampleTypeTable();
    String name = namer.getFieldVarName(field.name());
    if (disambiguateName) {
      name = symbolTable.getNewSymbol(name);
    }
    String defaultValue;
    if (!Strings.isNullOrEmpty(field.defaultValue())) {
      defaultValue = field.defaultValue();
    } else {
      defaultValue = typeTable.getZeroValueAndSaveNicknameFor(field.type());
    }
    return SampleFieldView.newBuilder()
        .name(name)
        .defaultValue(defaultValue)
        .example(field.example())
        .description(field.description())
        .required(field.required())
        .build();
  }
}
