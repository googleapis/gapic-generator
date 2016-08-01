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
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestCaseView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestClassView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;

import java.util.ArrayList;
import java.util.List;

public class JavaGapicSurfaceTestTransformer implements ModelToViewTransformer {

  static String TEST_TEMPLATE_FILE = "java/test.snip";

  private GapicCodePathMapper pathMapper;

  public JavaGapicSurfaceTestTransformer(GapicCodePathMapper javaPathMapper) {
    this.pathMapper = javaPathMapper;
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> views = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, createTypeTable(), new JavaSurfaceNamer());
      addImports(context);
      GapicSurfaceTestClassView testClass = createTestClassView(service, context, apiConfig);
      views.add(testClass);
    }
    return views;
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(TEST_TEMPLATE_FILE);
    // TODO(shinfan): Add more files
    return fileNames;
  }

  private ModelTypeTable createTypeTable() {
    return new ModelTypeTable(new JavaTypeTable(), new JavaModelTypeNameConverter());
  }

  private void addImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("org.junit.After");
    typeTable.saveNicknameFor("org.junit.AfterClass");
    typeTable.saveNicknameFor("org.junit.Before");
    typeTable.saveNicknameFor("org.junit.BeforeClass");
    typeTable.saveNicknameFor("org.junit.Test");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("com.google.api.gax.testing.ValueGenerator");
    typeTable.saveNicknameFor("com.google.api.gax.testing.LocalServiceHelper");
    typeTable.saveNicknameFor("com.google.protobuf.GeneratedMessage");
    typeTable.saveNicknameFor("junit.framework.Assert");
  }

  private GapicSurfaceTestClassView createTestClassView(
      Interface service, SurfaceTransformerContext context, ApiConfig apiConfig) {
    String outputPath = pathMapper.getOutputPath(service, apiConfig);

    GapicSurfaceTestClassView testClass =
        GapicSurfaceTestClassView.newBuilder()
            .packageName(context.getApiConfig().getPackageName())
            .apiSettingsClassName(context.getNamer().getApiSettingsClassName(service))
            .apiClassName(context.getNamer().getApiWrapperClassName(service))
            .name(context.getNamer().getTestClassName(service))
            .mockServiceClassName(context.getNamer().getMockServiceClassName(service))
            .testCases(createTestCaseViews(service, context, apiConfig))
            .outputPath(outputPath)
            .imports(context.getTypeTable().getImports())
            .templateFileName(TEST_TEMPLATE_FILE)
            .build();
    return testClass;
  }

  private List<GapicSurfaceTestCaseView> createTestCaseViews(
      Interface service, SurfaceTransformerContext context, ApiConfig apiConfig) {
    ArrayList<GapicSurfaceTestCaseView> testCaseViews = new ArrayList<>();
    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);
      MethodTransformerContext methodContext =
          MethodTransformerContext.create(
              service, apiConfig, context.getTypeTable(), context.getNamer(), method, methodConfig);
      GapicSurfaceTestCaseView testCaseView = new GapicSurfaceTestCaseView();
      testCaseView.name = methodContext.getNamer().getTestClassName(service);
      testCaseView.methodName = method.getSimpleName();
      testCaseView.requestTypeName =
          context.getTypeTable().getAndSaveNicknameFor(method.getInputType());
      InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
      testCaseView.initCode =
          initCodeTransformer.generateInitCode(methodContext, methodConfig.getRequiredFields());
      testCaseView.isPageStreaming = methodConfig.isPageStreaming();
      if (testCaseView.isPageStreaming) {
        testCaseView.resourceTypeName =
            methodContext
                .getTypeTable()
                .getAndSaveNicknameForElementType(
                    methodConfig.getPageStreaming().getResourcesField().getType());
      } else {
        testCaseView.resourceTypeName = "";
      }

      testCaseView.asserts =
          initCodeTransformer.generateTestAssertViews(
              methodContext, methodConfig.getRequiredFields());
      testCaseViews.add(testCaseView);
    }

    return testCaseViews;
  }
}
