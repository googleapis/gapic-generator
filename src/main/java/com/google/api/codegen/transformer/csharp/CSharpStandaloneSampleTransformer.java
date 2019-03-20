/* Copyright 2019 Google LLC
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

import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleFileRegistry;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangFileView;
import com.google.api.codegen.viewmodel.StaticLangSampleClassView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/** A transformer that generates C# standalone samples. */
public class CSharpStandaloneSampleTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "csharp/standalone_sample.snip";
  private static final String CSHARP_SAMPLE_PACKAGE_NAME = "Snippets";
  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.Global;
  private static final CSharpCommonTransformer csharpCommonTransformer =
      new CSharpCommonTransformer();

  private static final StaticLangApiMethodTransformer csharpApiMethodTransformer =
      new CSharpApiMethodTransformer(
          SampleTransformer.newBuilder().sampleType(SampleSpec.SampleType.STANDALONE).build());
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(STANDALONE_SAMPLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    String packageName = productConfig.getPackageName();
    CSharpSurfaceNamer namer = new CSharpSurfaceNamer(packageName, ALIAS_MODE);
    ModelTypeTable typeTable =
        csharpCommonTransformer.createTypeTable(productConfig.getPackageName(), ALIAS_MODE);

    List<InterfaceContext> interfaceContexts =
        Streams.stream(model.getInterfaces(productConfig))
            .filter(i -> productConfig.hasInterfaceConfig(i))
            .map(
                i ->
                    GapicInterfaceContext.create(
                        i, productConfig, typeTable, namer, new CSharpFeatureConfig()))
            .collect(ImmutableList.toImmutableList());
    ImmutableList.Builder<ViewModel> sampleFileViews = ImmutableList.builder();
    for (InterfaceContext interfaceContext : interfaceContexts) {
      List<StaticLangApiMethodView> methods =
          csharpApiMethodTransformer.generateApiMethods(interfaceContext);
      for (StaticLangApiMethodView method : methods) {
        for (MethodSampleView sample : method.samples()) {
          sampleFileViews.add(newSampleFileView(interfaceContext, method, sample, namer));
        }
      }
    }
    return sampleFileViews.build();
  }

  private StaticLangFileView newSampleFileView(
      InterfaceContext context,
      StaticLangApiMethodView method,
      MethodSampleView sample,
      SurfaceNamer namer) {
    SampleFileRegistry registry = new SampleFileRegistry();
    String callingForm = sample.callingForm().toLowerUnderscore();
    String valueSet = sample.valueSet().id();
    String regionTag = sample.regionTag();
    String sampleClassName = method.name() + Name.anyLower(callingForm, valueSet).toUpperCamel();
    String sampleOutputPath =
        Paths.get(CSHARP_SAMPLE_PACKAGE_NAME, sampleClassName + ".cs").toString();

    registry.addFile(sampleOutputPath, method.name(), callingForm, valueSet, regionTag);

    StaticLangSampleClassView sampleClassView =
        StaticLangSampleClassView.newBuilder()
            .name(sampleClassName)
            .libraryMethod(method)
            .sample(sample)
            .build();

    return StaticLangFileView.<StaticLangSampleClassView>newBuilder()
        .templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME)
        .fileHeader(fileHeaderTransformer.generateFileHeader(context))
        .outputPath(sampleOutputPath)
        .classView(sampleClassView)
        .build();
  }
}
