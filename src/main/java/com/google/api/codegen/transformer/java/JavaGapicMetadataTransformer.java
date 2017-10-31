/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.gapic.GapicGeneratorConfig;
import com.google.api.codegen.grpcmetadatagen.java.JavaPackageMetadataTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;

/** Responsible for producing package meta-data related views for Java GAPIC clients */
public class JavaGapicMetadataTransformer extends JavaPackageMetadataTransformer
    implements ModelToViewTransformer {
  private static final String GAPIC_BUILD_TEMPLATE_FILENAME = "java/build_gapic.gradle.snip";
  private static final String SAMPLE_BUILD_TEMPLATE_FILENAME = "java/sample.gradle.snip";

  private final PackageMetadataConfig packageConfig;
  private final GapicGeneratorConfig generatorConfig;
  private final GapicProductConfig productConfig;
  private final GapicCodePathMapper pathMapper;
  private final Map<String, String> snippetsOutput;

  public JavaGapicMetadataTransformer(
      GapicCodePathMapper pathMapper,
      GapicProductConfig productConfig,
      PackageMetadataConfig packageConfig,
      GapicGeneratorConfig generatorConfig) {
    this.packageConfig = packageConfig;
    this.generatorConfig = generatorConfig;
    this.productConfig = productConfig;
    this.pathMapper = pathMapper;

    if (generatorConfig.enableSampleAppGenerator()) {
      // Includes sample build file if the sample application generation is enabled
      this.snippetsOutput = ImmutableMap.of(SAMPLE_BUILD_TEMPLATE_FILENAME, "build.gradle");
    } else {
      this.snippetsOutput = ImmutableMap.of(GAPIC_BUILD_TEMPLATE_FILENAME, "build.gradle");
    }
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    String packageName = packageConfig.packageName(TargetLanguage.JAVA);
    JavaSurfaceNamer namer = new JavaSurfaceNamer(packageName, packageName);
    ProtoApiModel apiModel = new ProtoApiModel(model);

    List<ViewModel> viewModels = Lists.newArrayList();
    for (PackageMetadataView.Builder builder :
        this.generateMetadataViewBuilders(apiModel, packageConfig)) {
      if (generatorConfig.enableSampleAppGenerator()) {
        builder
            .sampleAppName(namer.getSampleAppClassName())
            .sampleAppPackage(getSamplePackageName(model));
      }
      viewModels.add(builder.build());
    }
    return viewModels;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Lists.newArrayList(snippetsOutput.keySet());
  }

  @Override
  protected Map<String, String> getSnippetsOutput() {
    return snippetsOutput;
  }

  private String getSamplePackageName(Model model) {
    JavaGapicSampleAppTransformer sampleAppTransformer =
        new JavaGapicSampleAppTransformer(pathMapper);
    GapicInterfaceContext context = sampleAppTransformer.getSampleContext(model, productConfig);
    return context.getNamer().getPackageName();
  }
}
