/* Copyright 2017 Google LLC
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
package com.google.api.codegen.grpcmetadatagen.java;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.grpcmetadatagen.ArtifactType;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.java.JavaPackageMetadataNamer;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Responsible for producing package meta-data related views for Java */
public class JavaPackageMetadataTransformer {
  // TODO determine if an API uses resource names from GAPIC config
  // https://github.com/googleapis/toolkit/issues/1668
  private ImmutableSet<String> SERVICES_WITH_NO_RESOURCE_NAMES =
      ImmutableSet.of("common-protos", "language", "speech", "video-intelligence", "vision");

  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();
  private final Map<String, String> snippetsOutput;
  private final ArtifactType artifactType;

  public JavaPackageMetadataTransformer(
      Map<String, String> snippetsOutput, ArtifactType artifactType) {
    this.snippetsOutput = ImmutableMap.copyOf(snippetsOutput);
    this.artifactType = artifactType;
  }

  protected Map<String, String> getSnippetsOutput() {
    return snippetsOutput;
  }

  public List<PackageMetadataView> transform(ApiModel model, PackageMetadataConfig config) {
    List<PackageMetadataView> views = new ArrayList<>();
    for (PackageMetadataView.Builder builder :
        generateMetadataViewBuilders(model, config, artifactType)) {
      views.add(builder.build());
    }
    return views;
  }

  /**
   * Creates a partially initialized builders that can be used to build PackageMetadataViews later.
   */
  protected final List<PackageMetadataView.Builder> generateMetadataViewBuilders(
      ApiModel model, PackageMetadataConfig config, ArtifactType artifactType) {
    JavaPackageMetadataNamer namer =
        new JavaPackageMetadataNamer(config.packageName(TargetLanguage.JAVA), artifactType);

    List<PackageDependencyView> additionalDependencies = new ArrayList<>();

    if (!SERVICES_WITH_NO_RESOURCE_NAMES.contains(config.shortName())) {
      PackageDependencyView packageDependency =
          PackageDependencyView.newBuilder()
              .group("com.google.api")
              .name("api-common")
              .versionBound(config.apiCommonVersionBound(TargetLanguage.JAVA))
              .build();
      additionalDependencies.add(packageDependency);
    }

    ArrayList<PackageMetadataView.Builder> viewBuilders = new ArrayList<>();
    for (Map.Entry<String, String> entry : getSnippetsOutput().entrySet()) {
      PackageMetadataView.Builder viewBuilder =
          metadataTransformer
              .generateMetadataView(
                  namer, config, model, entry.getKey(), entry.getValue(), TargetLanguage.JAVA)
              .additionalDependencies(additionalDependencies)
              .identifier(namer.getMetadataIdentifier())
              .protoPackageName(namer.getProtoPackageName())
              .grpcPackageName(namer.getGrpcPackageName())
              .publishProtos(artifactType == ArtifactType.PROTOBUF);
      viewBuilders.add(viewBuilder);
    }
    return viewBuilders;
  }
}
