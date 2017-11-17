/* Copyright 2016 Google LLC
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

import com.google.api.codegen.config.BatchingConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GrpcStreamingConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.SmokeTestConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitFieldConfig;
import com.google.api.codegen.metacode.InitValue;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.FieldSettingView;
import com.google.api.codegen.viewmodel.FormattedInitValueView;
import com.google.api.codegen.viewmodel.InitCodeLineView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.ResourceNameInitValueView;
import com.google.api.codegen.viewmodel.ResourceNameOneofInitValueView;
import com.google.api.codegen.viewmodel.SimpleInitCodeLineView;
import com.google.api.codegen.viewmodel.SimpleInitValueView;
import com.google.api.codegen.viewmodel.testing.GrpcStreamingView;
import com.google.api.codegen.viewmodel.testing.MockGrpcResponseView;
import com.google.api.codegen.viewmodel.testing.PageStreamingResponseView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;

/** TestCaseTransformer contains helper methods useful for creating test views. */
public class TestCaseTransformer {
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final TestValueGenerator valueGenerator;
  private boolean packageHasMultipleServices;

  public TestCaseTransformer(ValueProducer valueProducer) {
    this(valueProducer, false);
  }

  public TestCaseTransformer(ValueProducer valueProducer, boolean packageHasMultipleServices) {
    this.valueGenerator = new TestValueGenerator(valueProducer);
    this.packageHasMultipleServices = packageHasMultipleServices;
  }

  public TestCaseView createTestCaseView(
      MethodContext methodContext,
      SymbolTable testNameTable,
      InitCodeContext initCodeContext,
      ClientMethodType clientMethodType) {
    MethodModel method = methodContext.getMethodModel();
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();
    ImportTypeTable typeTable = methodContext.getTypeTable();

    String clientMethodName;
    String responseTypeName;
    String callerResponseTypeName;
    String fullyQualifiedResponseTypeName =
        methodContext.getMethodModel().getOutputTypeName(typeTable).getFullName();

    if (methodConfig.isPageStreaming()) {
      clientMethodName = namer.getApiMethodName(method, methodConfig.getVisibility());
      responseTypeName =
          namer.getAndSavePagedResponseTypeName(
              methodContext, methodConfig.getPageStreaming().getResourcesFieldConfig());
      callerResponseTypeName =
          namer.getAndSaveCallerPagedResponseTypeName(
              methodContext, methodConfig.getPageStreaming().getResourcesFieldConfig());
    } else if (methodConfig.isLongRunningOperation()) {
      clientMethodName = namer.getLroApiMethodName(method, methodConfig.getVisibility());
      responseTypeName =
          methodContext
              .getTypeTable()
              .getAndSaveNicknameFor(methodConfig.getLongRunningConfig().getReturnType());
      callerResponseTypeName = responseTypeName;
      fullyQualifiedResponseTypeName =
          methodContext
              .getTypeTable()
              .getFullNameFor(methodConfig.getLongRunningConfig().getReturnType());
    } else if (clientMethodType == ClientMethodType.CallableMethod) {
      clientMethodName = namer.getCallableMethodName(method);
      responseTypeName = method.getAndSaveResponseTypeName(typeTable, namer);
      callerResponseTypeName = responseTypeName;
    } else {
      clientMethodName = namer.getApiMethodName(method, methodConfig.getVisibility());
      responseTypeName = method.getAndSaveResponseTypeName(typeTable, namer);
      callerResponseTypeName = responseTypeName;
    }

    InitCodeView initCode = initCodeTransformer.generateInitCode(methodContext, initCodeContext);

    boolean hasRequestParameters = initCode.lines().size() > 0;
    boolean hasReturnValue = !method.isOutputTypeEmpty();
    if (methodConfig.isLongRunningOperation()) {
      hasReturnValue = !methodConfig.getLongRunningConfig().getReturnType().isEmptyType();
    }

    InitCodeContext responseInitCodeContext =
        createResponseInitCodeContext(methodContext, initCodeContext.symbolTable());
    MockGrpcResponseView mockGrpcResponseView =
        createMockResponseView(methodContext, responseInitCodeContext);

    GrpcStreamingView grpcStreamingView = null;
    if (methodConfig.isGrpcStreaming()) {
      String resourceTypeName = null;
      String resourcesFieldGetterName = null;
      if (methodConfig.getGrpcStreaming().hasResourceField()) {
        FieldModel resourcesField = methodConfig.getGrpcStreaming().getResourcesField();
        resourceTypeName =
            methodContext.getTypeTable().getAndSaveNicknameForElementType(resourcesField);
        resourcesFieldGetterName =
            namer.getFieldGetFunctionName(
                resourcesField, Name.from(resourcesField.getSimpleName()));
      }

      grpcStreamingView =
          GrpcStreamingView.newBuilder()
              .resourceTypeName(resourceTypeName)
              .resourcesFieldGetterName(resourcesFieldGetterName)
              .requestInitCodeList(
                  createGrpcStreamingInitCodeViews(methodContext, initCodeContext, initCode))
              .responseInitCodeList(
                  createGrpcStreamingInitCodeViews(
                      methodContext, responseInitCodeContext, mockGrpcResponseView.initCode()))
              .build();
    }

    return TestCaseView.newBuilder()
        .asserts(initCodeTransformer.generateRequestAssertViews(methodContext, initCodeContext))
        .clientMethodType(clientMethodType)
        .grpcStreamingType(methodConfig.getGrpcStreamingType())
        .hasRequestParameters(hasRequestParameters)
        .hasReturnValue(hasReturnValue)
        .initCode(initCode)
        .mockResponse(mockGrpcResponseView)
        .mockServiceVarName(namer.getMockServiceVarName(methodContext.getTargetInterface()))
        .name(namer.getTestCaseName(testNameTable, method))
        .nameWithException(namer.getExceptionTestCaseName(testNameTable, method))
        .pageStreamingResponseViews(createPageStreamingResponseViews(methodContext))
        .grpcStreamingView(grpcStreamingView)
        .requestTypeName(method.getAndSaveRequestTypeName(typeTable, namer))
        .responseTypeName(responseTypeName)
        .callerResponseTypeName(callerResponseTypeName)
        .fullyQualifiedRequestTypeName(method.getInputTypeName(typeTable).getFullName())
        .fullyQualifiedResponseTypeName(fullyQualifiedResponseTypeName)
        .serviceConstructorName(
            namer.getApiWrapperClassConstructorName(methodContext.getInterfaceModel()))
        .fullyQualifiedServiceClassName(
            namer.getFullyQualifiedApiWrapperClassName(methodContext.getInterfaceConfig()))
        .fullyQualifiedAliasedServiceClassName(
            namer.getTopLevelAliasedApiClassName(
                (methodContext.getInterfaceConfig()), packageHasMultipleServices))
        .clientMethodName(clientMethodName)
        .mockGrpcStubTypeName(namer.getMockGrpcServiceImplName(methodContext.getTargetInterface()))
        .createStubFunctionName(namer.getCreateStubFunctionName(methodContext.getTargetInterface()))
        .grpcStubCallString(namer.getGrpcStubCallString(methodContext.getTargetInterface(), method))
        .build();
  }

  private List<PageStreamingResponseView> createPageStreamingResponseViews(
      MethodContext methodContext) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();

    List<PageStreamingResponseView> pageStreamingResponseViews =
        new ArrayList<PageStreamingResponseView>();

    if (!methodConfig.isPageStreaming()) {
      return pageStreamingResponseViews;
    }

    FieldConfig resourcesFieldConfig = methodConfig.getPageStreaming().getResourcesFieldConfig();
    FieldModel resourcesField = resourcesFieldConfig.getField();
    String resourceTypeName =
        methodContext.getTypeTable().getAndSaveNicknameForElementType(resourcesField);
    String resourcesFieldGetterName = namer.getFieldGetFunctionName(resourcesField);
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

  /**
   * Return a list of three InitCodeView objects, including simpleInitCodeView and generating two
   * additional objects, which will be initialized with different initial values.
   */
  private List<InitCodeView> createGrpcStreamingInitCodeViews(
      MethodContext methodContext,
      InitCodeContext initCodeContext,
      InitCodeView simpleInitCodeView) {
    List<InitCodeView> requestInitCodeList = new ArrayList<>();
    requestInitCodeList.add(simpleInitCodeView);
    requestInitCodeList.add(initCodeTransformer.generateInitCode(methodContext, initCodeContext));
    requestInitCodeList.add(initCodeTransformer.generateInitCode(methodContext, initCodeContext));
    return requestInitCodeList;
  }

  private MockGrpcResponseView createMockResponseView(
      MethodContext methodContext, InitCodeContext responseInitCodeContext) {

    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(methodContext, responseInitCodeContext);
    String typeName =
        methodContext
            .getMethodModel()
            .getAndSaveResponseTypeName(methodContext.getTypeTable(), methodContext.getNamer());
    return MockGrpcResponseView.newBuilder().typeName(typeName).initCode(initCodeView).build();
  }

  private InitCodeContext createResponseInitCodeContext(
      MethodContext context, SymbolTable symbolTable) {
    ArrayList<FieldModel> primitiveFields = new ArrayList<>();
    TypeModel outputType = context.getMethodModel().getOutputType();
    if (context.getMethodConfig().isLongRunningOperation()) {
      outputType = context.getMethodConfig().getLongRunningConfig().getReturnType();
    }
    for (FieldModel field : outputType.getFields()) {
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
        .fieldConfigMap(context.getProductConfig().getDefaultResourceNameFieldConfigMap())
        .valueGenerator(valueGenerator)
        .additionalInitCodeNodes(createMockResponseAdditionalSubTrees(context))
        .build();
  }

  private Iterable<InitCodeNode> createMockResponseAdditionalSubTrees(MethodContext context) {
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
    if (context.getMethodConfig().isBatching()) {
      // Initialize one batching element if it is batching.
      BatchingConfig config = context.getMethodConfig().getBatching();
      if (config.getSubresponseField() != null) {
        String subResponseFieldName = config.getSubresponseField().getSimpleName();
        additionalSubTrees.add(InitCodeNode.createSingletonList(subResponseFieldName));
      }
    }
    if (context.getMethodConfig().isGrpcStreaming()) {
      GrpcStreamingConfig streamingConfig = context.getMethodConfig().getGrpcStreaming();
      if (streamingConfig.hasResourceField()) {
        String resourceFieldName = streamingConfig.getResourcesField().getSimpleName();
        additionalSubTrees.add(InitCodeNode.createSingletonList(resourceFieldName));
      }
    }
    return additionalSubTrees;
  }

  public TestCaseView createSmokeTestCaseView(MethodContext context) {
    MethodConfig methodConfig = context.getMethodConfig();
    ClientMethodType methodType;

    if (methodConfig.isPageStreaming()) {
      if (context.isFlattenedMethodContext()) {
        methodType = ClientMethodType.PagedFlattenedMethod;
      } else {
        methodType = ClientMethodType.PagedRequestObjectMethod;
      }
    } else {
      if (context.isFlattenedMethodContext()) {
        methodType = ClientMethodType.FlattenedMethod;
      } else {
        methodType = ClientMethodType.RequestObjectMethod;
      }
    }

    return createTestCaseView(
        context, new SymbolTable(), createSmokeTestInitContext(context), methodType);
  }

  public boolean requireProjectIdInSmokeTest(InitCodeView initCodeView, SurfaceNamer namer) {
    for (FieldSettingView settingsView : initCodeView.fieldSettings()) {
      InitCodeLineView line = settingsView.initCodeLine();
      if (line.lineType() == InitCodeLineType.SimpleInitLine) {
        SimpleInitCodeLineView simpleLine = (SimpleInitCodeLineView) line;
        String projectVarName =
            namer.localVarReference(Name.from(InitFieldConfig.PROJECT_ID_VARIABLE_NAME));
        if (simpleLine.initValue() instanceof ResourceNameInitValueView) {
          ResourceNameInitValueView initValue = (ResourceNameInitValueView) simpleLine.initValue();
          return initValue.formatArgs().contains(projectVarName);
        } else if (simpleLine.initValue() instanceof ResourceNameOneofInitValueView) {
          ResourceNameOneofInitValueView initValue =
              (ResourceNameOneofInitValueView) simpleLine.initValue();
          ResourceNameInitValueView subValue = initValue.specificResourceNameView();
          return subValue.formatArgs().contains(projectVarName);
        } else if (simpleLine.initValue() instanceof SimpleInitValueView) {
          SimpleInitValueView initValue = (SimpleInitValueView) simpleLine.initValue();
          return initValue.initialValue().equals(projectVarName);
        } else if (simpleLine.initValue() instanceof FormattedInitValueView) {
          FormattedInitValueView initValue = (FormattedInitValueView) simpleLine.initValue();
          return initValue.formatArgs().contains(projectVarName);
        }
      }
    }
    return false;
  }

  public InitCodeContext createSmokeTestInitContext(MethodContext context) {
    SmokeTestConfig testConfig = context.getInterfaceConfig().getSmokeTestConfig();
    InitCodeContext.InitCodeOutputType outputType;
    ImmutableMap<String, FieldConfig> fieldConfigMap;
    if (context.isFlattenedMethodContext()) {
      outputType = InitCodeContext.InitCodeOutputType.FieldList;
      fieldConfigMap =
          FieldConfig.toFieldConfigMap(
              context.getFlatteningConfig().getFlattenedFieldConfigs().values());
    } else {
      outputType = InitCodeContext.InitCodeOutputType.SingleObject;
      fieldConfigMap =
          FieldConfig.toFieldConfigMap(context.getMethodConfig().getRequiredFieldConfigs());
    }

    // Store project ID variable name into the symbol table since it is used
    // by the execute method as a parameter.
    SymbolTable table = new SymbolTable();
    table.getNewSymbol(Name.from(InitFieldConfig.PROJECT_ID_VARIABLE_NAME));

    InitCodeContext.Builder contextBuilder =
        InitCodeContext.newBuilder()
            .initObjectType(testConfig.getMethod().getInputType())
            .suggestedName(Name.from("request"))
            .outputType(outputType)
            .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
            .initFieldConfigStrings(testConfig.getInitFieldConfigStrings())
            .symbolTable(table)
            .fieldConfigMap(fieldConfigMap);
    if (context.isFlattenedMethodContext()) {
      contextBuilder.initFields(context.getFlatteningConfig().getFlattenedFields());
    }
    return contextBuilder.build();
  }

  public FlatteningConfig getSmokeTestFlatteningGroup(
      MethodConfig methodConfig, SmokeTestConfig smokeTestConfig) {
    for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
      if (flatteningGroup.getFlatteningName().equals(smokeTestConfig.getFlatteningName())) {
        return flatteningGroup;
      }
    }
    throw new IllegalArgumentException(
        "Flattening name in smoke test config did not correspond to any flattened method.");
  }
}
