/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Responsible for producing package metadata related views for PHP */
public class PhpPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String PACKAGE_FILE = "php/composer.snip";

  private PackageMetadataConfig packageConfig;
  private PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  public PhpPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(PACKAGE_FILE);
  }

  @Override
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> models = new ArrayList<>();
    PhpPackageMetadataNamer namer =
        new PhpPackageMetadataNamer(
            productConfig.getPackageName(), productConfig.getDomainLayerLocation());
    models.add(generateMetadataView(model, namer));
    return models;
  }

  private ViewModel generateMetadataView(ApiModel model, PackageMetadataNamer namer) {
    List<PackageDependencyView> dependencies =
        ImmutableList.of(
            PackageDependencyView.create(
                "google/gax", packageConfig.gaxVersionBound(TargetLanguage.PHP)),
            PackageDependencyView.create(
                "google/protobuf", packageConfig.protoVersionBound(TargetLanguage.PHP)));
    return metadataTransformer
        .generateMetadataView(
            namer, packageConfig, model, PACKAGE_FILE, "composer.json", TargetLanguage.PHP)
        .additionalDependencies(dependencies)
        .hasMultipleServices(model.hasMultipleServices())
        .identifier(namer.getMetadataIdentifier())
        .build();
  }
}
