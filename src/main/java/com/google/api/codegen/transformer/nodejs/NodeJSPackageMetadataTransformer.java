/* Copyright 2016 Google Inc
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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.PackageMetadataView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Responsible for producing package metadata related views for NodeJS */
public class NodeJSPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String PACKAGE_FILE = "nodejs/package.snip";

  PackageMetadataConfig packageConfig;

  public NodeJSPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(PACKAGE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    NodeJSPackageMetadataNamer namer =
        new NodeJSPackageMetadataNamer(
            apiConfig.getPackageName(), apiConfig.getDomainLayerLocation());
    models.add(generateMetadataView(model, apiConfig, namer));
    return models;
  }

  private ViewModel generateMetadataView(
      Model model, ApiConfig apiConfig, NodeJSPackageMetadataNamer namer) {
    return PackageMetadataView.newBuilder()
        .templateFileName(PACKAGE_FILE)
        .outputPath("package.json")
        .identifier(namer.getMetadataIdentifier())
        .packageVersion(packageConfig.packageVersion("nodejs"))
        .protoPath(packageConfig.protoPath())
        .shortName(packageConfig.shortName())
        .gaxVersion(packageConfig.gaxVersion("nodejs"))
        .protoVersion(packageConfig.protoVersion("nodejs"))
        .commonProtosVersion(packageConfig.commonProtosVersion("nodejs"))
        .packageName(packageConfig.packageName("nodejs"))
        .majorVersion(packageConfig.apiVersion())
        .author(packageConfig.author())
        .email(packageConfig.email())
        .homepage(packageConfig.homepage())
        .license(packageConfig.license())
        .fullName(model.getServiceConfig().getTitle())
        .serviceName(namer.getMetadataName())
        .build();
  }
}
