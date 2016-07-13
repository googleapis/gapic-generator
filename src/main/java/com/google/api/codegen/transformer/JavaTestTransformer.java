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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.viewmodel.ViewModelDoc;
import com.google.api.codegen.viewmodel.testing.ApiTestClassView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;

import java.util.ArrayList;
import java.util.List;

public class JavaTestTransformer implements Transformer {

  private ApiConfig apiConfig;
  private GapicCodePathMapper pathMapper;
  private ModelToJavaTypeTable typeTable;
  private JavaSurfaceNamer surfaceNamer;

  public JavaTestTransformer(ApiConfig apiConfig, GapicCodePathMapper javaPathMapper) {
    this.apiConfig = apiConfig;
    this.pathMapper = javaPathMapper;
    this.typeTable = new ModelToJavaTypeTable();
    this.surfaceNamer = new JavaSurfaceNamer();
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
    addImports();
    ApiTestClassView testClass = createTestClassView(service);

    List<ViewModelDoc> views = new ArrayList<>();
    views.add(testClass);
    return views;
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();
    fileNames.add(new ApiTestClassView().getTemplateFileName());
    // TODO(shinfan): Add more files
    return fileNames;
  }

  private void addImports() {
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

  private ApiTestClassView createTestClassView(Interface service) {
    ApiTestClassView testClass = new ApiTestClassView();
    testClass.packageName = apiConfig.getPackageName();
    testClass.apiSettingsClassName = service.getSimpleName() + "Settings";
    testClass.apiClassName = surfaceNamer.getApiWrapperClassName(service);
    testClass.name = testClass.apiClassName + "Test";
    testClass.mockServiceClassName = "Mock" + testClass.apiClassName;
    testClass.testCases = new ArrayList<>();

    String outputPath = pathMapper.getOutputPath(service, apiConfig);
    testClass.outputPath = outputPath + "/" + testClass.name + ".java";
    testClass.imports = typeTable.getImports();
    return testClass;
  }
}
