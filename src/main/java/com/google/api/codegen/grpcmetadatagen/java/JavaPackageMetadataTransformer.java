/* Copyright 2017 Google LLC
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
package com.google.api.codegen.grpcmetadatagen.java;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.java.JavaPackageMetadataNamer;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Responsible for producing package meta-data related views for Java */
public abstract class JavaPackageMetadataTransformer {

  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  protected abstract Map<String, String> getSnippetsOutput();

  public List<PackageMetadataView> transform(ApiModel model, PackageMetadataConfig config) {
    List<PackageMetadataView> views = new ArrayList<>();
    for (PackageMetadataView.Builder builder : generateMetadataViewBuilders(model, config)) {
      views.add(builder.build());
    }
    return views;
  }

  /**
   * Creates a partially initialized builders that can be used to build PackageMetadataViews later.
   */
  protected final List<PackageMetadataView.Builder> generateMetadataViewBuilders(
      ApiModel model, PackageMetadataConfig config) {
    JavaPackageMetadataNamer namer =
        new JavaPackageMetadataNamer(
            config.packageName(TargetLanguage.JAVA), config.generationLayer());

    ArrayList<PackageMetadataView.Builder> viewBuilders = new ArrayList<>();
    for (Map.Entry<String, String> entry : getSnippetsOutput().entrySet()) {
      PackageMetadataView.Builder viewBuilder =
          metadataTransformer
              .generateMetadataView(
                  config, model, entry.getKey(), entry.getValue(), TargetLanguage.JAVA)
              .identifier(namer.getMetadataIdentifier())
              .protoPackageName(namer.getProtoPackageName())
              .grpcPackageName(namer.getGrpcPackageName())
              .generationLayer(config.generationLayer())
              .apiCommonVersionBound(config.apiCommonVersionBound(TargetLanguage.JAVA));
      viewBuilders.add(viewBuilder);
    }
    return viewBuilders;
  }
}
