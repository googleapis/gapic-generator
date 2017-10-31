/* Copyright 2016 Google LLC
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
package com.google.api.codegen.grpcmetadatagen.py;

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.py.PythonSurfaceNamer;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/** Python specific transformer to generate meta-data views for gRPC packages. */
public class PythonGrpcMetadataTransformer {
  private static final List<String> SNIPPETS =
      Lists.newArrayList(
          "LICENSE.snip",
          "metadatagen/py/setup.py.snip",
          "metadatagen/py/README.rst.snip",
          "metadatagen/py/PUBLISHING.rst.snip",
          "metadatagen/py/MANIFEST.in.snip");

  private static final ImmutableSet<String> PROTO_PACKAGE_DEPENDENCY_WHITELIST =
      ImmutableSet.of("googleapis-common-protos");

  private PythonPackageCopierResult copierResult;

  public PythonGrpcMetadataTransformer(PythonPackageCopierResult copierResult) {
    this.copierResult = copierResult;
  }

  public List<PackageMetadataView> transform(ApiModel model, PackageMetadataConfig config) {
    ArrayList<PackageMetadataView> views = new ArrayList<>();
    PythonSurfaceNamer surfaceNamer =
        new PythonSurfaceNamer(config.packageName(TargetLanguage.PYTHON));

    PackageMetadataTransformer transformer = new PackageMetadataTransformer();
    for (String snippetFilename : SNIPPETS) {
      PackageMetadataView view =
          transformer
              .generateMetadataView(
                  config,
                  model,
                  snippetFilename,
                  outputPath(snippetFilename),
                  TargetLanguage.PYTHON,
                  PROTO_PACKAGE_DEPENDENCY_WHITELIST)
              .namespacePackages(copierResult.namespacePackages())
              .developmentStatus(
                  surfaceNamer.getReleaseAnnotation(config.releaseLevel(TargetLanguage.PYTHON)))
              .build();
      views.add(view);
    }
    return views;
  }

  private static String outputPath(String templateFileName) {
    String baseName = Paths.get(templateFileName).getFileName().toString();
    int extensionIndex = baseName.lastIndexOf(".");
    return baseName.substring(0, extensionIndex);
  }
}
