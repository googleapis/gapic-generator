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

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.codegen.viewmodel.ViewModel;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Responsible for producing package metadata related views for Ruby */
public class RubySamplePackageMetadataTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String TEMPLATE_FILE = "ruby/Gemfile_sample.snip";

  private final PackageMetadataConfig packageConfig;

  abstract static class RubySampleGemfileView implements ViewModel {

    @Override
    public String resourceRoot() {
      return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
    }

    @Override
    public String templateFileName() {
      return TEMPLATE_FILE;
    }

    @Override
    public String outputPath() {
      return "samples/Gemfile";
    }

    public abstract String gapicGemName();
  }

  public RubySamplePackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    boolean shouldGenerateSamplePackages =
        productConfig
            .getInterfaceConfigMap()
            .values()
            .stream()
            .flatMap(config -> config.getMethodConfigs().stream())
            .anyMatch(config -> config.getSampleSpec().isConfigured());
    if (!shouldGenerateSamplePackages) {
      return Collections.emptyList();
    }

    ViewModel rubySampleGemfileView =
        new RubySampleGemfileView() {
          @Override
          public String gapicGemName() {
            return RubySamplePackageMetadataTransformer.gapicGemName(packageConfig.packageName());
          }
        };

    return Collections.singletonList(rubySampleGemfileView);
  }

  private static String gapicGemName(String packageName) {
    return Stream.of(packageName.split("-"))
        .filter(p -> !VersionMatcher.isVersion(p))
        .collect(Collectors.joining("-"));
  }
}
