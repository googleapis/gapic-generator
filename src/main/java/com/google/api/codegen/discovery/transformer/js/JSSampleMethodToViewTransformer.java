/* Copyright 2017 Google Inc
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
package com.google.api.codegen.discovery.transformer.js;

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
import com.google.api.codegen.util.js.JSNameFormatter;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import java.util.ArrayList;
import java.util.List;

public class JSSampleMethodToViewTransformer implements SampleMethodToViewTransformer {

  private static final String TEMPLATE_FILENAME = "js/sample.snip";

  public JSSampleMethodToViewTransformer() {}

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

    String requestVarName = symbolTable.getNewSymbol(namer.getRequestVarName());

    // Call first so function names take precedence in the symbol table.
    builder.makeApiCallFuncName(symbolTable.getNewSymbol("makeApiCall"));
    builder.initClientFuncName(symbolTable.getNewSymbol("initClient"));
    builder.handleClientLoadFuncName(symbolTable.getNewSymbol("handleClientLoad"));
    builder.updateSignInStatusFuncName(symbolTable.getNewSymbol("updateSignInStatus"));
    builder.handleSignInClickFuncName(symbolTable.getNewSymbol("handleSignInClick"));
    builder.handleSignOutClickFuncName(symbolTable.getNewSymbol("handleSignOutClick"));
    builder.paramsVarName(symbolTable.getNewSymbol("params"));

    List<SampleFieldView> requiredFields = new ArrayList<>();
    for (FieldInfo field : methodInfo.fields().values()) {
      // The distinction between required and optional fields doesn't matter as
      // much in JS, because all fields are part of the same dict. Therefore, we
      // don't bother splitting into two lists. That also saves us some trouble
      // in the snippet.
      requiredFields.add(createSampleFieldView(field, typeTable));
    }

    boolean hasRequestBody = methodInfo.requestBodyType() != null;
    List<SampleFieldView> requestBodyFields = new ArrayList<>();
    if (hasRequestBody) {
      String requestBodyVarName =
          symbolTable.getNewSymbol(
              namer.getRequestBodyVarName(methodInfo.requestBodyType().message().typeName()));
      builder.requestBodyVarName(requestBodyVarName);

      for (FieldInfo fieldInfo : methodInfo.requestBodyType().message().fields().values()) {
        requestBodyFields.add(createSampleFieldView(fieldInfo, typeTable));
      }
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

    // If there are fields, or if the method is page streaming and the
    // `pageToken` field is in the `params` object, then the `params` object
    // must be present.
    builder.needParams(
        methodInfo.fields().size() > 0
            || (methodInfo.isPageStreaming()
                && !methodInfo.isPageStreamingResourceSetterInRequestBody()));

    return builder
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath(context.getMethodName() + ".frag.js")
        .apiTitle(config.apiTitle())
        .apiName(config.apiName())
        .apiVersion(config.apiVersion())
        .auth(createSampleAuthView(context))
        .methodVerb(methodInfo.verb())
        .methodNameComponents(methodInfo.nameComponents())
        .requestVarName(requestVarName)
        .hasRequestBody(hasRequestBody)
        .requestBodyFields(requestBodyFields)
        .hasResponse(hasResponse)
        .requiredFields(requiredFields)
        .isPageStreaming(methodInfo.isPageStreaming())
        .hasMediaUpload(methodInfo.hasMediaUpload())
        .hasMediaDownload(methodInfo.hasMediaDownload())
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

    // Since the JS sample's page streaming logic is in it's own function, the
    // only identifier that needs to be added to the symbol table is the name of
    // the function. `resourceKeyVarName` and `pageVarName` cannot collide with
    // one another, so we don't add them to the table.
    if (fieldInfo.type().isMap()) {
      builder.resourceKeyVarName(namer.localVarName(Name.lowerCamel("name")));
    }
    builder.resourceFieldName(namer.getFieldVarName(fieldInfo.name()));
    builder.isResourceRepeated(fieldInfo.cardinality() == Cardinality.CARDINALITY_REPEATED);
    builder.isResourceMap(fieldInfo.type().isMap());
    builder.pageVarName(namer.localVarName(Name.lowerCamel(fieldInfo.name(), "page")));

    builder.isResourceSetterInRequestBody(methodInfo.isPageStreamingResourceSetterInRequestBody());
    builder.executeRequestFuncName(symbolTable.getNewSymbol("executeRequest"));
    return builder.build();
  }

  private SampleFieldView createSampleFieldView(FieldInfo field, SampleTypeTable typeTable) {
    return SampleFieldView.newBuilder()
        .name(field.name())
        .defaultValue(typeTable.getZeroValueAndSaveNicknameFor(field.type()))
        .example(field.example())
        .description(field.description())
        .build();
  }
}
