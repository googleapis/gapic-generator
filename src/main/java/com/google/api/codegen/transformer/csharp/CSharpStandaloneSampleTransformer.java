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
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.Collections;
import java.util.List;

/** A transformer that generates C# standalone samples. */
public class CSharpStandaloneSampleTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String SNIPPETS_TEMPLATE_FILENAME = "csharp/standalone_sample.snip";

  private static final StaticLangApiMethodTransformer csharpApiMethodTransformer =
      new CSharpApiMethodTransformer(
          SampleTransformer.newBuilder().sampleType(SampleSpec.SampleType.STANDALONE).build());

  private final GapicCodePathMapper pathMapper;

  public CSharpStandaloneSampleTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(SNIPPETS_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    String packageName = productConfig.getPackageName();
    CSharpSurfaceNamer namer = new CSharpSurfaceNamer(packageName);
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new CSharpTypeTable(packageName), new CSharpModelTypeNameConverter(packageName));

    List<InterfaceContext> interfaceContexts =
        Streams.stream(apiModel.getInterfaces(productConfig))
            .filter(i -> productConfig.hasInterfaceConfig(i))
            .map(
                i ->
                    GapicInterfaceContext.create(
                        i, productConfig, typeTable, namer, new RubyFeatureConfig()))
            .collect(ImmutableList.toImmutableList());
    ImmutableList.Builder<ViewModel> sampleFileViews = ImmutableList.builder();
    for (InterfaceContext interfaceContext : interfaceContexts) {
      List<StaticLangApiMethodView> methods =
          csharpApiMethodTransformer.generateApiMethods(interfaceContext);
      for (StaticLangApiMethodView method : methods.samples()) {
        sampleFileViews.add(newSampleFileView(interfaceContext, method, sample, namer));
      }
    }
    return sampleFileViews;
  }

  private StaticLangFileView newSampleFileView(
      InterfaceContext context,
      OptionalArrayMethodView method,
      MethodSampleView sample,
      SurfaceNamer namer) {
    SampleFileRegistry registry = new SampleFileRegistry();
    String callingForm = sample.callingForm().toLowerUnderscore();
    String valueSet = sample.valueSet().id();
    String regionTag = sample.regionTag();
    String sampleOutputPath =
        Paths.get(
                RUBY_SAMPLE_PACKAGE_NAME,
                Name.anyLower(method.name(), callingForm, valueSet).toLowerUnderscore() + ".rb")
            .toString();

    registry.addFile(sampleOutputPath, method.name(), callingForm, valueSet, regionTag);

    return DynamicLangSampleView.newBuilder()
        .templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME)
        .fileHeader(fileHeaderTransformer.generateFileHeader(context))
        .outputPath(sampleOutputPath)
        .libraryMethod(method)
        .sample(sample)
        .gapicPackageName(namer.getGapicPackageName(packageConfig.packageName()))
        .build();
  }
}
