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
package com.google.api.codegen.transformer;

import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.BundlingConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValue;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.testing.MockGrpcResponseView;
import com.google.api.codegen.viewmodel.testing.PageStreamingResponseView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;

/** TestCaseTransformer contains helper methods useful for creating test views. */
public class TestCaseTransformer {
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final TestValueGenerator valueGenerator;

  public TestCaseTransformer(ValueProducer valueProducer) {
    this.valueGenerator = new TestValueGenerator(valueProducer);
  }

  public TestCaseView createTestCaseView(
      MethodTransformerContext methodContext,
      SymbolTable testNameTable,
      InitCodeContext initCodeContext,
      ClientMethodType clientMethodType) {
    Method method = methodContext.getMethod();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();

    String clientMethodName;
    String responseTypeName;
    if (methodConfig.isPageStreaming()) {
      clientMethodName = namer.getApiMethodName(method, methodConfig.getVisibility());
      responseTypeName =
          namer.getAndSavePagedResponseTypeName(
              method,
              methodContext.getTypeTable(),
              methodConfig.getPageStreaming().getResourcesFieldConfig());
    } else if (methodConfig.isLongRunningOperation()) {
      clientMethodName = namer.getAsyncApiMethodName(method, methodConfig.getVisibility());
      responseTypeName =
          methodContext
              .getTypeTable()
              .getAndSaveNicknameFor(methodConfig.getLongRunningConfig().getReturnType());
    } else if (clientMethodType == ClientMethodType.CallableMethod) {
      clientMethodName = namer.getCallableMethodName(method);
      responseTypeName = methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    } else {
      clientMethodName = namer.getApiMethodName(method, methodConfig.getVisibility());
      responseTypeName = methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    }

    InitCodeView initCode = initCodeTransformer.generateInitCode(methodContext, initCodeContext);

    boolean hasRequestParameters = initCode.lines().size() > 0;
    boolean hasReturnValue = !ServiceMessages.s_isEmptyType(method.getOutputType());
    if (methodConfig.isLongRunningOperation()) {
      hasReturnValue =
          !ServiceMessages.s_isEmptyType(methodConfig.getLongRunningConfig().getReturnType());
    }

    return TestCaseView.newBuilder()
        .asserts(initCodeTransformer.generateRequestAssertViews(methodContext, initCodeContext))
        .clientMethodType(clientMethodType)
        .grpcStreamingType(methodConfig.getGrpcStreamingType())
        .hasRequestParameters(hasRequestParameters)
        .hasReturnValue(hasReturnValue)
        .initCode(initCode)
        .mockResponse(createMockResponseView(methodContext, initCodeContext.symbolTable()))
        .mockServiceVarName(namer.getMockServiceVarName(methodContext.getTargetInterface()))
        .name(namer.getTestCaseName(testNameTable, method))
        .nameWithException(namer.getExceptionTestCaseName(testNameTable, method))
        .pageStreamingResponseViews(createPageStreamingResponseViews(methodContext))
        .requestTypeName(methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType()))
        .responseTypeName(responseTypeName)
        .serviceConstructorName(
            namer.getApiWrapperClassConstructorName(methodContext.getInterface()))
        .clientMethodName(clientMethodName)
        .mockGrpcStubTypeName(namer.getMockGrpcServiceImplName(methodContext.getTargetInterface()))
        .createStubFunctionName(namer.getCreateStubFunctionName(methodContext.getTargetInterface()))
        .grpcMethodName(namer.getGrpcMethodName(method))
        .build();
  }

  private List<PageStreamingResponseView> createPageStreamingResponseViews(
      MethodTransformerContext methodContext) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();

    List<PageStreamingResponseView> pageStreamingResponseViews =
        new ArrayList<PageStreamingResponseView>();

    if (!methodConfig.isPageStreaming()) {
      return pageStreamingResponseViews;
    }

    FieldConfig resourcesFieldConfig = methodConfig.getPageStreaming().getResourcesFieldConfig();
    Field resourcesField = resourcesFieldConfig.getField();
    String resourceTypeName =
        methodContext.getTypeTable().getAndSaveNicknameForElementType(resourcesField.getType());
    String resourcesFieldGetterName =
        namer.getFieldGetFunctionName(
            resourcesField.getType(), Name.from(resourcesField.getSimpleName()));
    pageStreamingResponseViews.add(
        PageStreamingResponseView.newBuilder()
            .resourceTypeName(resourceTypeName)
            .resourcesFieldGetterName(resourcesFieldGetterName)
            .resourcesIterateMethod(namer.getPagedResponseIterateMethod())
            .resourcesVarName(namer.localVarName(Name.from("resources")))
            .build());

    if (methodContext.getFeatureConfig().useResourceNameFormatOption(resourcesFieldConfig)) {
      resourceTypeName =
          methodContext
              .getNamer()
              .getAndSaveElementResourceTypeName(
                  methodContext.getTypeTable(), resourcesFieldConfig);

      resourcesFieldGetterName = namer.getResourceNameFieldGetFunctionName(resourcesFieldConfig);
      pageStreamingResponseViews.add(
          PageStreamingResponseView.newBuilder()
              .resourceTypeName(resourceTypeName)
              .resourcesFieldGetterName(resourcesFieldGetterName)
              .resourcesIterateMethod(
                  namer.getPagedResponseIterateMethod(
                      methodContext.getFeatureConfig(), resourcesFieldConfig))
              .resourcesVarName(namer.localVarName(Name.from("resource_names")))
              .build());
    }
    return pageStreamingResponseViews;
  }

  private MockGrpcResponseView createMockResponseView(
      MethodTransformerContext methodContext, SymbolTable symbolTable) {
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            methodContext, createResponseInitCodeContext(methodContext, symbolTable));

    String typeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(methodContext.getMethod().getOutputType());
    return MockGrpcResponseView.newBuilder().typeName(typeName).initCode(initCodeView).build();
  }

  private InitCodeContext createResponseInitCodeContext(
      MethodTransformerContext context, SymbolTable symbolTable) {
    ArrayList<Field> primitiveFields = new ArrayList<>();
    TypeRef outputType = context.getMethod().getOutputType();
    if (context.getMethodConfig().isLongRunningOperation()) {
      outputType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    }
    for (Field field : outputType.getMessageType().getFields()) {
      if (field.getType().isPrimitive() && !field.getType().isRepeated()) {
        primitiveFields.add(field);
      }
    }
    return InitCodeContext.newBuilder()
        .initObjectType(outputType)
        .symbolTable(symbolTable)
        .suggestedName(Name.from("expected_response"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(ImmutableMap.<String, InitValueConfig>of())
        .initFields(primitiveFields)
        .fieldConfigMap(context.getApiConfig().getDefaultResourceNameFieldConfigMap())
        .valueGenerator(valueGenerator)
        .additionalInitCodeNodes(createMockResponseAdditionalSubTrees(context))
        .build();
  }

  private Iterable<InitCodeNode> createMockResponseAdditionalSubTrees(
      MethodTransformerContext context) {
    List<InitCodeNode> additionalSubTrees = new ArrayList<>();
    if (context.getMethodConfig().isPageStreaming()) {
      // Initialize one resource element if it is page-streaming.
      PageStreamingConfig config = context.getMethodConfig().getPageStreaming();
      String resourceFieldName = config.getResourcesFieldName();
      additionalSubTrees.add(InitCodeNode.createSingletonList(resourceFieldName));

      // Set the initial value of the page token to empty, in order to indicate that no more pages
      // are available
      String responseTokenName = config.getResponseTokenField().getSimpleName();
      additionalSubTrees.add(
          InitCodeNode.createWithValue(
              responseTokenName, InitValueConfig.createWithValue(InitValue.createLiteral(""))));
    }
    if (context.getMethodConfig().isBundling()) {
      // Initialize one bundling element if it is bundling.
      BundlingConfig config = context.getMethodConfig().getBundling();
      String subResponseFieldName = config.getSubresponseField().getSimpleName();
      additionalSubTrees.add(InitCodeNode.createSingletonList(subResponseFieldName));
    }
    return additionalSubTrees;
  }
}
