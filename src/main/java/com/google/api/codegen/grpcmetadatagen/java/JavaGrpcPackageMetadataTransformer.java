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
import java.util.ArrayList;
import java.util.List;

/** Responsible for producing GRPC package meta-data related views for Java */
public class JavaGrpcPackageMetadataTransformer {
  private static final String GRPC_PACKAGE_TEMPLATE = "java/grpc_package.snip";
  private static final String GRPC_PACKAGE_FILE = "build.gradle";

  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  public List<PackageMetadataView> transform(Model model, PackageMetadataConfig config) {
    List<PackageMetadataView> views = new ArrayList<>();
    views.add(generateMetadataView(model, config));
    return views;
  }

  private PackageMetadataView generateMetadataView(Model model, PackageMetadataConfig config) {
    return metadataTransformer
        .generateMetadataView(
            config, model, GRPC_PACKAGE_TEMPLATE, GRPC_PACKAGE_FILE, TargetLanguage.JAVA)
        .identifier(config.packageName(TargetLanguage.JAVA))
        .build();
  }
}
