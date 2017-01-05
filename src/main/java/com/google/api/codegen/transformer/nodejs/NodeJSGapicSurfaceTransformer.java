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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.IndexRequireView;
import com.google.api.codegen.viewmodel.metadata.IndexView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Responsible for producing GAPIC surface views for NodeJS
 *
 * <p>NOTE: Currently NodeJS is not MVVM yet. This transformer only produces views for index.js.
 */
public class NodeJSGapicSurfaceTransformer implements ModelToViewTransformer {
  private static final String INDEX_TEMPLATE_FILE = "nodejs/index.snip";
  private static final String VERSION_INDEX_TEMPLATE_FILE = "nodejs/version_index.snip";

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(INDEX_TEMPLATE_FILE, VERSION_INDEX_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    List<ViewModel> models = new ArrayList<ViewModel>();
    NodeJSSurfaceNamer surfaceNamer = new NodeJSSurfaceNamer(apiConfig.getPackageName());
    models.addAll(generateIndexViews(services, surfaceNamer));

    return models;
  }

  private List<ViewModel> generateIndexViews(
      Iterable<Interface> services, NodeJSSurfaceNamer namer) {
    ArrayList<ViewModel> indexViews = new ArrayList<>();

    ArrayList<IndexRequireView> requireViews = new ArrayList<>();
    for (Interface service : services) {
      requireViews.add(
          IndexRequireView.newBuilder()
              .clientName(namer.getApiWrapperClassName(service))
              .fileName(namer.getClientFileName(service))
              .build());
    }
    String version = namer.getApiWrapperModuleVersion();
    boolean hasVersion = version != null && !version.isEmpty();
    String indexOutputPath = hasVersion ? "src/" + version + "/index.js" : "src/index.js";
    IndexView.Builder indexViewbuilder =
        IndexView.newBuilder()
            .templateFileName(INDEX_TEMPLATE_FILE)
            .outputPath(indexOutputPath)
            .requireViews(requireViews)
            .primaryService(requireViews.get(0));
    if (hasVersion) {
      indexViewbuilder.apiVersion(version);
    }
    indexViews.add(indexViewbuilder.build());

    if (hasVersion) {
      String versionIndexOutputPath = "src/index.js";
      IndexView.Builder versionIndexViewBuilder =
          IndexView.newBuilder()
              .templateFileName(VERSION_INDEX_TEMPLATE_FILE)
              .outputPath(versionIndexOutputPath)
              .requireViews(new ArrayList<IndexRequireView>())
              .apiVersion(version);
      indexViews.add(versionIndexViewBuilder.build());
    }
    return indexViews;
  }
}
