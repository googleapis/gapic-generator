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
import static com.google.common.base.MoreObjects.firstNonNull;

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
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangFileView;
import com.google.api.codegen.viewmodel.StaticLangSampleClassView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Streams;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * A base transformer to generate standalone samples for each method in the GAPIC surface generated
 * from the same ApiModel in Java and C#.
 */
public abstract class StaticLangGapicSamplesTransformer
    implements ModelToViewTransformer<ProtoApiModel> {

  private final String templateFileName;
  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer;
  private final StaticLangApiMethodTransformer apiMethodTransformer;
  private final Function<GapicProductConfig, FeatureConfig> newFeatureConfig;
  private final Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer;
  private final Function<String, ModelTypeTable> newTypeTable;

  protected StaticLangGapicSamplesTransformer(
      String templateFileName,
      GapicCodePathMapper pathMapper,
      FileHeaderTransformer fileHeaderTransformer,
      StaticLangApiMethodTransformer apiMethodTransformer,
      Function<GapicProductConfig, FeatureConfig> newFeatureConfig,
      Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer,
      Function<String, ModelTypeTable> newTypeTable) {
    this.templateFileName = templateFileName;
    this.pathMapper = pathMapper;
    this.fileHeaderTransformer = fileHeaderTransformer;
    this.apiMethodTransformer = apiMethodTransformer;
    this.newFeatureConfig = newFeatureConfig;
    this.newSurfaceNamer = newSurfaceNamer;
    this.newTypeTable = newTypeTable;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel apiModel, GapicProductConfig productConfig) {
    String packageName = productConfig.getPackageName();
    SurfaceNamer namer = newSurfaceNamer.apply(productConfig);
    ModelTypeTable typeTable = newTypeTable.apply(packageName);
    FeatureConfig featureConfig = newFeatureConfig.apply(productConfig);

    List<InterfaceContext> interfaceContexts =
        Streams.stream(apiModel.getInterfaces(productConfig))
            .filter(iface -> productConfig.hasInterfaceConfig(iface))
            .map(
                iface ->
                    GapicInterfaceContext.create(
                        iface, productConfig, typeTable, namer, featureConfig))
            .collect(ImmutableList.toImmutableList());

    ImmutableTable<String, String, ImmutableList<SampleConfig>> sampleConfigTable =
        productConfig.getSampleConfigTable();

    // We don't have sample configs. Continue to use gapic config.
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
            .flatMap(iface -> apiMethodTransformer.generateApiMethods(iface).stream())
            .flatMap(method -> method.samples().stream())
            .collect(ImmutableList.toImmutableList());
    SampleFileRegistry registry = new SampleFileRegistry(namer, allSamples);
    ImmutableList.Builder<ViewModel> sampleFileViews = ImmutableList.builder();
    for (InterfaceContext context : interfaceContexts) {
      List<StaticLangApiMethodView> methods = apiMethodTransformer.generateApiMethods(context);
      for (StaticLangApiMethodView method : methods) {
        for (MethodSampleView sample : method.samples()) {
          sampleFileViews.add(
              newSampleFileView(
                  productConfig,
                  context,
                  registry.getSampleClassName(sample, method.name()),
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

    // Loop through sample configs and and map each sample ID to its matching calling forms.
    // We need this information when we need to create, in a language-specific way, unique
    // sample ids when one sample id has multiple matching calling forms
    Map<String, List<CallingForm>> configsAndMatchingForms = new HashMap<>();
    for (InterfaceContext interfaceContext : interfaceContexts) {
      for (MethodModel method : interfaceContext.getSupportedMethods()) {
        MethodContext methodContext = interfaceContext.asRequestMethodContext(method);
        String interfaceName =
            interfaceContext.getInterfaceConfig().getInterfaceModel().getFullName();
        String methodName = method.getSimpleName();
        List<SampleConfig> sampleConfigs = sampleConfigTable.get(interfaceName, methodName);
        sampleConfigs = firstNonNull(sampleConfigs, ImmutableList.<SampleConfig>of());
        for (SampleConfig config : sampleConfigs) {
          List<CallingForm> allMatchingCallingForms =
              namer.getMatchingCallingForms(methodContext, config.callingPattern());
          List<CallingForm> existingForms = configsAndMatchingForms.get(config.id());
          if (existingForms == null) {
            existingForms = new ArrayList<>();
            configsAndMatchingForms.put(config.id(), existingForms);
          }
          existingForms.addAll(allMatchingCallingForms);
        }
      }
    }

    SampleFileRegistry registry = new SampleFileRegistry(namer, configsAndMatchingForms);

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
          List<CallingForm> allMatchingCallingForms =
              configsAndMatchingForms.get(sampleConfig.id());
          for (CallingForm form : allMatchingCallingForms) {
            InitCodeOutputType initCodeOutputType = InitCodeOutputType.SingleObject;

            // In Java and C#, we need to handle flattening.
            if (CallingForm.isFlattened(form)) {
              if (!methodContext.getMethodConfig().isFlattening()) {
                continue;
              }
              initCodeOutputType = InitCodeOutputType.FieldList;
              methodContext =
                  methodContext
                      .getSurfaceInterfaceContext()
                      .asFlattenedMethodContext(
                          methodContext,
                          methodContext.getMethodConfig().getFlatteningConfigs().get(0));
            }

            SampleContext sampleContext =
                SampleContext.newBuilder()
                    .uniqueSampleId(registry.getUniqueSampleId(sampleConfig, form))
                    .sampleType(SampleSpec.SampleType.STANDALONE)
                    .callingForm(form)
                    .clientMethodType(fromCallingForm(form))
                    .sampleConfig(sampleConfig)
                    .initCodeOutputType(initCodeOutputType)
                    .build();
            StaticLangApiMethodView methodView =
                apiMethodTransformer.generateApiMethod(methodContext, sampleContext);

            MethodSampleView methodSampleView = methodView.samples().get(0);
            String fileName = namer.getApiSampleFileName(sampleContext.uniqueSampleId());
            String className = namer.getApiSampleClassName(sampleContext.uniqueSampleId());
            sampleFileViews.add(
                newSampleFileView(
                    productConfig,
                    interfaceContext,
                    className,
                    fileName,
                    methodView,
                    methodSampleView));
          }
        }
      }
    }
    return sampleFileViews.build();
  }

  private StaticLangFileView newSampleFileView(
      GapicProductConfig productConfig,
      InterfaceContext context,
      String sampleClassName,
      String sampleFileName,
      StaticLangApiMethodView method,
      MethodSampleView sample) {
    StaticLangSampleClassView sampleClassView =
        StaticLangSampleClassView.newBuilder()
            .name(sampleClassName)
            .libraryMethod(method)
            .sample(sample)
            .build();

    String outputPath =
        Paths.get(
                pathMapper.getOutputPath(context.getInterfaceModel().getFullName(), productConfig),
                sampleFileName)
            .toString();

    StaticLangFileView.Builder<StaticLangSampleClassView> builder = StaticLangFileView.newBuilder();
    builder.templateFileName(templateFileName);
    builder.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    builder.outputPath(outputPath);
    builder.classView(sampleClassView);
    return builder.build();
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

  protected abstract ClientMethodType fromCallingForm(CallingForm callingForm);
}
