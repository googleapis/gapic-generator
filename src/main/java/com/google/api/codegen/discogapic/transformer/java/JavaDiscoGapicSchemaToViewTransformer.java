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

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.DiscoGapicInterfaceContext;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.viewmodel.StaticLangApiSchemaView;
import com.google.api.codegen.viewmodel.StaticLangApiSchemaFileView;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaModelTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ViewModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/* Creates the ViewModel for a Discovery Doc Schema Java class. */
public class JavaDiscoGapicSchemaToViewTransformer implements DocumentToViewTransformer {
  private final GapicCodePathMapper pathMapper;
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String SCHEMA_TEMPLATE_FILENAME = "java/schema.snip";

  public JavaDiscoGapicSchemaToViewTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    // TODO use packageMetadataConfig
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        XAPI_TEMPLATE_FILENAME,
        PACKAGE_INFO_TEMPLATE_FILENAME,
        SCHEMA_TEMPLATE_FILENAME);
  }


  @Override
  public List<ViewModel> transform(Document document, GapicProductConfig productConfig) {
    List<ViewModel> surfaceSchemas = new ArrayList<>();
    SurfaceNamer namer = new JavaSurfaceNamer(productConfig.getPackageName());

    DiscoGapicInterfaceContext context =
        DiscoGapicInterfaceContext.create(
            document,
            productConfig,
            createTypeTable(productConfig.getPackageName()),
            namer,
            JavaFeatureConfig.newBuilder().enableStringFormatFunctions(false).build());

    StaticLangApiSchemaFileView apiFile;
    for (Map.Entry<String, Schema> entry : context.getDocument().schemas().entrySet()) {
      apiFile = generateSchemaFile(context, entry.getKey(), entry.getValue());
      surfaceSchemas.add(apiFile);
    }


    return surfaceSchemas;
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaModelTypeNameConverter(implicitPackageName));
  }

  private StaticLangApiSchemaFileView generateSchemaFile(DiscoGapicInterfaceContext context,
      String schemaName, Schema schema) {
    StaticLangApiSchemaFileView.Builder apiFile = StaticLangApiSchemaFileView.newBuilder();

    apiFile.templateFileName(SCHEMA_TEMPLATE_FILENAME);

    apiFile.schema(generateSchemaClass(context, schemaName, schema));

    String outputPath = pathMapper.getOutputPath(null, context.getProductConfig());
    String className = context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
    apiFile.outputPath(outputPath + File.separator + className + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiSchemaView generateSchemaClass(DiscoGapicInterfaceContext context,
      String schemaName, Schema schema) {
    addApiImports(context);
    List<StaticLangApiSchemaView> schemaViews = new ArrayList<>();

    StaticLangApiSchemaView.Builder schemaView = StaticLangApiSchemaView.newBuilder();

    schemaView.typeName(schemaName);
    schemaView.type(schema.type());
    // TODO(andrealin): apply Java naming format.
    schemaView.className(schemaName);
    schemaView.defaultValue(schema.defaultValue());
//    schemaView.enumValues(schema.)

    return schemaView.build();
  }

  private void addApiImports(DiscoGapicInterfaceContext context) {
    ModelTypeTable typeTable = context.getModelTypeTable();
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("java.util.ArrayList");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("javax.annotation.Generated");
  }
}
