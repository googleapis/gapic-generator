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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.util.testing.JavaValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestAssertView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestCaseView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestClassView;
import com.google.api.codegen.viewmodel.testing.MockGrpcMethodView;
import com.google.api.codegen.viewmodel.testing.MockGrpcResponseView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.MockServiceView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** A subclass of ModelToViewTransformer which translates model into API tests in Java. */
public class JavaGapicSurfaceTestTransformer implements ModelToViewTransformer {

  private static String TEST_TEMPLATE_FILE = "java/test.snip";
  private static String MOCK_SERVICE_FILE = "java/mock_service.snip";
  private static String MOCK_SERVICE_IMPL_FILE = "java/mock_service_impl.snip";

  private final GapicCodePathMapper pathMapper;
  private final TestValueGenerator valueGenerator = new TestValueGenerator(new JavaValueProducer());

  public JavaGapicSurfaceTestTransformer(GapicCodePathMapper javaPathMapper) {
    this.pathMapper = javaPathMapper;
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> views = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      views.add(createTestClassView(context));
    }
    for (Interface service : getGrpcInterfacesToMock(model, apiConfig)) {
      SurfaceTransformerContext context = createContext(service, apiConfig);
      views.add(createMockServiceImplView(context));

      context = createContext(service, apiConfig);
      views.add(createMockServiceView(context));
    }
    return views;
  }

  private Iterable<Interface> getGrpcInterfacesToMock(Model model, ApiConfig apiConfig) {
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

    return interfaces.values();
  }

  private SurfaceTransformerContext createContext(Interface service, ApiConfig apiConfig) {
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new JavaTypeTable(apiConfig.getPackageName()),
            new JavaModelTypeNameConverter(apiConfig.getPackageName()));
    return SurfaceTransformerContext.create(
        service, apiConfig, typeTable, new JavaSurfaceNamer(apiConfig.getPackageName()));
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(TEST_TEMPLATE_FILE);
    fileNames.add(MOCK_SERVICE_IMPL_FILE);
    fileNames.add(MOCK_SERVICE_FILE);
    return fileNames;
  }

  private void addTestImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("org.junit.After");
    typeTable.saveNicknameFor("org.junit.AfterClass");
    typeTable.saveNicknameFor("org.junit.Assert");
    typeTable.saveNicknameFor("org.junit.Before");
    typeTable.saveNicknameFor("org.junit.BeforeClass");
    typeTable.saveNicknameFor("org.junit.Test");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.Arrays");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockServiceHelper");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockGrpcService");
    typeTable.saveNicknameFor("com.google.api.gax.core.PageAccessor");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessage");
  }

  private void addMockServiceImplImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.LinkedList");
    typeTable.saveNicknameFor("java.util.Queue");
    typeTable.saveNicknameFor("com.google.common.collect.Lists");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessage");
    typeTable.saveNicknameFor("io.grpc.stub.StreamObserver");
  }

  private void addMockServiceImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.api.gax.testing.MockGrpcService");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessage");
    typeTable.saveNicknameFor("io.grpc.ServerServiceDefinition");
  }

  private GapicSurfaceTestClassView createTestClassView(SurfaceTransformerContext context) {
    addTestImports(context);

    Interface service = context.getInterface();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    SurfaceNamer namer = context.getNamer();
    String name = namer.getTestClassName(service);
    ImportTypeTransformer importTypeTransformer = new ImportTypeTransformer();

    GapicSurfaceTestClassView testClass =
        GapicSurfaceTestClassView.newBuilder()
            .packageName(context.getApiConfig().getPackageName())
            .apiSettingsClassName(namer.getApiSettingsClassName(service))
            .apiClassName(namer.getApiWrapperClassName(service))
            .name(name)
            .testCases(createTestCaseViews(context))
            .mockServices(createMockServices(context))
            .outputPath(namer.getSourceFilePath(outputPath, name))
            .templateFileName(TEST_TEMPLATE_FILE)
            // Imports must be done as the last step to catch all imports.
            .imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()))
            .build();
    return testClass;
  }

  private List<GapicSurfaceTestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ArrayList<GapicSurfaceTestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getNonStreamingMethods()) {
      MethodTransformerContext methodContext = context.asMethodContext(method);
      MethodConfig methodConfig = methodContext.getMethodConfig();
      if (methodConfig.isFlattening()) {
        for (List<Field> paramFields : methodConfig.getFlattening().getFlatteningGroups()) {
          testCaseViews.add(createTestCaseView(methodContext, paramFields, testNameTable));
        }
      } else {
        // TODO: Add support of non-flattening method
        // Github issue: https://github.com/googleapis/toolkit/issues/393
        System.err.println(
            "Non-flattening method test is not supported yet for " + method.getSimpleName());
      }
    }
    return testCaseViews;
  }

  private List<MockServiceUsageView> createMockServices(SurfaceTransformerContext context) {
    List<MockServiceUsageView> mockServices = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    for (Interface service : getGrpcInterfacesToMock(context.getModel(), context.getApiConfig())) {
      MockServiceUsageView mockService =
          MockServiceUsageView.newBuilder()
              .className(namer.getMockServiceClassName(service))
              .varName(namer.getMockServiceVarName(service))
              .build();
      mockServices.add(mockService);
    }

    return mockServices;
  }

  private GapicSurfaceTestCaseView createTestCaseView(
      MethodTransformerContext methodContext, List<Field> paramFields, SymbolTable testNameTable) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();
    Method method = methodContext.getMethod();
    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();

    // This symbol table is used to produce unique variable names used in the initialization code.
    // Shared by both request and response views.
    SymbolTable initSymbolTable = new SymbolTable();

    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            methodContext, paramFields, initSymbolTable, valueGenerator);

    String resourceTypeName = "";
    String resourcesFieldGetterName = "";
    ApiMethodType type = ApiMethodType.FlattenedMethod;
    boolean isPageStreaming = methodConfig.isPageStreaming();
    if (isPageStreaming) {
      Field resourcesField = methodConfig.getPageStreaming().getResourcesField();
      resourceTypeName =
          methodContext.getTypeTable().getAndSaveNicknameForElementType(resourcesField.getType());
      resourcesFieldGetterName = namer.getFieldGetFunctionName(resourcesField);
      type = ApiMethodType.PagedFlattenedMethod;
    }

    String requestTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    String responseTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());

    List<GapicSurfaceTestAssertView> requestAssertViews =
        initCodeTransformer.generateRequestAssertViews(methodContext, paramFields);

    return GapicSurfaceTestCaseView.newBuilder()
        .name(getTestName(testNameTable, namer, method))
        .surfaceMethodName(namer.getApiMethodName(method))
        .hasReturnValue(!ServiceMessages.s_isEmptyType(method.getOutputType()))
        .requestTypeName(requestTypeName)
        .responseTypeName(responseTypeName)
        .initCode(initCodeView)
        .methodType(type)
        .resourceTypeName(resourceTypeName)
        .resourcesFieldGetterName(resourcesFieldGetterName)
        .asserts(requestAssertViews)
        .mockResponse(createMockResponseView(methodContext, initSymbolTable))
        .mockServiceVarName(namer.getMockServiceVarName(methodContext.getTargetInterface()))
        .build();
  }

  private MockGrpcResponseView createMockResponseView(
      MethodTransformerContext methodContext, SymbolTable symbolTable) {
    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateMockResponseObjectInitCode(
            methodContext, symbolTable, valueGenerator);

    String typeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(methodContext.getMethod().getOutputType());
    return MockGrpcResponseView.newBuilder().typeName(typeName).initCode(initCodeView).build();
  }

  private MockServiceView createMockServiceView(SurfaceTransformerContext context) {
    addMockServiceImports(context);

    Interface service = context.getInterface();
    SurfaceNamer namer = context.getNamer();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    String name = namer.getMockServiceClassName(context.getInterface());
    String grpcContainerName =
        context.getTypeTable().getAndSaveNicknameFor(namer.getGrpcContainerTypeName(service));
    ImportTypeTransformer importTypeTransformer = new ImportTypeTransformer();
    return MockServiceView.newBuilder()
        .name(name)
        .serviceImplClassName(namer.getMockGrpcServiceImplName(context.getInterface()))
        .packageName(context.getApiConfig().getPackageName())
        .grpcContainerName(grpcContainerName)
        .outputPath(namer.getSourceFilePath(outputPath, name))
        .templateFileName(MOCK_SERVICE_FILE)
        // Imports must be done as the last step to catch all imports.
        .imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()))
        .build();
  }

  private MockServiceImplView createMockServiceImplView(SurfaceTransformerContext context) {
    addMockServiceImplImports(context);

    Interface service = context.getInterface();
    SurfaceNamer namer = context.getNamer();
    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    String name = namer.getMockGrpcServiceImplName(context.getInterface());
    String grpcClassName =
        context.getTypeTable().getAndSaveNicknameFor(namer.getGrpcServiceClassName(service));
    ImportTypeTransformer importTypeTransformer = new ImportTypeTransformer();
    return MockServiceImplView.newBuilder()
        .name(name)
        .packageName(context.getApiConfig().getPackageName())
        .grpcMethods(createGrpcMethodViews(context))
        .grpcClassName(grpcClassName)
        .outputPath(namer.getSourceFilePath(outputPath, name))
        .templateFileName(MOCK_SERVICE_IMPL_FILE)
        // Imports must be done as the last step to catch all imports.
        .imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()))
        .build();
  }

  private List<MockGrpcMethodView> createGrpcMethodViews(SurfaceTransformerContext context) {
    ArrayList<MockGrpcMethodView> testCaseViews = new ArrayList<>();
    for (Method method : context.getInterface().getMethods()) {
      MethodTransformerContext methodContext = context.asMethodContext(method);
      testCaseViews.add(createGrpcMethodView(methodContext));
    }
    return testCaseViews;
  }

  private MockGrpcMethodView createGrpcMethodView(MethodTransformerContext methodContext) {
    Method method = methodContext.getMethod();
    String requestTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    String responseTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    return MockGrpcMethodView.newBuilder()
        .name(methodContext.getNamer().getApiMethodName(method))
        .requestTypeName(requestTypeName)
        .responseTypeName(responseTypeName)
        .isStreaming(method.getRequestStreaming() || method.getResponseStreaming())
        .build();
  }

  private String getTestName(SymbolTable symbolTable, SurfaceNamer namer, Method method) {
    Name name = symbolTable.getNewSymbol(Name.lowerCamel(namer.getTestCaseName(method)));
    return namer.methodName(name);
  }
}
