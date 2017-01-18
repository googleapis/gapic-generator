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
package com.google.api.codegen.transformer;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;

/** Constructs a partial ViewModel for producing package metadata related views */
public class PackageMetadataTransformer {
  public PackageMetadataView.Builder generateMetadataView(
      PackageMetadataConfig packageConfig,
      Model model,
      String template,
      String outputPath,
      TargetLanguage language) {

    return PackageMetadataView.newBuilder()
        .templateFileName(template)
        .outputPath(outputPath)
        .packageVersionBound(packageConfig.generatedPackageVersionBound(language))
        .protoPath(packageConfig.protoPath())
        .shortName(packageConfig.shortName())
        .gaxVersionBound(packageConfig.gaxVersionBound(language))
        .grpcVersionBound(packageConfig.grpcVersionBound(language))
        .protoVersionBound(packageConfig.protoVersionBound(language))
        .commonProtosVersionBound(packageConfig.commonProtosVersionBound(language))
        .authVersionBound(packageConfig.authVersionBound(language))
        .grpcPackageName("grpc-" + packageConfig.packageName(language))
        .gapicPackageName("gapic-" + packageConfig.packageName(language))
        .majorVersion(packageConfig.apiVersion())
        .author(packageConfig.author())
        .email(packageConfig.email())
        .homepage(packageConfig.homepage())
        .licenseName(packageConfig.licenseName())
        .fullName(model.getServiceConfig().getTitle())
        .hasMultipleServices(false);
  }
}
