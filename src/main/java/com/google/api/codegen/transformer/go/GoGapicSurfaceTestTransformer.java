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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.MockServiceTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.testing.MockCombinedView;
import com.google.api.codegen.viewmodel.testing.MockServiceImplView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GoGapicSurfaceTestTransformer implements ModelToViewTransformer {
  private static final String MOCK_SERVICE_TEMPLATE_FILE = "go/mock.snip";

  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new GoImportTransformer());
  private final MockServiceTransformer mockServiceTransformer = new MockServiceTransformer();
  private final FeatureConfig featureConfig = new GoFeatureConfig();

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(MOCK_SERVICE_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    GoSurfaceNamer namer = new GoSurfaceNamer(apiConfig.getPackageName());
    models.add(generateMockServiceView(model, apiConfig, namer));
    return models;
  }

  private MockCombinedView generateMockServiceView(
      Model model, ApiConfig apiConfig, SurfaceNamer namer) {
    Map<String, TypeAlias> imports = new TreeMap<>();
    List<MockServiceImplView> impls = new ArrayList<>();
    FileHeaderView fileHeader =
        fileHeaderTransformer.generateFileHeader(
            apiConfig, Collections.<String, TypeAlias>emptyMap(), namer);

    for (Interface service : mockServiceTransformer.getGrpcInterfacesToMock(model, apiConfig)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              GoGapicSurfaceTransformer.createTypeTable(),
              namer,
              featureConfig);
      impls.add(
          MockServiceImplView.newBuilder()
              .outputPath("")
              .templateFileName("")
              .grpcClassName(namer.getGrpcServerTypeName(service))
              .name(namer.getMockGrpcServiceImplName(service))
              .grpcMethods(mockServiceTransformer.createMockGrpcMethodViews(context))
              .fileHeader(fileHeader)
              .build());
      imports.putAll(context.getTypeTable().getImports());
    }

    return MockCombinedView.newBuilder()
        .outputPath(apiConfig.getPackageName() + File.separator + "mock_test.go")
        .serviceImpls(impls)
        .templateFileName(MOCK_SERVICE_TEMPLATE_FILE)
        .fileHeader(fileHeaderTransformer.generateFileHeader(apiConfig, imports, namer))
        .build();
  }
}
