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
package com.google.api.codegen.transformer.php;

/**
 * A transformer to generate PHP standalone samples for each method in the GAPIC surface generated
 * from the same ApiModel.
 */
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Collections;
import java.util.List;

public class PhpGapicSamplesTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "php/standalone_sample.snip";

  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new PhpApiMethodParamTransformer(), new InitCodeTransformer(), SampleType.STANDALONE);
  private final GapicCodePathMapper pathMapper;
  private final PhpMethodViewGenerator methodGenerator =
      new PhpMethodViewGenerator(apiMethodTransformer);
  private final PackageMetadataConfig packageConfig;

  public PhpGapicSamplesTransformer(
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
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      models.addAll(generateSamples(context));
    }
    return models.build();
  }

  private List<ViewModel> generateSamples(GapicInterfaceContext context) {
    ImmutableList.Builder<ViewModel> viewModels = new ImmutableList.Builder<>();
    SurfaceNamer namer = context.getNamer();

    List<OptionalArrayMethodView> allmethods = methodGenerator.generateApiMethods(context);
    DynamicLangSampleView.Builder sampleClassBuilder = DynamicLangSampleView.newBuilder();
    for (OptionalArrayMethodView method : allmethods) {
      String subPath =
          pathMapper.getSamplesOutputPath(
              context.getInterfaceModel().getFullName(), context.getProductConfig(), method.name());
      for (MethodSampleView methodSample : method.samples()) {
        String className =
            namer.getApiSampleClassName(
                method.name(), methodSample.callingForm().toString(), methodSample.valueSet().id());
        String sampleOutputPath = subPath + File.separator + namer.getApiSampleFileName(className);
        viewModels.add(
            sampleClassBuilder
                .templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME)
                .outputPath(sampleOutputPath)
                .className(className)
                .libraryMethod(
                    method.toBuilder().samples(Collections.singletonList(methodSample)).build())
                .gapicPackageName(namer.getGapicPackageName(packageConfig.packageName()))
                .extraInfo(
                    PhpSampleExtraInfo.newBuilder()
                        .hasDefaultServiceScopes(
                            context.getInterfaceConfig().hasDefaultServiceScopes())
                        .hasDefaultServiceAddress(
                            context.getInterfaceConfig().hasDefaultServiceAddress())
                        .build())
                .build());
      }
    }
    return viewModels.build();
  }

  @AutoValue
  public abstract static class PhpSampleExtraInfo extends DynamicLangSampleView.SampleExtraInfo {

    // private PhpSampleExtraInfo(Builder builder) {
    //   hasDefaultServiceAddress = builder.hasDefaultServiceAddress;
    //   hasDefaultServiceScopes = builder.hasDefaultServiceScopes;
    // }

    abstract boolean hasDefaultServiceAddress();

    abstract boolean hasDefaultServiceScopes();

    public boolean missingDefaultServiceScopes() {
      return !hasDefaultServiceScopes();
    }

    public boolean missingDefaultServiceAddress() {
      return !hasDefaultServiceAddress();
    }

    public boolean hasMissingDefaultOptions() {
      return missingDefaultServiceAddress() || missingDefaultServiceScopes();
    }

    public static Builder newBuilder() {
      return new AutoValue_PhpGapicSamplesTransformer_PhpSampleExtraInfo.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      abstract Builder hasDefaultServiceAddress(boolean val);

      abstract Builder hasDefaultServiceScopes(boolean val);

      abstract PhpSampleExtraInfo build();
    }
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new PhpTypeTable(productConfig.getPackageName()),
            new PhpModelTypeNameConverter(productConfig.getPackageName())),
        new PhpSurfaceNamer(productConfig.getPackageName()),
        new PhpFeatureConfig());
  }
}
