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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.ServiceMessages;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ResourceNameMessageConfigs;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.util.testing.JavaValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestAssertView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestCaseView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestClassView;
import com.google.api.codegen.viewmodel.testing.MockGrpcResponseView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.PageStreamingResponseView;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A subclass of ModelToViewTransformer which translates model into API tests in Ruby. */
// TODO(jcanizales): Much of this implementation is necessarily duplicated from the Java one, even
// for things not used in Ruby tests. Find a way to remove the redundancy.
public class RubyTestsTransformer implements ModelToViewTransformer {
  // Template files
  private static String UNIT_TEST_TEMPLATE_FILE = "ruby/test.snip";

  private final FileHeaderTransformer fileHeaderTransformer = new FileHeaderTransformer();
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  // It looks like this isn't producing anything that's specific to Java?
  // TODO(jcanizales): Use same value generator as sample gen!
  private final TestValueGenerator valueGenerator = new TestValueGenerator(new JavaValueProducer());

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(UNIT_TEST_TEMPLATE_FILE);
    return fileNames;
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {

    SurfaceNamer namer = new RubySurfaceNamer(apiConfig.getPackageName());

    List<ViewModel> views = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      views.add(createTestClassView(service, namer, apiConfig));
    }
    return views;
  }

  private GapicSurfaceTestClassView createTestClassView(
      Interface service, SurfaceNamer namer, ApiConfig apiConfig) {
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new RubyTypeTable(apiConfig.getPackageName()),
            new RubyModelTypeNameConverter(apiConfig.getPackageName()));
    SurfaceTransformerContext context =
        SurfaceTransformerContext.create(
            service, apiConfig, typeTable, namer, new RubyFeatureConfig());

    GapicSurfaceTestClassView.Builder testClass =
        GapicSurfaceTestClassView.newBuilder()
            // We want something like language_service_api_test.rb (i.e. append _test to the name of
            // the generated VKit client).
            .outputPath("tests/" + namer.getServiceFileName(service) + "_test.rb")
            .templateFileName(UNIT_TEST_TEMPLATE_FILE)
            .service(service)
            .name(namer.getUnitTestClassName(service))
            .apiClassName(namer.getApiWrapperClassName(service))
            .apiSettingsClassName(namer.getApiSettingsClassName(service)) // What is this?
            .mockServices(Collections.<MockServiceUsageView>emptyList())
            .testCases(createTestCaseViews(context));

    // Imports must be done as the last step to catch all imports.
    testClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return testClass.build();
  }

  private List<GapicSurfaceTestCaseView> createTestCaseViews(SurfaceTransformerContext context) {
    ArrayList<GapicSurfaceTestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
        MethodTransformerContext methodContext =
            context.asFlattenedMethodContext(method, flatteningGroup);
        testCaseViews.add(
            createTestCaseView(
                methodContext, testNameTable, flatteningGroup.getFlattenedFieldConfigs().values()));
      }
    }
    return testCaseViews;
  }

  private GapicSurfaceTestCaseView createTestCaseView(
      MethodTransformerContext methodContext,
      SymbolTable testNameTable,
      Iterable<FieldConfig> paramFieldConfigs) {
    MethodConfig methodConfig = methodContext.getMethodConfig();
    SurfaceNamer namer = methodContext.getNamer();
    Method method = methodContext.getMethod();

    // This symbol table is used to produce unique variable names used in the initialization code.
    // Shared by both request and response views.
    SymbolTable initSymbolTable = new SymbolTable();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            methodContext,
            createRequestInitCodeContext(
                methodContext, initSymbolTable, paramFieldConfigs, InitCodeOutputType.FieldList));

    String requestTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getInputType());
    String responseTypeName =
        methodContext.getTypeTable().getAndSaveNicknameFor(method.getOutputType());
    String surfaceMethodName = namer.getApiMethodName(method, methodConfig.getVisibility());

    ApiMethodType type = ApiMethodType.FlattenedMethod;

    List<GapicSurfaceTestAssertView> requestAssertViews =
        initCodeTransformer.generateRequestAssertViews(methodContext, paramFieldConfigs);

    return GapicSurfaceTestCaseView.newBuilder()
        .name(namer.getTestCaseName(testNameTable, method))
        .surfaceMethodName(surfaceMethodName)
        .hasReturnValue(!ServiceMessages.s_isEmptyType(method.getOutputType()))
        .requestTypeName(requestTypeName)
        .responseTypeName(responseTypeName)
        .initCode(initCodeView)
        .methodType(type)
        .asserts(requestAssertViews)
        .mockResponse(createMockResponseView(methodContext, initSymbolTable))
        .mockServiceVarName(namer.getMockServiceVarName(methodContext.getTargetInterface()))
        .grpcStreamingType(methodConfig.getGrpcStreamingType())
        .pageStreamingResponseViews(new ArrayList<PageStreamingResponseView>())
        .build();
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

  private InitCodeContext createRequestInitCodeContext(
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

  private InitCodeContext createResponseInitCodeContext(
      MethodTransformerContext context, SymbolTable symbolTable) {
    ArrayList<Field> primitiveFields = new ArrayList<>();
    for (Field field : context.getMethod().getOutputMessage().getFields()) {
      if (field.getType().isPrimitive() && !field.getType().isRepeated()) {
        primitiveFields.add(field);
      }
    }
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethod().getOutputType())
        .symbolTable(symbolTable)
        .suggestedName(Name.from("expected_response"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(primitiveFields)
        .fieldConfigMap(createResponseFieldConfigMap(context))
        .valueGenerator(valueGenerator)
        .build();
  }

  private ImmutableMap<String, FieldConfig> createResponseFieldConfigMap(
      MethodTransformerContext context) {
    ApiConfig apiConfig = context.getApiConfig();
    ResourceNameMessageConfigs messageConfig = apiConfig.getResourceNameMessageConfigs();
    ResourceNameTreatment treatment = context.getMethodConfig().getDefaultResourceNameTreatment();

    if (messageConfig == null || treatment == ResourceNameTreatment.NONE) {
      return ImmutableMap.of();
    }
    ImmutableMap.Builder<String, FieldConfig> builder = ImmutableMap.builder();
    for (Field field : context.getMethod().getOutputMessage().getFields()) {
      if (messageConfig.fieldHasResourceName(field)) {
        builder.put(
            field.getFullName(),
            FieldConfig.createFieldConfig(
                field, treatment, messageConfig.getFieldResourceName(field)));
      }
    }
    return builder.build();
  }
}
