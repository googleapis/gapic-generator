/* Copyright 2018 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.Synchronicity;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSharpGapicUnitTestTransformer implements ModelToViewTransformer {

  private static final String UNITTEST_SNIPPETS_TEMPLATE_FILENAME = "csharp/gapic_unittest.snip";
  private static final String UNITTEST_CSPROJ_TEMPLATE_FILENAME =
      "csharp/gapic_unittest_csproj.snip";

  private final GapicCodePathMapper pathMapper;
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();

  public CSharpGapicUnitTestTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName());

    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              csharpCommonTransformer.createTypeTable(namer.getPackageName()),
              namer,
              new CSharpFeatureConfig());
      csharpCommonTransformer.addCommonImports(context);
      ModelTypeTable typeTable = context.getImportTypeTable();
      typeTable.saveNicknameFor("Xunit.FactAttribute");
      typeTable.saveNicknameFor("Moq.Mock");
      if (context.getLongRunningMethods().iterator().hasNext()) {
        typeTable.saveNicknameFor("Google.LongRunning.Operations");
      }
      surfaceDocs.add(generateUnitTest(context));
      surfaceDocs.add(generateUnitTestCsproj(context));
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(UNITTEST_SNIPPETS_TEMPLATE_FILENAME, UNITTEST_CSPROJ_TEMPLATE_FILENAME);
  }

  private ClientTestFileView generateUnitTestCsproj(GapicInterfaceContext context) {
    ClientTestFileView.Builder builder = generateUnitTestBuilder(context);
    GapicProductConfig productConfig = context.getProductConfig();
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), productConfig);
    builder.outputPath(
        outputPath + File.separator + productConfig.getPackageName() + ".Tests.csproj");
    builder.templateFileName(UNITTEST_CSPROJ_TEMPLATE_FILENAME);
    return builder.build();
  }

  private ClientTestFileView generateUnitTest(GapicInterfaceContext context) {
    ClientTestFileView.Builder builder = generateUnitTestBuilder(context);
    SurfaceNamer namer = context.getNamer();
    String name = namer.getUnitTestClassName(context.getInterfaceConfig());
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    builder.outputPath(outputPath + File.separator + name + ".g.cs");
    builder.templateFileName(UNITTEST_SNIPPETS_TEMPLATE_FILENAME);
    return builder.build();
  }

  private ClientTestFileView.Builder generateUnitTestBuilder(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getUnitTestClassName(context.getInterfaceConfig());

    ClientTestClassView.Builder testClass = ClientTestClassView.newBuilder();
    testClass.apiSettingsClassName(namer.getApiSettingsClassName(context.getInterfaceConfig()));
    testClass.apiClassName(namer.getApiWrapperClassName(context.getInterfaceConfig()));
    testClass.apiVariableName("client");
    testClass.name(name);
    testClass.testCases(createTestCaseViews(context));
    testClass.apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations());
    testClass.mockServices(
        mockServiceTransformer.createMockServices(
            context.getNamer(), context.getApiModel(), context.getProductConfig()));
    testClass.grpcServiceClassName(namer.getGrpcServiceClassName(context.getInterfaceModel()));

    testClass.missingDefaultServiceAddress(
        !context.getInterfaceConfig().hasDefaultServiceAddress());
    testClass.missingDefaultServiceScopes(!context.getInterfaceConfig().hasDefaultServiceScopes());
    testClass.reroutedGrpcClients(csharpCommonTransformer.generateReroutedGrpcView(context));
    testClass.hasLongRunningOperations(context.getLongRunningMethods().iterator().hasNext());

    ClientTestFileView.Builder testFile = ClientTestFileView.newBuilder();
    testFile.testClass(testClass.build());

    // Imports must be done as the last step to catch all imports.
    FileHeaderView fileHeader = fileHeaderTransformer.generateFileHeader(context);
    testFile.fileHeader(fileHeader);

    return testFile;
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ArrayList<TestCaseView> testCaseViews = new ArrayList<>();
    SymbolTable testNameTable = new SymbolTable();
    for (MethodModel method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (methodConfig.isGrpcStreaming()) {
        // TODO: Add support for streaming methods
      } else if (methodConfig.isFlattening()) {
        ClientMethodType clientMethodTypeSync;
        ClientMethodType clientMethodTypeAsync;
        if (methodConfig.isPageStreaming()) {
          // TODO: Add support for page-streaming methods
          continue;
        } else if (methodConfig.isLongRunningOperation()) {
          // TODO: Add support for LRO methods
          continue;
        } else {
          clientMethodTypeSync = ClientMethodType.FlattenedMethod;
          clientMethodTypeAsync = ClientMethodType.FlattenedAsyncCallSettingsMethod;
        }
        if (methodConfig.getRerouteToGrpcInterface() != null) {
          // TODO: Add support for rerouted methods
          continue;
        }
        for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
          GapicMethodContext methodContext =
              context.asFlattenedMethodContext(method, flatteningGroup);
          InitCodeContext initCodeContextSync =
              initCodeTransformer.createRequestInitCodeContext(
                  methodContext,
                  new SymbolTable(),
                  flatteningGroup.getFlattenedFieldConfigs().values(),
                  InitCodeOutputType.FieldList,
                  valueGenerator);
          testCaseViews.add(
              testCaseTransformer.createTestCaseView(
                  methodContext,
                  testNameTable,
                  initCodeContextSync,
                  clientMethodTypeSync,
                  Synchronicity.Sync));
          InitCodeContext initCodeContextAsync =
              initCodeTransformer.createRequestInitCodeContext(
                  methodContext,
                  new SymbolTable(),
                  flatteningGroup.getFlattenedFieldConfigs().values(),
                  InitCodeOutputType.FieldList,
                  valueGenerator);
          testCaseViews.add(
              testCaseTransformer.createTestCaseView(
                  methodContext,
                  testNameTable,
                  initCodeContextAsync,
                  clientMethodTypeAsync,
                  Synchronicity.Async));
        }
      } else {
        // TODO: Add support for non-flattening method
      }
    }
    return testCaseViews;
  }
}
