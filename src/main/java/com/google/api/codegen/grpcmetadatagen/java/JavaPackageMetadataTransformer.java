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

import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.java.JavaPackageMetadataNamer;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Responsible for producing package meta-data related views for Java */
public abstract class JavaPackageMetadataTransformer {

  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  protected abstract Map<String, String> getSnippetsOutput();

  public List<PackageMetadataView> transform(Model model, PackageMetadataConfig config) {
    return generateMetadataView(model, config);
  }

  private List<PackageMetadataView> generateMetadataView(
      Model model, PackageMetadataConfig config) {
    JavaPackageMetadataNamer namer =
        new JavaPackageMetadataNamer(
            config.packageName(TargetLanguage.JAVA), config.generationLayer());
    ArrayList<PackageMetadataView> views = new ArrayList<>();
    for (String template : getSnippetsOutput().keySet()) {
      PackageMetadataView view =
          metadataTransformer
              .generateMetadataView(
                  config, model, template, getSnippetsOutput().get(template), TargetLanguage.JAVA)
              .identifier(namer.getMetadataIdentifier())
              .protoPackageName(namer.getProtoPackageName())
              .generationLayer(config.generationLayer())
              .apiCommonVersionBound(config.apiCommonVersionBound(TargetLanguage.JAVA))
              .build();
      views.add(view);
    }
    return views;
  }
}
