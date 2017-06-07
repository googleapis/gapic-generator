/* Copyright 2017 Google Inc
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

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.grpcmetadatagen.java.JavaPackageMetadataTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Responsible for producing package meta-data related views for Java GAPIC clients */
public class JavaGapicMetadataTransformer extends JavaPackageMetadataTransformer
    implements ModelToViewTransformer {
  private static final String BUILD_TEMPLATE_FILENAME = "java/build_gapic.gradle.snip";
  private final Map<String, String> snippetsOutput =
      ImmutableMap.of("java/build_gapic.gradle.snip", "build.gradle");

  private final PackageMetadataConfig packageConfig;

  public JavaGapicMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> viewModels = Lists.newArrayList();
    viewModels.addAll(this.transform(model, packageConfig));
    return viewModels;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(BUILD_TEMPLATE_FILENAME);
  }

  @Override
  protected Map<String, String> getSnippetsOutput() {
    return snippetsOutput;
  }
}
