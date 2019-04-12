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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleFileRegistry;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * A transformer to generate NodeJS standalone samples for each method in the GAPIC surface
 * generated from the same ApiModel.
 */
public class NodeJSGapicSamplesTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "nodejs/standalone_sample.snip";

  private final GapicCodePathMapper pathMapper;
  private final NodeJSImportSectionTransformer importSectionTransformer =
      new NodeJSImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new NodeJSApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer, false))
              .sampleImportTransformer(new NodeJSSampleImportTransformer())
              .sampleType(SampleType.STANDALONE)
              .build());
  private final NodeJSMethodViewGenerator methodGenerator =
      new NodeJSMethodViewGenerator(apiMethodTransformer);
  private final PackageMetadataConfig packageConfig;

  public NodeJSGapicSamplesTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(STANDALONE_SAMPLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    models.addAll(generateSampleClassesForModel(model, productConfig));
    return models.build();
  }

  private List<ViewModel> generateSampleClassesForModel(
      ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> models = ImmutableList.builder();
    Iterable<? extends InterfaceModel> interfaces = model.getInterfaces(productConfig);
    for (InterfaceModel apiInterface : interfaces) {
      if (!productConfig.hasInterfaceConfig(apiInterface)) {
        continue;
      }

      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      models.addAll(generateSampleClasses(context, model.hasMultipleServices()));
    }
    return models.build();
  }

  private List<ViewModel> generateSampleClasses(
      GapicInterfaceContext context, boolean hasMultipleServices) {
    ImmutableList.Builder<ViewModel> viewModels = new ImmutableList.Builder<>();
    SurfaceNamer namer = context.getNamer();

    List<OptionalArrayMethodView> allMethods =
        methodGenerator.generateApiMethods(context, hasMultipleServices);
    List<MethodSampleView> allSamples =
        allMethods
            .stream()
            .flatMap(m -> m.samples().stream())
            .collect(ImmutableList.toImmutableList());
    SampleFileRegistry registry = new SampleFileRegistry(namer, allSamples);
    DynamicLangSampleView.Builder sampleClassBuilder = DynamicLangSampleView.newBuilder();
    for (OptionalArrayMethodView method : allMethods) {
      String subPath =
          pathMapper.getSamplesOutputPath(
              context.getInterfaceModel().getFullName(),
              context.getProductConfig(),
              Name.lowerCamel(method.name()).toLowerUnderscore());
      for (MethodSampleView methodSample : method.samples()) {
        String sampleOutputPath =
            subPath
                + File.separator
                + registry.getSampleFileName(
                    methodSample, Name.anyLower((method.name())).toLowerUnderscore());
        viewModels.add(
            sampleClassBuilder
                .templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME)
                .fileHeader(fileHeaderTransformer.generateFileHeader(context))
                .outputPath(sampleOutputPath)
                .libraryMethod(method.toBuilder().samples(Arrays.asList(methodSample)).build())
                .gapicPackageName(namer.getGapicPackageName(packageConfig.packageName()))
                .build());
      }
    }
    return viewModels.build();
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new JSTypeTable(productConfig.getPackageName()),
            new NodeJSModelTypeNameConverter(productConfig.getPackageName())),
        new NodeJSSurfaceNamer(productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig)),
        new NodeJSFeatureConfig());
  }
}
