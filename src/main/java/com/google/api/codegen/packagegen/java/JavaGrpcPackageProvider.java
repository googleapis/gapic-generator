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
package com.google.api.codegen.packagegen.java;

import com.google.api.codegen.GeneratedResult;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.packagegen.PackageProvider;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Map;

/** Performs grpc package generation for Java */
public class JavaGrpcPackageProvider implements PackageProvider<Doc> {

  private final JavaPackageTransformer transformer;

  public JavaGrpcPackageProvider(JavaPackageTransformer transformer) {
    this.transformer = transformer;
  }

  @Override
  public Map<String, GeneratedResult<Doc>> generate(Model model, PackageMetadataConfig config) {
    ImmutableMap.Builder<String, GeneratedResult<Doc>> results = new ImmutableMap.Builder<>();

    ProtoApiModel apiModel = new ProtoApiModel(model);
    ArrayList<PackageMetadataView> metadataViews = new ArrayList<>();
    metadataViews.addAll(transformer.transform(apiModel, config));

    for (PackageMetadataView view : metadataViews) {
      CommonSnippetSetRunner runner = new CommonSnippetSetRunner(view, false);
      results.putAll(runner.generate(view));
    }
    return results.build();
  }
}
