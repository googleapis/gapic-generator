/* Copyright 2017 Google Inc
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

import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.grpcmetadatagen.GrpcMetadataProvider;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

/** Performs gRPC meta-data generation for Java */
public class JavaGrpcMetadataProvider implements GrpcMetadataProvider {

  private final JavaPackageMetadataTransformer transformer;
  private final JavaPackageCopier copier;

  private static final ImmutableList<String> STATIC_FILES =
      ImmutableList.of(
          "gradlew",
          "gradle/wrapper/gradle-wrapper.jar",
          "gradle/wrapper/gradle-wrapper.properties",
          "gradlew.bat",
          "PUBLISHING.md",
          "templates/apidocs_index.html.template");

  public JavaGrpcMetadataProvider(JavaPackageMetadataTransformer transformer, ToolOptions options) {
    this.transformer = transformer;
    this.copier = new JavaPackageCopier(STATIC_FILES, options);
  }

  @Override
  public Map<String, Doc> generate(Model model, PackageMetadataConfig config) throws IOException {
    ImmutableMap.Builder<String, Doc> docs = new ImmutableMap.Builder<String, Doc>();
    copier.run();
    ProtoApiModel apiModel = new ProtoApiModel(model);
    ArrayList<PackageMetadataView> metadataViews = new ArrayList<>();
    metadataViews.addAll(transformer.transform(apiModel, config));

    for (PackageMetadataView view : metadataViews) {
      CommonSnippetSetRunner runner = new CommonSnippetSetRunner(view);
      Doc result = runner.generate(view);
      if (!result.isWhitespace()) {
        docs.put(view.outputPath(), result);
      }
    }
    return docs.build();
  }
}
