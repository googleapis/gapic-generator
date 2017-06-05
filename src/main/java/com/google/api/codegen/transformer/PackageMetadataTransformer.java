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
import com.google.api.codegen.config.VersionBound;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;
import java.util.*;

/** Constructs a partial ViewModel for producing package metadata related views */
public class PackageMetadataTransformer {

  /**
   * Construct a partial ViewModel, represented by its Builder, using all of the proto package
   * dependencies specified in the package config.
   */
  public PackageMetadataView.Builder generateMetadataView(
      PackageMetadataConfig packageConfig,
      Model model,
      String template,
      String outputPath,
      TargetLanguage language) {
    return generateMetadataView(packageConfig, model, template, outputPath, language, null);
  }

  /**
   * Construct a partial ViewModel, represented by its Builder, from the config. Proto package
   * dependencies are included only if whitelisted.
   */
  public PackageMetadataView.Builder generateMetadataView(
      PackageMetadataConfig packageConfig,
      Model model,
      String template,
      String outputPath,
      TargetLanguage language,
      Set<String> whitelistedDependencies) {
    // Note that internally, this is overridable in the service config, but the component is not
    // available externally. See:
    //   https://github.com/googleapis/toolkit/issues/933
    String discoveryApiName = model.getServiceConfig().getName();
    int dotIndex = discoveryApiName.indexOf(".");
    if (dotIndex > 0) {
      discoveryApiName = discoveryApiName.substring(0, dotIndex).replace("-", "_");
    }

    List<PackageDependencyView> protoPackageDependencies = new ArrayList<>();
    if (packageConfig.protoPackageDependencies(language) != null) {
      for (Map.Entry<String, VersionBound> entry :
          packageConfig.protoPackageDependencies(language).entrySet()) {
        if (entry.getValue() != null
            && (whitelistedDependencies == null
                || whitelistedDependencies.contains(entry.getKey()))) {
          protoPackageDependencies.add(
              PackageDependencyView.create(entry.getKey(), entry.getValue()));
        }
      }
      // Ensures deterministic test results.
      Collections.sort(protoPackageDependencies);
    }

    return PackageMetadataView.newBuilder()
        .templateFileName(template)
        .outputPath(outputPath)
        .packageVersionBound(packageConfig.generatedPackageVersionBound(language))
        .protoPath(packageConfig.protoPath())
        .shortName(packageConfig.shortName())
        .gapicConfigName(packageConfig.gapicConfigName())
        .packageType(packageConfig.packageType())
        .dependencyType(packageConfig.dependencyType())
        .gaxVersionBound(packageConfig.gaxVersionBound(language))
        .gaxGrpcVersionBound(packageConfig.gaxGrpcVersionBound(language))
        .grpcVersionBound(packageConfig.grpcVersionBound(language))
        .protoVersionBound(packageConfig.protoVersionBound(language))
        .protoPackageDependencies(
            getDependencies(
                packageConfig.protoPackageDependencies(language), whitelistedDependencies))
        .protoPackageTestDependencies(
            getDependencies(
                packageConfig.protoPackageTestDependencies(language), whitelistedDependencies))
        .authVersionBound(packageConfig.authVersionBound(language))
        .protoPackageName("proto-" + packageConfig.packageName(language))
        .gapicPackageName("gapic-" + packageConfig.packageName(language))
        .majorVersion(packageConfig.apiVersion())
        .author(packageConfig.author())
        .email(packageConfig.email())
        .homepage(packageConfig.homepage())
        .licenseName(packageConfig.licenseName())
        .fullName(model.getServiceConfig().getTitle())
        .discoveryApiName(discoveryApiName)
        .apiSummary(model.getServiceConfig().getDocumentation().getSummary())
        .hasMultipleServices(false);
  }

  private List<PackageDependencyView> getDependencies(
      Map<String, VersionBound> dependencies, Set<String> whitelistedDependencies) {
    List<PackageDependencyView> protoPackageDependencies = new ArrayList<>();
    if (dependencies != null) {
      Map<String, VersionBound> dependenciesCopy = new HashMap<>(dependencies);
      if (whitelistedDependencies != null) {
        dependenciesCopy.keySet().retainAll(whitelistedDependencies);
      }
      for (Map.Entry<String, VersionBound> entry : dependenciesCopy.entrySet())
        protoPackageDependencies.add(
            PackageDependencyView.create(entry.getKey(), entry.getValue()));
      // Ensures deterministic test results.
      Collections.sort(protoPackageDependencies);
    }
    return protoPackageDependencies;
  }
}
