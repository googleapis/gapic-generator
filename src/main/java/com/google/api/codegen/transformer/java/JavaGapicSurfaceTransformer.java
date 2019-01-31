/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformer;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.List;

/**
 * The ModelToViewTransformer to transform a ProtoApiModel into the standard GAPIC surface in Java.
 */
public class JavaGapicSurfaceTransformer
    implements ModelToViewTransformer<ProtoApiModel>, SurfaceTransformer {

  private final GapicCodePathMapper pathMapper;

  private static final String API_TEMPLATE_FILENAME = "java/main.snip";
  private static final String SETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String STUB_SETTINGS_TEMPLATE_FILENAME = "java/stub_settings.snip";
  private static final String STUB_INTERFACE_TEMPLATE_FILENAME = "java/stub_interface.snip";
  private static final String GRPC_STUB_TEMPLATE_FILENAME = "java/grpc_stub.snip";
  private static final String GRPC_CALLABLE_FACTORY_TEMPLATE_FILENAME =
      "java/grpc_callable_factory.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME =
      "java/page_streaming_response.snip";

  public JavaGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = Preconditions.checkNotNull(pathMapper);
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        API_TEMPLATE_FILENAME,
        SETTINGS_TEMPLATE_FILENAME,
        STUB_SETTINGS_TEMPLATE_FILENAME,
        STUB_INTERFACE_TEMPLATE_FILENAME,
        GRPC_STUB_TEMPLATE_FILENAME,
        GRPC_CALLABLE_FACTORY_TEMPLATE_FILENAME,
        PACKAGE_INFO_TEMPLATE_FILENAME,
        PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    JavaSurfaceTransformer commonSurfaceTransformer =
        new JavaSurfaceTransformer(
            pathMapper, this, GRPC_STUB_TEMPLATE_FILENAME, GRPC_CALLABLE_FACTORY_TEMPLATE_FILENAME);
    return commonSurfaceTransformer.transform(model, productConfig);
  }

  @Override
  public SurfaceNamer createSurfaceNamer(GapicProductConfig productConfig) {
    return new JavaSurfaceNamer(productConfig.getPackageName(), productConfig.getPackageName());
  }

  @Override
  public GapicInterfaceContext createInterfaceContext(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      ImportTypeTable typeTable) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        (ModelTypeTable) typeTable,
        namer,
        JavaFeatureConfig.create(productConfig));
  }

  @Override
  public ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaModelTypeNameConverter(implicitPackageName));
  }
}
