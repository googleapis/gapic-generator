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
package com.google.api.codegen.grpcmetadatagen.java;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Responsible for producing GRPC package meta-data related views for Java */
public class JavaGrpcPackageMetadataTransformer {
  private static final Map<String, String> SNIPPETS_OUTPUT =
      ImmutableMap.of(
          "LICENSE.snip", "LICENSE",
          "metadatagen/java/grpc/grpc_package.snip", "build.gradle");

  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  public List<PackageMetadataView> transform(Model model, PackageMetadataConfig config) {
    return generateMetadataView(model, config);
  }

  private List<PackageMetadataView> generateMetadataView(
      Model model, PackageMetadataConfig config) {
    ArrayList<PackageMetadataView> views = new ArrayList<>();
    for (String template : SNIPPETS_OUTPUT.keySet()) {
      PackageMetadataView view =
          metadataTransformer
              .generateMetadataView(
                  config, model, template, SNIPPETS_OUTPUT.get(template), TargetLanguage.JAVA)
              .identifier(config.packageName(TargetLanguage.JAVA))
              .apiCommonVersionBound(config.apiCommonVersionBound(TargetLanguage.JAVA))
              .build();
      views.add(view);
    }
    return views;
  }
}
