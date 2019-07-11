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
package com.google.api.codegen.transformer.ruby;

import static com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleConfig;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleFileRegistry;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Streams;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * A transformer to generate Ruby standalone samples for each method in the GAPIC surface generated
 * from the same ApiModel.
 */
public class RubyGapicSamplesTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "ruby/standalone_sample.snip";
  private static final SampleSpec.SampleType sampleType = SampleSpec.SampleType.STANDALONE;

  private final RubyImportSectionTransformer importSectionTransformer =
      new RubyImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new RubyApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer, false))
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
    ImmutableTable<String, String, ImmutableList<SampleConfig>> sampleConfigTable =
        productConfig.getSampleConfigTable();

    List<InterfaceContext> interfaceContexts =
        Streams.stream(apiModel.getInterfaces(productConfig))
            .filter(i -> productConfig.hasInterfaceConfig(i))
            .map(
                i ->
                    GapicInterfaceContext.create(
                        i, productConfig, typeTable, namer, new RubyFeatureConfig()))
            .collect(ImmutableList.toImmutableList());

    // We don't have sample configs written in sample config. Continue to use gapic config.
    if (sampleConfigTable.isEmpty()) {
      return generateSamplesFromGapicConfigs(interfaceContexts, productConfig, namer);
    }

    // Generate samples using sample configs.
    return generateSamplesFromSampleConfigs(
        interfaceContexts, productConfig, namer, sampleConfigTable);
  }

  private DynamicLangSampleView newSampleFileView(
      GapicProductConfig productConfig,
      InterfaceContext context,
      String sampleFileName,
      OptionalArrayMethodView method,
      MethodSampleView sample) {
    String outputPath =
        Paths.get(
                pathMapper.getSamplesOutputPath(
                    context.getInterfaceModel().getFullName(), productConfig, method.name()),
                sampleFileName)
            .toString();
    return DynamicLangSampleView.newBuilder()
        .templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME)
        .fileHeader(fileHeaderTransformer.generateFileHeader(context))
        .outputPath(outputPath)
        .libraryMethod(method)
        .sample(sample)
        .build();
  }

  private List<ViewModel> generateSamplesFromGapicConfigs(
      List<InterfaceContext> interfaceContexts,
      GapicProductConfig productConfig,
      RubySurfaceNamer namer) {
    List<MethodSampleView> allSamples =
        interfaceContexts
            .stream()
            .flatMap(c -> apiMethodTransformer.generateApiMethods(c).stream())
            .flatMap(m -> m.samples().stream())
            .collect(ImmutableList.toImmutableList());
    SampleFileRegistry registry = new SampleFileRegistry(namer, allSamples);
    ImmutableList.Builder<ViewModel> sampleFileViews = ImmutableList.builder();
    for (InterfaceContext context : interfaceContexts) {
      List<OptionalArrayMethodView> methods = apiMethodTransformer.generateApiMethods(context);
      for (OptionalArrayMethodView method : methods) {
        for (MethodSampleView sample : method.samples()) {
          sampleFileViews.add(
              newSampleFileView(
                  productConfig,
                  context,
                  registry.getSampleFileName(sample, method.name()),
                  method,
                  sample));
        }
      }
    }
    return sampleFileViews.build();
  }

  private List<ViewModel> generateSamplesFromSampleConfigs(
      List<InterfaceContext> interfaceContexts,
      GapicProductConfig productConfig,
      RubySurfaceNamer namer,
      ImmutableTable<String, String, ImmutableList<SampleConfig>> sampleConfigTable) {
    SampleFileRegistry registry =
        new SampleFileRegistry(
            namer,
            sampleConfigTable
                .values()
                .stream()
                .flatMap(List::stream)
                .collect(ImmutableList.toImmutableList()));
    ImmutableList.Builder<ViewModel> sampleFileViews = ImmutableList.builder();
    for (InterfaceContext interfaceContext : interfaceContexts) {
      for (MethodModel method : interfaceContext.getSupportedMethods()) {
        MethodContext methodContext = interfaceContext.asRequestMethodContext(method);
        String interfaceName =
            interfaceContext.getInterfaceConfig().getInterfaceModel().getFullName();
        String methodName = method.getSimpleName();
        ImmutableList<SampleConfig> sampleConfigs =
            sampleConfigTable.get(interfaceName, methodName);
        if (sampleConfigs == null) {
          continue;
        }
        for (SampleConfig sampleConfig : sampleConfigs) {
          List<CallingForm> allMatchingCallingForms;
          if (sampleConfig.usesDefaultCallingForm()) {
            allMatchingCallingForms =
                Collections.singletonList(
                    CallingForm.getDefaultCallingForm(methodContext, TargetLanguage.RUBY));
          } else {
            allMatchingCallingForms =
                CallingForm.getCallingForms(methodContext, TargetLanguage.RUBY)
                    .stream()
                    .filter(t -> t.toLowerUnderscore().matches(sampleConfig.callingPattern()))
                    .collect(ImmutableList.toImmutableList());
          }

          for (CallingForm form : allMatchingCallingForms) {
            InitCodeOutputType initCodeOutputType =
                methodContext.getMethodModel().getRequestStreaming()
                    ? InitCodeOutputType.SingleObject
                    : InitCodeOutputType.FieldList;
            SampleContext sampleContext =
                SampleContext.newBuilder()
                    .uniqueSampleId(registry.getUniqueSampleId(sampleConfig.id(), form))
                    .sampleType(SampleSpec.SampleType.STANDALONE)
                    .callingForm(
                        CallingForm.getDefaultCallingForm(methodContext, TargetLanguage.RUBY))
                    .sampleConfig(sampleConfig)
                    .initCodeOutputType(initCodeOutputType)
                    .build();
            OptionalArrayMethodView methodView =
                apiMethodTransformer.generateApiMethod(methodContext, sampleContext);

            MethodSampleView methodSampleView = methodView.samples().get(0);
            String fileName = namer.getApiSampleFileName(sampleContext.uniqueSampleId());
            sampleFileViews.add(
                newSampleFileView(
                    productConfig, interfaceContext, fileName, methodView, methodSampleView));
          }
        }
      }
    }
    return sampleFileViews.build();
  }
}
