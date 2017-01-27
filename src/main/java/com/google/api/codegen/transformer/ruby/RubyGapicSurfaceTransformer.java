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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.IndexRequireView;
import com.google.api.codegen.viewmodel.metadata.IndexView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in Ruby. */
public class RubyGapicSurfaceTransformer implements ModelToViewTransformer {
  private static final String VERSION_INDEX_TEMPLATE_FILE = "ruby/version_index.snip";

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(VERSION_INDEX_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.add(generateVersionIndexView(model, apiConfig));
    return views.build();
  }

  private ViewModel generateVersionIndexView(Model model, ApiConfig apiConfig) {
    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    SurfaceNamer namer = new RubySurfaceNamer(apiConfig.getPackageName());

    ImmutableList.Builder<IndexRequireView> requireViewsBuilder = ImmutableList.builder();
    for (Interface service : services) {
      requireViewsBuilder.add(simpleRequireView(namer.getServiceFileName(service), namer));
    }

    ImmutableList.Builder<IndexRequireView> protoRequireViewsBuilder = ImmutableList.builder();
    for (Interface service : services) {
      protoRequireViewsBuilder.add(
          simpleRequireView(namer.getProtoFileImportFromService(service), namer));
    }

    boolean hasLongrunning = false;
    for (Interface service : services) {
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new RubyTypeTable(apiConfig.getPackageName()),
              new RubyModelTypeNameConverter(apiConfig.getPackageName()));
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              modelTypeTable,
              new RubySurfaceNamer(apiConfig.getPackageName()),
              new RubyFeatureConfig());
      if (context.getLongRunningMethods().iterator().hasNext()) {
        hasLongrunning = true;
        break;
      }
    }

    if (hasLongrunning) {
      requireViewsBuilder.add(simpleRequireView("google/longrunning/operations_client", namer));
      protoRequireViewsBuilder.add(simpleRequireView("google/longrunning/operations_pb", namer));
    }

    List<IndexRequireView> requireViews = requireViewsBuilder.build();
    IndexView.Builder versionIndexViewBuilder =
        IndexView.newBuilder()
            .templateFileName(VERSION_INDEX_TEMPLATE_FILE)
            .outputPath(namer.getIndexOutputFilePath(services.iterator().next()))
            .requireViews(requireViews)
            .protoRequireViews(protoRequireViewsBuilder.build());
    versionIndexViewBuilder.apiVersion(namer.getApiWrapperModuleVersion());

    return versionIndexViewBuilder.build();
  }

  private IndexRequireView simpleRequireView(String fileName, SurfaceNamer namer) {
    return IndexRequireView.newBuilder()
        .clientName(namer.getNotImplementedString("IndexRequireView.clientName"))
        .fileName(fileName)
        .build();
  }
}
