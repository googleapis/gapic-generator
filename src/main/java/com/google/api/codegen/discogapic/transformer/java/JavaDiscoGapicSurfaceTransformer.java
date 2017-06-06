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

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.DiscoGapicInterfaceContext;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DiscoTypeTable;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaSchemaTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.FormatResourceFunctionView;
import com.google.api.codegen.viewmodel.ParseResourceFunctionView;
import com.google.api.codegen.viewmodel.PathTemplateView;
import com.google.api.codegen.viewmodel.ServiceDocView;
import com.google.api.codegen.viewmodel.StaticLangApiFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangApiView;
import com.google.api.codegen.viewmodel.ViewModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class JavaDiscoGapicSurfaceTransformer implements DocumentToViewTransformer {
  private final GapicCodePathMapper pathMapper;
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String XSETTINGS_TEMPLATE_FILENAME = "java/settings.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME =
      "java/page_streaming_response.snip";

  public JavaDiscoGapicSurfaceTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    // TODO use packageMetadataConfig
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        XAPI_TEMPLATE_FILENAME,
        XSETTINGS_TEMPLATE_FILENAME,
        PACKAGE_INFO_TEMPLATE_FILENAME,
        PAGE_STREAMING_RESPONSE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Document document, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new JavaSurfaceNamer(productConfig.getPackageName());

    List<ServiceDocView> serviceDocs = new ArrayList<>();

    DiscoGapicInterfaceContext context =
        DiscoGapicInterfaceContext.create(
            document,
            productConfig,
            createTypeTable(productConfig.getPackageName()),
            new JavaDiscoGapicNamer(),
            namer,
            JavaFeatureConfig.newBuilder().enableStringFormatFunctions(false).build()
        );


    StaticLangApiFileView apiFile = generateApiFile(context);
    surfaceDocs.add(apiFile);

    serviceDocs.add(apiFile.api().doc());

    return surfaceDocs;
  }

  private DiscoTypeTable createTypeTable(String implicitPackageName) {
    return new DiscoTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaSchemaTypeNameConverter(implicitPackageName));
  }

  private StaticLangApiFileView generateApiFile(DiscoGapicInterfaceContext context) {
    StaticLangApiFileView.Builder apiFile = StaticLangApiFileView.newBuilder();

    apiFile.templateFileName(XAPI_TEMPLATE_FILENAME);

    apiFile.api(generateApiClass(context));

    String outputPath = pathMapper.getOutputPath(null, context.getProductConfig());
    String className = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    apiFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiView generateApiClass(DiscoGapicInterfaceContext context) {
    addApiImports(context);

    List<StaticLangApiMethodView> methods = new ArrayList<>();

    StaticLangApiView.Builder xapiClass = StaticLangApiView.newBuilder();

    String name = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    xapiClass.releaseLevelAnnotation(context.getNamer().getReleaseAnnotation(ReleaseLevel.ALPHA));
    xapiClass.name(name);
    xapiClass.settingsClassName(
        context.getNamer().getApiSettingsClassName(context.getInterfaceConfig()));
    xapiClass.apiCallableMembers(new ArrayList<ApiCallableView>());
    xapiClass.pathTemplates(new ArrayList<PathTemplateView>());
    xapiClass.formatResourceFunctions(new ArrayList<FormatResourceFunctionView>());
    xapiClass.parseResourceFunctions(new ArrayList<ParseResourceFunctionView>());
    xapiClass.apiMethods(methods);
    xapiClass.hasDefaultInstance(true);
    xapiClass.hasLongRunningOperations(false);

    return xapiClass.build();
  }

  private void addApiImports(DiscoGapicInterfaceContext context) {
    DiscoTypeTable typeTable = context.getDiscoTypeTable();
    // TODO several of these can be deleted (e.g. DiscoGapic doesn't use grpc)
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.ChannelAndExecutor");
    typeTable.saveNicknameFor("com.google.api.gax.grpc.UnaryCallable");
    typeTable.saveNicknameFor("com.google.api.pathtemplate.PathTemplate");
    typeTable.saveNicknameFor("io.grpc.ManagedChannel");
    typeTable.saveNicknameFor("java.io.Closeable");
    typeTable.saveNicknameFor("java.io.IOException");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("java.util.concurrent.ScheduledExecutorService");
    typeTable.saveNicknameFor("javax.annotation.Generated");
  }
}
