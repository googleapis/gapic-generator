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
package com.google.api.codegen.discovery.transformer.java;

import java.util.ArrayList;
import java.util.List;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.SampleConfig;
import com.google.api.codegen.discovery.TypeInfo;
import com.google.api.codegen.discovery.transformer.MethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleNamer;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.viewmodel.SampleView;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.protobuf.Method;

/*
 * Transforms a Model into the standard discovery surface in Java.
 */
public class JavaMethodToViewTransformer implements MethodToViewTransformer {

  private final static String TEMPLATE_FILENAME = "java/discovery_fragment.snip";

  public JavaMethodToViewTransformer() {}

  @Override
  public ViewModel transform(Method method, ApiaryConfig apiaryConfig) {
    SampleConfig sampleConfig = SampleConfig.createSampleConfig(method, apiaryConfig);
    JavaSampleNamer namer = new JavaSampleNamer();
    SampleTransformerContext context =
        SampleTransformerContext.create(sampleConfig, createTypeTable(), namer);
    return getSample(context);
  }

  /*
   * Returns a new Java TypeTable.
   */
  private SampleTypeTable createTypeTable() {
    return new SampleTypeTable(new JavaTypeTable(), new JavaProtobufTypeNameConverter());
  }

  private SampleView getSample(SampleTransformerContext context) {
    addStaticImports(context);
    SampleConfig sampleConfig = context.getSampleConfig();
    SampleTypeTable typeTable = context.getTypeTable();
    SampleNamer namer = context.getNamer();

    SampleView.Builder sampleView = SampleView.newBuilder();
    sampleView.templateFileName(TEMPLATE_FILENAME);
    sampleView.outputPath("output");
    sampleView.apiTitle(sampleConfig.apiTitle());
    sampleView.apiName(sampleConfig.apiName());
    sampleView.apiVersion(sampleConfig.apiVersion());
    sampleView.clientClassName(namer.getClientClassName(sampleConfig));
    // Defaults...
    sampleView.requestClassName("");
    sampleView.responseClassName("");
    sampleView.hasRequest(sampleConfig.methodInfo().hasRequest());
    if (sampleConfig.methodInfo().hasRequest()) {
      sampleView.requestClassName(
          typeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().requestType()));
    }
    sampleView.hasResponse(sampleConfig.methodInfo().hasResponse());
    if (sampleConfig.methodInfo().hasResponse()) {
      sampleView.responseClassName(
          typeTable.getAndSaveNicknameFor(sampleConfig.methodInfo().responseType()));
    }
    List<TypeInfo> fields = sampleConfig.methodInfo().paramTypes();
    List<String> paramVarNames = new ArrayList<String>();
    for (TypeInfo field : fields) {
      paramVarNames.add(namer.getParamVarName(field));
    }
    sampleView.paramVarNames(paramVarNames);
    sampleView.imports(typeTable.getImports());

    return sampleView.build();
  }

  private void addStaticImports(SampleTransformerContext context) {
    SampleTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.client.http.HttpTransport");
    typeTable.saveNicknameFor("com.google.api.client.json.jackson2.JacksonFactory");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.security.GeneralSecurityException");
    typeTable.saveNicknameFor("java.util.Collections");
  }
}
