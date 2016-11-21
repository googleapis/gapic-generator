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

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.BundlingConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameMessageConfigs;
import com.google.api.codegen.config.VisibilityConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValue;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockGrpcResponseView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.PageStreamingResponseView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** GapicTestTransformer contains helper methods useful for creating mock and test views. */
public class GapicTestTransformer {

  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final TestValueGenerator valueGenerator;

  public GapicTestTransformer(ValueProducer valueProducer) {
    this.valueGenerator = new TestValueGenerator(valueProducer);
  }

  public List<Interface> getGrpcInterfacesToMock(Model model, ApiConfig apiConfig) {
    Map<String, Interface> interfaces = new LinkedHashMap<>();

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      if (!service.isReachable()) {
        continue;
      }
      interfaces.put(service.getFullName(), service);
      InterfaceConfig interfaceConfig = apiConfig.getInterfaceConfig(service);
      for (MethodConfig methodConfig : interfaceConfig.getMethodConfigs()) {
        String reroute = methodConfig.getRerouteToGrpcInterface();
        if (!Strings.isNullOrEmpty(reroute)) {
          Interface targetInterface = model.getSymbolTable().lookupInterface(reroute);
          interfaces.put(reroute, targetInterface);
        }
      }
    }

    return new ArrayList<Interface>(interfaces.values());
  }

  public List<MockGrpcMethodView> createMockGrpcMethodViews(SurfaceTransformerContext context) {
    List<Method> methods = context.getInterface().getMethods();
    ArrayList<MockGrpcMethodView> mocks = new ArrayList<>(methods.size());
    for (Method method : methods) {
      MethodTransformerContext methodContext = context.asRequestMethodContext(method);
      String requestTypeName =
          methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType());
      String responseTypeName =
          methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
      MethodConfig methodConfig = methodContext.getMethodConfig();
      mocks.add(
          MockGrpcMethodView.newBuilder()
              .name(methodContext.getNamer().getApiMethodName(method, VisibilityConfig.PUBLIC))
              .requestTypeName(requestTypeName)
              .responseTypeName(responseTypeName)
              .grpcStreamingType(methodConfig.getGrpcStreamingType())
              .streamHandleTypeName(methodContext.getNamer().getStreamingServerName(method))
              .build());
    }
    return mocks;
  }

  public List<MockServiceUsageView> createMockServices(
      SurfaceNamer namer, Model model, ApiConfig apiConfig) {
    List<MockServiceUsageView> mockServices = new ArrayList<>();

    for (Interface service : getGrpcInterfacesToMock(model, apiConfig)) {
      MockServiceUsageView mockService =
          MockServiceUsageView.newBuilder()
              .className(namer.getMockServiceClassName(service))
              .varName(namer.getMockServiceVarName(service))
              .implName(namer.getMockGrpcServiceImplName(service))
              .registerFunctionName(namer.getServerRegisterFunctionName(service))
              .build();
      mockServices.add(mockService);
    }

    return mockServices;
  }

  public InitCodeContext createRequestInitCodeContext(
      MethodTransformerContext context,
      SymbolTable symbolTable,
      Iterable<FieldConfig> fieldConfigs,
      InitCodeOutputType outputType) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethod().getInputType())
        .symbolTable(symbolTable)
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldIterable(fieldConfigs))
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .outputType(outputType)
        .valueGenerator(valueGenerator)
        .build();
  }

  public List<PageStreamingResponseView> createPageStreamingResponseViews(
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

  public MockGrpcResponseView createMockResponseView(
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
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(primitiveFields)
        .fieldConfigMap(createResponseFieldConfigMap(context))
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

  private ImmutableMap<String, FieldConfig> createResponseFieldConfigMap(
      MethodTransformerContext context) {
    ApiConfig apiConfig = context.getApiConfig();
    ResourceNameMessageConfigs messageConfig = apiConfig.getResourceNameMessageConfigs();
    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        apiConfig.getResourceNameConfigs();
    ResourceNameTreatment treatment = context.getMethodConfig().getDefaultResourceNameTreatment();

    if (messageConfig == null || treatment == ResourceNameTreatment.NONE) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<String, FieldConfig> builder = ImmutableMap.builder();
    for (Field field : context.getMethod().getOutputMessage().getFields()) {
      if (messageConfig.fieldHasResourceName(field)) {
        ResourceNameConfig resourceNameConfig =
            resourceNameConfigs.get(messageConfig.getFieldResourceName(field));
        builder.put(
            field.getFullName(),
            FieldConfig.createFieldConfig(field, treatment, resourceNameConfig));
      }
    }
    return builder.build();
  }
}
