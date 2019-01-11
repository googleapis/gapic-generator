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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.packagegen.java.JavaPackageTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;

/** Generate packaging related views for Java Gapic samples */
public class JavaGapicSamplesPackageTransformer extends JavaPackageTransformer
    implements ModelToViewTransformer<ProtoApiModel> {

  private final PackageMetadataConfig packageConfig;

  public JavaGapicSamplesPackageTransformer(PackageMetadataConfig packageConfig) {
    super(ImmutableMap.of("java/build_gapic_samples.gradle.snip", "samples/build.gradle"), null);
    this.packageConfig = packageConfig;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productionConfig) {
    List<ViewModel> viewModels = Lists.newArrayList();
    boolean shouldGenerateSamplePackages =
        productionConfig
            .getInterfaceConfigMap()
            .values()
            .stream()
            .flatMap(config -> config.getMethodConfigs().stream())
            .anyMatch(config -> config.getSampleSpec().isConfigured());
    if (!shouldGenerateSamplePackages) {
      return viewModels;
    }
    for (PackageMetadataView.Builder builder :
        this.generateMetadataViewBuilders(model, packageConfig, null)) {
      builder.additionalDependencies(additionalSampleDependencies());
      viewModels.add(builder.build());
    }
    return viewModels;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Lists.newArrayList(getSnippetsOutput().keySet());
  }

  private List<PackageDependencyView> additionalSampleDependencies() {
    return Collections.singletonList(
        PackageDependencyView.newBuilder()
            .group("com.google.api")
            .name("api-common")
            .versionBound(packageConfig.apiCommonVersionBound(TargetLanguage.JAVA))
            .build());
  }
}
