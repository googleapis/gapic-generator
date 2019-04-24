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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.VersionBound;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;

/** Responsible for producing package metadata for standalone samples in Node.js. */
public class NodeJSSamplePackageMetadataTransformer
    implements ModelToViewTransformer<ProtoApiModel> {

  private static final String TEMPLATE_FILE = "nodejs/sample_package.json.snip";
  private static final PackageMetadataTransformer metadataTransformer =
      new PackageMetadataTransformer();

  private final PackageMetadataConfig packageConfig;

  public NodeJSSamplePackageMetadataTransformer(PackageMetadataConfig packageConfig) {
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

    NodeJSPackageMetadataNamer gapicPackageNamer =
        new NodeJSPackageMetadataNamer(
            productConfig.getPackageName(), productConfig.getDomainLayerLocation());
    String outputPath = "samples/package.json";
    ViewModel metadataView =
        metadataTransformer
            .generateMetadataView(
                gapicPackageNamer,
                packageConfig,
                model,
                TEMPLATE_FILE,
                outputPath,
                TargetLanguage.NODEJS)
            .identifier(samplePackageIdentifier(productConfig))
            .hasMultipleServices(model.hasMultipleServices())
            .additionalDependencies(additionalDependencies(gapicPackageNamer))
            .build();
    return Collections.singletonList(metadataView);
  }

  private String samplePackageIdentifier(GapicProductConfig productConfig) {
    String packageName = productConfig.getPackageName();
    int firstDotIndex = packageName.indexOf(".");
    return "nodejs-docs-samples"
        + (firstDotIndex == -1 ? packageName : packageName.substring(0, firstDotIndex));
  }

  private List<PackageDependencyView> additionalDependencies(
      NodeJSPackageMetadataNamer gapicPackageNamer) {
    PackageDependencyView yargsDep =
        PackageDependencyView.create(
            "yargs", packageConfig.commonsCliVersionBound(TargetLanguage.NODEJS));
    PackageDependencyView gapicDep =
        PackageDependencyView.create(
            gapicPackageNamer.getMetadataIdentifier(), VersionBound.create("file:..", "file:.."));
    return ImmutableList.of(yargsDep, gapicDep);
  }
}
