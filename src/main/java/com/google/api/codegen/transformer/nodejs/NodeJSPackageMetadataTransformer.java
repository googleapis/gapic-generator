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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.Iterables;

/** Responsible for producing package metadata related views for NodeJS */
public class NodeJSPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String PACKAGE_FILE = "nodejs/package.snip";

  PackageMetadataConfig packageConfig;

  public NodeJSPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(PACKAGE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    boolean hasMultipleServices = Iterables.size(services) > 1;
    List<ViewModel> models = new ArrayList<ViewModel>();
    NodeJSPackageMetadataNamer namer =
        new NodeJSPackageMetadataNamer(
            apiConfig.getPackageName(), apiConfig.getDomainLayerLocation());
    models.add(generateMetadataView(model, namer, hasMultipleServices));
    return models;
  }

  private ViewModel generateMetadataView(Model model, NodeJSPackageMetadataNamer namer,
      boolean hasMultipleServices) {
    return PackageMetadataView.newBuilder()
        .templateFileName(PACKAGE_FILE)
        .outputPath("package.json")
        .identifier(namer.getMetadataIdentifier())
        .packageVersionBound(packageConfig.packageVersionBound(TargetLanguage.NODEJS))
        .protoPath(packageConfig.protoPath())
        .shortName(packageConfig.shortName())
        .gaxVersionBound(packageConfig.gaxVersionBound(TargetLanguage.NODEJS))
        .protoVersionBound(packageConfig.protoVersionBound(TargetLanguage.NODEJS))
        .commonProtosVersionBound(packageConfig.commonProtosVersionBound(TargetLanguage.NODEJS))
        .packageName(packageConfig.packageName(TargetLanguage.NODEJS))
        .majorVersion(packageConfig.apiVersion())
        .author(packageConfig.author())
        .email(packageConfig.email())
        .homepage(packageConfig.homepage())
        .licenseName(packageConfig.licenseName())
        .fullName(model.getServiceConfig().getTitle())
        .serviceName(namer.getMetadataName())
        .hasMultipleServices(hasMultipleServices)
        .build();
  }
}
