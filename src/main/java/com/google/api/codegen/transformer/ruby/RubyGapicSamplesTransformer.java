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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleFileRegistry;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.CaseFormat;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.nio.file.Paths;
import java.util.List;

public class RubyGapicSamplesTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "ruby/standalone_sample.snip";
  private static final SampleSpec.SampleType sampleType = SampleSpec.SampleType.STANDALONE;

  private static final String RUBY_SAMPLE_PACKAGE_NAME = "sample";

  private final RubyImportSectionTransformer importSectionTransformer =
      new RubyImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new RubyApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer))
              .sampleType(sampleType)
              .build());
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;

  public RubyGapicSamplesTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(STANDALONE_SAMPLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel apiModel, GapicProductConfig productConfig) {
    String packageName = productConfig.getPackageName();
    RubySurfaceNamer namer = new RubySurfaceNamer(packageName);
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new RubyTypeTable(packageName), new RubyModelTypeNameConverter(packageName));

    List<InterfaceContext> interfaceContexts =
        Streams.stream(apiModel.getInterfaces(productConfig))
            .filter(i -> productConfig.hasInterfaceConfig(i))
            .map(
                i ->
                    GapicInterfaceContext.create(
                        i, productConfig, typeTable, namer, new RubyFeatureConfig()))
            .collect(ImmutableList.toImmutableList());
    ImmutableList.Builder<ViewModel> sampleFileViews = ImmutableList.builder();
    for (InterfaceContext context : interfaceContexts) {
      List<OptionalArrayMethodView> methods = apiMethodTransformer.generateApiMethods(context);
      for (OptionalArrayMethodView method : methods) {
        for (MethodSampleView sample : method.samples()) {
          sampleFileViews.add(newSampleFileView(context, method, sample, namer));
        }
      }
    }
    return sampleFileViews.build();
  }

  private DynamicLangSampleView newSampleFileView(
      InterfaceContext context,
      OptionalArrayMethodView method,
      MethodSampleView sample,
      SurfaceNamer namer) {
    SampleFileRegistry registry = new SampleFileRegistry();
    String callingForm = sample.callingForm().toLowerCamel();
    String valueSet = sample.valueSet().id();
    String regionTag = sample.regionTag();
    String sampleOutputPath =
        Paths.get(
                RUBY_SAMPLE_PACKAGE_NAME,
                namer.getApiSampleClassName(
                    CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, method.name()),
                    callingForm,
                    valueSet))
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
