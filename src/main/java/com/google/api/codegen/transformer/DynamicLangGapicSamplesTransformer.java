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
package com.google.api.codegen.transformer;

import static com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;

import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleConfig;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Streams;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

/**
 * A base transformer to generate standalone samples for each method in the GAPIC surface generated
 * from the same ApiModel in dynamic languages.
 */
public abstract class DynamicLangGapicSamplesTransformer
    implements ModelToViewTransformer<ProtoApiModel> {

  private static final SampleSpec.SampleType sampleType = SampleSpec.SampleType.STANDALONE;
  private final String templateFileName;
  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer;
  private final DynamicLangApiMethodTransformer apiMethodTransformer;
  private final FeatureConfig featureConfig;
  private final Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer;
  private final Function<String, ModelTypeTable> newTypeTable;

  protected DynamicLangGapicSamplesTransformer(
      String templateFileName,
      GapicCodePathMapper pathMapper,
      FileHeaderTransformer fileHeaderTransformer,
      DynamicLangApiMethodTransformer apiMethodTransformer,
      FeatureConfig featureConfig,
      Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer,
      Function<String, ModelTypeTable> newTypeTable) {
    this.templateFileName = templateFileName;
    this.pathMapper = pathMapper;
    this.fileHeaderTransformer = fileHeaderTransformer;
    this.apiMethodTransformer = apiMethodTransformer;
    this.featureConfig = featureConfig;
    this.newSurfaceNamer = newSurfaceNamer;
    this.newTypeTable = newTypeTable;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel apiModel, GapicProductConfig productConfig) {
    String packageName = productConfig.getPackageName();
    SurfaceNamer namer = newSurfaceNamer.apply(productConfig);
    ModelTypeTable typeTable = newTypeTable.apply(packageName);

    List<InterfaceContext> interfaceContexts =
        Streams.stream(apiModel.getInterfaces(productConfig))
            .filter(i -> productConfig.hasInterfaceConfig(i))
            .map(
                i ->
                    GapicInterfaceContext.create(i, productConfig, typeTable, namer, featureConfig))
            .collect(ImmutableList.toImmutableList());

    ImmutableTable<String, String, ImmutableList<SampleConfig>> sampleConfigTable =
        productConfig.getSampleConfigTable();

    // We don't have sample configs written in sample config. Continue to use gapic config.
    if (sampleConfigTable.isEmpty()) {
      return generateSamplesFromGapicConfigs(interfaceContexts, productConfig, namer);
    }

    // Generate samples using sample configs.
    return generateSamplesFromSampleConfigs(
        interfaceContexts, productConfig, namer, sampleConfigTable);
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(templateFileName);
  }

  private List<ViewModel> generateSamplesFromGapicConfigs(
      List<InterfaceContext> interfaceContexts,
      GapicProductConfig productConfig,
      SurfaceNamer namer) {
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
      SurfaceNamer namer,
      ImmutableTable<String, String, ImmutableList<SampleConfig>> sampleConfigTable) {

    List<SampleConfig> sampleConfigWithValidCallingPattern = new ArrayList<>();
    for (InterfaceContext interfaceContext : interfaceContexts) {
      for (MethodModel method : interfaceContext.getSupportedMethods()) {
        MethodContext methodContext = interfaceContext.asRequestMethodContext(method);
        String interfaceName =
            interfaceContext.getInterfaceConfig().getInterfaceModel().getFullName();
        String methodName = method.getSimpleName();
        MoreObjects.firstNonNull(
                sampleConfigTable.get(interfaceName, methodName), ImmutableList.<SampleConfig>of())
            .stream()
            .filter(c -> hasMatchingCallingForm(c.callingPattern(), namer, methodContext))
            .forEach(c -> sampleConfigWithValidCallingPattern.add(c));
      }
    }
    SampleFileRegistry registry =
        new SampleFileRegistry(namer, sampleConfigWithValidCallingPattern);

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
                Collections.singletonList(namer.getDefaultCallingForm(methodContext));
          } else {
            allMatchingCallingForms =
                namer
                    .getCallingForms(methodContext)
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
                    .uniqueSampleId(registry.getUniqueSampleId(sampleConfig, form))
                    .sampleType(SampleSpec.SampleType.STANDALONE)
                    .callingForm(form)
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

  private DynamicLangSampleView newSampleFileView(
      GapicProductConfig productConfig,
      InterfaceContext context,
      String sampleFileName,
      OptionalArrayMethodView method,
      MethodSampleView sample) {
    return newSampleFileViewBuilder(productConfig, context, sampleFileName, method, sample).build();
  }

  protected DynamicLangSampleView.Builder newSampleFileViewBuilder(
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
        .templateFileName(templateFileName)
        .fileHeader(fileHeaderTransformer.generateFileHeader(context))
        .outputPath(outputPath)
        .libraryMethod(method)
        .sample(sample);
  }

  private static boolean hasMatchingCallingForm(
      String userProvidedCallingPattern, SurfaceNamer namer, MethodContext context) {
    return userProvidedCallingPattern.equals("")
        || userProvidedCallingPattern.equals("default")
        || namer
            .getCallingForms(context)
            .stream()
            .anyMatch(t -> t.toLowerUnderscore().matches(userProvidedCallingPattern));
  }
}
