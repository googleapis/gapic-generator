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
package com.google.api.codegen.discogapic.transformer.java;

import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.DiscoGapicInterfaceContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StandardTransformationContext;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformer;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaSchemaTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.transformer.java.JavaSurfaceTransformer;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.OperationSnapshotCallableView;
import com.google.api.codegen.viewmodel.StaticLangFileView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Arrays;
import java.util.List;

/**
 * The ModelToViewTransformer to transform a DiscoApiModel into the standard GAPIC surface in Java.
 */
public class JavaDiscoGapicSurfaceTransformer
    implements ModelToViewTransformer<DiscoApiModel>, SurfaceTransformer {
  private final GapicCodePathMapper pathMapper;

  private final JavaNameFormatter nameFormatter = new JavaNameFormatter();

  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);

  private static final String API_TEMPLATE_FILENAME = "java/main.snip";
  private static final String SETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String STUB_SETTINGS_TEMPLATE_FILENAME = "java/stub_settings.snip";
  private static final String STUB_INTERFACE_TEMPLATE_FILENAME = "java/stub_interface.snip";
  private static final String RPC_STUB_TEMPLATE_FILENAME = "java/http_stub.snip";
  private static final String CALLABLE_FACTORY_TEMPLATE_FILENAME =
      "java/http_callable_factory.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME =
      "java/page_streaming_response.snip";

  private static final String OPERATION_CALLABLE_TEMPLATE_FILENAME = "java/operation_callable.snip";

  public JavaDiscoGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = Preconditions.checkNotNull(pathMapper);
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        API_TEMPLATE_FILENAME,
        SETTINGS_TEMPLATE_FILENAME,
        STUB_SETTINGS_TEMPLATE_FILENAME,
        STUB_INTERFACE_TEMPLATE_FILENAME,
        RPC_STUB_TEMPLATE_FILENAME,
        CALLABLE_FACTORY_TEMPLATE_FILENAME,
        PACKAGE_INFO_TEMPLATE_FILENAME,
        PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME,
        OPERATION_CALLABLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(DiscoApiModel model, GapicProductConfig productConfig) {
    JavaSurfaceTransformer commonSurfaceTransformer =
        new JavaSurfaceTransformer(
            pathMapper, this, RPC_STUB_TEMPLATE_FILENAME, CALLABLE_FACTORY_TEMPLATE_FILENAME);
    List<ViewModel> commonSurfaces = commonSurfaceTransformer.transform(model, productConfig);
    return ImmutableList.<ViewModel>builder()
        .addAll(commonSurfaces)
        .addAll(transformDiscovery(model, productConfig))
        .build();
  }

  /** Generate files that only Discovery Java clients will use. */
  private List<ViewModel> transformDiscovery(
      DiscoApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> discoveryClientFiles = ImmutableList.builder();
    SurfaceNamer baseSurfaceNamer = createSurfaceNamer(productConfig);
    String lroPackageName = baseSurfaceNamer.getApiLroPackageName();
    StandardTransformationContext context =
        StandardTransformationContext.create(
            productConfig,
            new JavaSurfaceNamer(lroPackageName, lroPackageName),
            createTypeTable(lroPackageName));
    discoveryClientFiles.add(generateOperationCallableView(model, context));
    return discoveryClientFiles.build();
  }

  @Override
  public DiscoGapicInterfaceContext createInterfaceContext(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      ImportTypeTable importTypeTable) {
    return newInterfaceContext(apiInterface, productConfig, namer, importTypeTable);
  }

  static DiscoGapicInterfaceContext newInterfaceContext(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      ImportTypeTable importTypeTable) {
    return DiscoGapicInterfaceContext.createWithInterface(
        apiInterface,
        productConfig,
        importTypeTable,
        namer,
        JavaFeatureConfig.create(productConfig));
  }

  @Override
  public SchemaTypeTable createTypeTable(String implicitPackageName) {
    return new SchemaTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaSchemaTypeNameConverter(implicitPackageName, nameFormatter),
        new JavaSurfaceNamer(implicitPackageName, implicitPackageName));
  }

  private StaticLangFileView<OperationSnapshotCallableView> generateOperationCallableView(
      DiscoApiModel apiModel, TransformationContext context) {
    StaticLangFileView.Builder<OperationSnapshotCallableView> apiFile =
        StaticLangFileView.newBuilder();

    apiFile.templateFileName(OPERATION_CALLABLE_TEMPLATE_FILENAME);

    apiFile.classView(generateOperationSnapshotCallableView(context));

    String outputPath =
        pathMapper.getOutputPath(
            context.getNamer().getApiLroOperationCallableName(), context.getProductConfig());
    String className = context.getNamer().getApiLroOperationCallableName(apiModel.getDocument());
    apiFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context, className));

    return apiFile.build();
  }

  private OperationSnapshotCallableView generateOperationSnapshotCallableView(
      TransformationContext context) {
    ImportTypeTable typeTable = context.getImportTypeTable();

    addOperationCallableImports(context.getImportTypeTable());

    OperationSnapshotCallableView.Builder xapiClass = OperationSnapshotCallableView.newBuilder();

    // TODO(andrealin): Parameterize this in config.
    xapiClass.operationResourceName(
        typeTable.getAndSaveNicknameFor("com.google.cloud.compute.v1.Operation"));
    xapiClass.operationSnapshotName(
        typeTable.getAndSaveNicknameFor("com.google.cloud.compute.v1.ComputeOperationSnapshot"));
    return xapiClass.build();
  }

  private void addOperationCallableImports(ImportTypeTable typeTable) {
    typeTable.saveNicknameFor("com.google.api.core.ApiFunction");
    typeTable.saveNicknameFor("com.google.api.core.ApiFuture");
    typeTable.saveNicknameFor("com.google.api.core.ApiFutures");
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.longrunning.OperationSnapshot");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.ApiCallContext");
    typeTable.saveNicknameFor("com.google.api.gax.rpc.UnaryCallable");
    typeTable.saveNicknameFor("com.google.cloud.compute.v1.Operation");
  }

  @Override
  public JavaSurfaceNamer createSurfaceNamer(GapicProductConfig productConfig) {
    return new JavaSurfaceNamer(
        productConfig.getPackageName(), productConfig.getPackageName(), new JavaNameFormatter());
  }
}
