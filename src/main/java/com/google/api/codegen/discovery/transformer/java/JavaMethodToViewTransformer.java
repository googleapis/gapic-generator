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

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.discovery.SampleConfig;
import com.google.api.codegen.discovery.transformer.MethodToViewTransformer;
import com.google.api.codegen.discovery.transformer.SampleParamTransformer;
import com.google.api.codegen.discovery.transformer.SampleTypeTable;
import com.google.api.codegen.discovery.transformer.SampleTransformerContext;
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
    addSampleImports(context);
    return SampleView.newBuilder()
        .templateFileName(TEMPLATE_FILENAME)
        .outputPath("output")
        .apiTitle(context.getSampleConfig().apiTitle())
        .apiName(context.getSampleConfig().apiName())
        .apiVersion(context.getSampleConfig().apiVersion())
        .params(SampleParamTransformer.generateSampleParams(context))
        .imports(context.getTypeTable().getImports())
        .build();
  }

  private void addSampleImports(SampleTransformerContext context) {
    SampleTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("com.google.api.codegen.Swag");
    typeTable.saveNicknameFor("com.google.api.codegen.Dab");
  }
}
