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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.util.testing.PythonValueProducer;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.ClientTestClassView;
import com.google.api.codegen.viewmodel.testing.ClientTestFileView;
import com.google.api.codegen.viewmodel.testing.MockServiceUsageView;
import com.google.api.codegen.viewmodel.testing.TestCaseView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Transforms the model into API tests for Python. Responsible for producing a list of
 * ClientTestFileViews for unit tests.
 */
public class PythonGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String TEST_TEMPLATE_FILE = "py/test.snip";

  private final GapicCodePathMapper pathMapper;
  private final ValueProducer valueProducer = new PythonValueProducer();
  private final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final TestValueGenerator valueGenerator = new TestValueGenerator(valueProducer);
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final FeatureConfig featureConfig = new DefaultFeatureConfig();

  public PythonGapicSurfaceTestTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.<String>of(TEST_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    SurfaceNamer surfacePackageNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    SurfaceNamer testPackageNamer =
        new PythonSurfaceNamer(surfacePackageNamer.getTestPackageName());
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable typeTable = createTypeTable(surfacePackageNamer.getTestPackageName());
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, typeTable, surfacePackageNamer, featureConfig);
      String testClassName = surfacePackageNamer.getUnitTestClassName(context.getInterfaceConfig());
      ClientTestClassView testClassView =
          ClientTestClassView.newBuilder()
              .apiSettingsClassName(
                  surfacePackageNamer.getNotImplementedString(
                      "PythonGapicSurfaceTestTransformer.transform - apiSettingsClassName"))
              .apiClassName(
                  surfacePackageNamer.getApiWrapperClassName(context.getInterfaceConfig()))
              .apiVariableName(
                  surfacePackageNamer.getApiWrapperVariableName(context.getInterfaceConfig()))
              .name(testClassName)
              .apiName(
                  surfacePackageNamer.publicClassName(
                      Name.upperCamelKeepUpperAcronyms(apiInterface.getSimpleName())))
              .testCases(createTestCaseViews(context))
              .apiHasLongRunningMethods(context.getInterfaceConfig().hasLongRunningOperations())
              .mockServices(ImmutableList.<MockServiceUsageView>of())
              .build();

      String outputPath = pathMapper.getOutputPath(context.getInterface(), productConfig);
      ImportSectionView importSection = importSectionTransformer.generateTestImportSection(context);
      models.add(
          ClientTestFileView.newBuilder()
              .templateFileName(TEST_TEMPLATE_FILE)
              .outputPath(surfacePackageNamer.getSourceFilePath(outputPath, testClassName))
              .testClass(testClassView)
              .fileHeader(
                  fileHeaderTransformer.generateFileHeader(
                      productConfig, importSection, testPackageNamer))
              .build());
    }
    return models.build();
  }

  private static ModelTypeTable createTypeTable(String packageName) {
    return new ModelTypeTable(
        new PythonTypeTable(packageName), new PythonModelTypeNameConverter(packageName));
  }

  private List<TestCaseView> createTestCaseViews(GapicInterfaceContext context) {
    ImmutableList.Builder<TestCaseView> testCaseViews = ImmutableList.builder();
    SymbolTable testNameTable = new SymbolTable();
    for (Method method : context.getSupportedMethods()) {
      GapicMethodContext methodContext = context.asRequestMethodContext(method);

      if (methodContext.getMethodConfig().isGrpcStreaming()) {
        // TODO(eoogbe): Remove this check once grpc streaming is supported by test
        continue;
      }

      ClientMethodType clientMethodType = ClientMethodType.OptionalArrayMethod;
      if (methodContext.getMethodConfig().isLongRunningOperation()) {
        clientMethodType = ClientMethodType.OperationOptionalArrayMethod;
      } else if (methodContext.getMethodConfig().isPageStreaming()) {
        clientMethodType = ClientMethodType.PagedOptionalArrayMethod;
      }

      Iterable<FieldConfig> fieldConfigs =
          methodContext.getMethodConfig().getRequiredFieldConfigs();
      InitCodeContext initCodeContext =
          InitCodeContext.newBuilder()
              .initObjectType(methodContext.getMethod().getInputType())
              .suggestedName(Name.from("request"))
              .initFieldConfigStrings(methodContext.getMethodConfig().getSampleCodeInitFields())
              .initValueConfigMap(InitCodeTransformer.createCollectionMap(methodContext))
              .initFields(FieldConfig.toFieldIterable(fieldConfigs))
              .outputType(InitCodeOutputType.FieldList)
              .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
              .valueGenerator(valueGenerator)
              .build();

      testCaseViews.add(
          testCaseTransformer.createTestCaseView(
              methodContext, testNameTable, initCodeContext, clientMethodType));
    }
    return testCaseViews.build();
  }
}
