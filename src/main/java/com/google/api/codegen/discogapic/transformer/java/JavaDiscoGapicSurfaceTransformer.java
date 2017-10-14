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
package com.google.api.codegen.discogapic.transformer.java;

import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DiscoGapicInterfaceContext;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformer;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaSchemaTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.transformer.java.JavaSurfaceTransformer;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import java.util.Arrays;
import java.util.List;

/** The ModelToViewTransformer to transform a Document into the standard GAPIC surface in Java. */
public class JavaDiscoGapicSurfaceTransformer
    implements DocumentToViewTransformer, SurfaceTransformer {
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageMetadataConfig;

  private final JavaNameFormatter nameFormatter = new JavaNameFormatter();

  private static final String API_TEMPLATE_FILENAME = "java/main.snip";
  private static final String SETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String STUB_INTERFACE_TEMPLATE_FILENAME = "java/stub_interface.snip";
  private static final String RPC_STUB_TEMPLATE_FILENAME = "java/http_stub.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME =
      "java/page_streaming_response.snip";

  public JavaDiscoGapicSurfaceTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageMetadataConfig = packageMetadataConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        API_TEMPLATE_FILENAME,
        SETTINGS_TEMPLATE_FILENAME,
        STUB_INTERFACE_TEMPLATE_FILENAME,
        RPC_STUB_TEMPLATE_FILENAME,
        PACKAGE_INFO_TEMPLATE_FILENAME,
        PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Document document, GapicProductConfig productConfig) {
    JavaSurfaceTransformer surfaceTransformer =
        new JavaSurfaceTransformer(
            pathMapper, packageMetadataConfig, this, RPC_STUB_TEMPLATE_FILENAME);
    return surfaceTransformer.transform(new DiscoApiModel(document), productConfig);
  }

  @Override
  public DiscoGapicInterfaceContext createInterfaceContext(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      ImportTypeTable importTypeTable,
      boolean enableStringFormatFunctions) {
    return newInterfaceContext(
        apiInterface, productConfig, namer, importTypeTable, enableStringFormatFunctions);
  }

  public static DiscoGapicInterfaceContext newInterfaceContext(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      ImportTypeTable importTypeTable,
      boolean enableStringFormatFunctions) {
    return DiscoGapicInterfaceContext.createWithInterface(
        apiInterface,
        productConfig,
        importTypeTable,
        new DiscoGapicNamer(namer),
        JavaFeatureConfig.newBuilder()
            .enableStringFormatFunctions(enableStringFormatFunctions)
            .build());
  }

  @Override
  public SchemaTypeTable createTypeTable(String implicitPackageName) {
    return new SchemaTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaSchemaTypeNameConverter(implicitPackageName, nameFormatter));
  }

  @Override
  public JavaSurfaceNamer createSurfaceNamer(GapicProductConfig productConfig) {
    return new JavaSurfaceNamer(
        productConfig.getPackageName(), productConfig.getPackageName(), new JavaNameFormatter());
  }
}
