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
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.Transformer;
import com.google.api.codegen.viewmodel.ViewModelDoc;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestCaseView;
import com.google.api.codegen.viewmodel.testing.GapicSurfaceTestClassView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;

import java.util.ArrayList;
import java.util.List;

public class JavaGapicSurfaceTestTransformer implements Transformer {

  private ApiConfig apiConfig;
  private GapicCodePathMapper pathMapper;

  public JavaGapicSurfaceTestTransformer(ApiConfig apiConfig, GapicCodePathMapper javaPathMapper) {
    this.apiConfig = apiConfig;
    this.pathMapper = javaPathMapper;
  }

  @Override
  public List<ViewModelDoc> transform(Model model) {
    List<ViewModelDoc> views = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      views.addAll(transform(service));
    }
    return views;
  }

  public List<ViewModelDoc> transform(Interface service) {
    SurfaceTransformerContext context =
        SurfaceTransformerContext.create(
            service, this.apiConfig, new ModelToJavaTypeTable(), new JavaSurfaceNamer());
    addImports(context);
    GapicSurfaceTestClassView testClass = createTestClassView(service, context);

    List<ViewModelDoc> views = new ArrayList<>();
    views.add(testClass);
    return views;
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(new GapicSurfaceTestClassView().getTemplateFileName());
    // TODO(shinfan): Add more files
    return fileNames;
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
      Interface service, SurfaceTransformerContext context) {
    GapicSurfaceTestClassView testClass = new GapicSurfaceTestClassView();
    testClass.packageName = context.getApiConfig().getPackageName();
    testClass.apiSettingsClassName = service.getSimpleName() + "Settings";
    testClass.apiClassName = context.getNamer().getApiWrapperClassName(service);
    testClass.name = context.getNamer().getTestClassName(service);
    testClass.mockServiceClassName = "Mock" + testClass.apiClassName;
    testClass.testCases = createTestCaseViews(service, context);

    String outputPath = pathMapper.getOutputPath(service, apiConfig);
    testClass.outputPath = outputPath + "/" + testClass.name + ".java";
    testClass.imports = context.getTypeTable().getImports();
    return testClass;
  }

  private List<GapicSurfaceTestCaseView> createTestCaseViews(
      Interface service, SurfaceTransformerContext context) {
    ArrayList<GapicSurfaceTestCaseView> testCaseViews = new ArrayList<>();
    for (Method method : service.getMethods()) {
      MethodConfig methodConfig = apiConfig.getInterfaceConfig(service).getMethodConfig(method);
      MethodTransformerContext methodContext =
          MethodTransformerContext.create(
              service, apiConfig, context.getTypeTable(), context.getNamer(), method, methodConfig);
      GapicSurfaceTestCaseView testCaseView = new GapicSurfaceTestCaseView();
      testCaseView.name = methodContext.getNamer().getTestCaseName(method);
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
