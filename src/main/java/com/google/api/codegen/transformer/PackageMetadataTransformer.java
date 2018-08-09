/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.VersionBound;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Constructs a partial ViewModel for producing package metadata related views */
public class PackageMetadataTransformer {

  /**
   * Construct a partial ViewModel, represented by its Builder, using all of the proto package
   * dependencies specified in the package config.
   */
  public PackageMetadataView.Builder generateMetadataView(
      PackageMetadataNamer namer,
      PackageMetadataConfig packageConfig,
      ApiModel model,
      String template,
      String outputPath,
      TargetLanguage language) {
    return generateMetadataView(namer, packageConfig, model, template, outputPath, language, null);
  }

  /**
   * Construct a partial ViewModel, represented by its Builder, from the config. Proto package
   * dependencies are included only if whitelisted.
   */
  public PackageMetadataView.Builder generateMetadataView(
      PackageMetadataNamer namer,
      PackageMetadataConfig packageConfig,
      ApiModel model,
      String template,
      String outputPath,
      TargetLanguage language,
      Set<String> whitelistedDependencies) {
    return PackageMetadataView.newBuilder()
        .templateFileName(template)
        .outputPath(outputPath)
        .packageVersionBound(packageConfig.generatedPackageVersionBound(language))
        .protoPath(packageConfig.protoPath())
        .shortName(packageConfig.shortName())
        .artifactType(packageConfig.artifactType())
        .gaxVersionBound(packageConfig.gaxVersionBound(language))
        .gaxGrpcVersionBound(packageConfig.gaxGrpcVersionBound(language))
        .gaxHttpVersionBound(packageConfig.gaxHttpVersionBound(language))
        .grpcVersionBound(packageConfig.grpcVersionBound(language))
        .protoVersionBound(packageConfig.protoVersionBound(language))
        .protoPackageDependencies(
            getDependencies(
                namer, packageConfig.protoPackageDependencies(language), whitelistedDependencies))
        .protoPackageTestDependencies(
            getDependencies(
                namer,
                packageConfig.protoPackageTestDependencies(language),
                whitelistedDependencies))
        .authVersionBound(packageConfig.authVersionBound(language))
        .protoPackageName("proto-" + packageConfig.packageName())
        .gapicPackageName("gapic-" + packageConfig.packageName())
        .majorVersion(packageConfig.apiVersion())
        .author(packageConfig.author())
        .email(packageConfig.email())
        .homepage(packageConfig.homepage())
        .licenseName(packageConfig.licenseName())
        .fullName(model.getTitle())
        .hasMultipleServices(false)
        .publishProtos(false);
  }

  /**
   * Merges release levels from a PackageMetadataConfig and a GapicProductConfig. The
   * GapicProductConfig always overrides the PackageMetadataConfig if its release level is set.
   */
  public ReleaseLevel getMergedReleaseLevel(
      PackageMetadataConfig packageConfig, GapicProductConfig productConfig) {
    return productConfig.getReleaseLevel() == ReleaseLevel.UNSET_RELEASE_LEVEL
        ? packageConfig.releaseLevel()
        : productConfig.getReleaseLevel();
  }

  private List<PackageDependencyView> getDependencies(
      PackageMetadataNamer namer,
      Map<String, VersionBound> dependencies,
      Set<String> whitelistedDependencies) {
    List<PackageDependencyView> protoPackageDependencies = new ArrayList<>();
    if (dependencies != null) {
      Map<String, VersionBound> dependenciesCopy = new HashMap<>(dependencies);
      if (whitelistedDependencies != null) {
        dependenciesCopy.keySet().retainAll(whitelistedDependencies);
      }
      for (Map.Entry<String, VersionBound> entry : dependenciesCopy.entrySet()) {
        PackageDependencyView packageDependency =
            PackageDependencyView.newBuilder()
                .group(namer.getProtoPackageGroup())
                .name(entry.getKey())
                .versionBound(entry.getValue())
                .build();
        protoPackageDependencies.add(packageDependency);
      }
      // Ensures deterministic test results.
      Collections.sort(protoPackageDependencies);
    }
    return protoPackageDependencies;
  }
}
