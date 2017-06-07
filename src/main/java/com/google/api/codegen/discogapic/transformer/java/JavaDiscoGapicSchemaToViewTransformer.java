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
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DiscoTypeTable;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaSchemaTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.SimpleMessagePropertyView;
import com.google.api.codegen.viewmodel.StaticLangApiMessageFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMessageView;
import com.google.api.codegen.viewmodel.ViewModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/* Creates the ViewModel for a Discovery Doc Schema Java class. */
public class JavaDiscoGapicSchemaToViewTransformer implements DocumentToViewTransformer {
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final JavaNameFormatter nameFormatter = new JavaNameFormatter();

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String SCHEMA_TEMPLATE_FILENAME = "java/message.snip";

  public JavaDiscoGapicSchemaToViewTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageMetadataConfig;
    // TODO use packageMetadataConfig
  }

  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        XAPI_TEMPLATE_FILENAME, PACKAGE_INFO_TEMPLATE_FILENAME, SCHEMA_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Document document, GapicProductConfig productConfig) {
    List<ViewModel> surfaceSchemas = new ArrayList<>();
    JavaDiscoGapicNamer discoGapicNamer = new JavaDiscoGapicNamer();

    DiscoGapicInterfaceContext context =
        DiscoGapicInterfaceContext.create(
            document,
            productConfig,
            createTypeTable(productConfig.getPackageName()),
            discoGapicNamer,
            new JavaSurfaceNamer(productConfig.getPackageName()),
            JavaFeatureConfig.newBuilder().enableStringFormatFunctions(false).build());

    StaticLangApiMessageFileView apiFile;
    for (Map.Entry<String, Schema> entry : context.getDocument().schemas().entrySet()) {
      apiFile = generateSchemaFile(context, entry.getValue());
      surfaceSchemas.add(apiFile);
    }
    return surfaceSchemas;
  }

  private DiscoTypeTable createTypeTable(String implicitPackageName) {
    return new DiscoTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaSchemaTypeNameConverter(implicitPackageName));
  }

  private StaticLangApiMessageFileView generateSchemaFile(
      DiscoGapicInterfaceContext context, Schema schema) {
    StaticLangApiMessageFileView.Builder apiFile = StaticLangApiMessageFileView.newBuilder();
    // Escape any schema's field names that are Java keywords.
    SymbolTable schemaSymbolTable = SymbolTable.fromSeed(JavaNameFormatter.RESERVED_IDENTIFIER_SET);

    apiFile.templateFileName(SCHEMA_TEMPLATE_FILENAME);

    StaticLangApiMessageView messageView = generateSchemaClass(context, schema, schemaSymbolTable);
    apiFile.schema(messageView);

    String outputPath = pathMapper.getOutputPath(null, context.getProductConfig());
    apiFile.outputPath(outputPath + File.separator + messageView.typeName() + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiMessageView generateSchemaClass(
      DiscoGapicInterfaceContext context, Schema schema, SymbolTable schemaSymbolTable) {
    addApiImports(context);

    StaticLangApiMessageView.Builder schemaView = StaticLangApiMessageView.newBuilder();

    schemaView.typeName(nameFormatter.publicClassName(Name.anyCamel(schema.id())));
    schemaView.type(schema.type());
    // TODO(andrealin): use symbol table to make sure Schema names aren't Java keywords.
    schemaView.defaultValue(schema.defaultValue());

    // Map each property name to the Java typeName of the property.
    List<SimpleMessagePropertyView> properties = new LinkedList<>();
    for (Map.Entry<String, Schema> propertyEntry : schema.properties().entrySet()) {
      String propertyString = schemaSymbolTable.getNewSymbol(propertyEntry.getKey());
      Schema property = propertyEntry.getValue();
      SimpleMessagePropertyView.Builder simpleProperty =
          SimpleMessagePropertyView.newBuilder().name(propertyString);
      String typeName = context.getDiscoTypeTable().getAndSaveNicknameForElementType(property);
      simpleProperty.typeName(typeName);
      simpleProperty.fieldGetFunction(
          context.getDiscoGapicNamer().getResourceGetterName(propertyString));
      simpleProperty.fieldSetFunction(
          context.getDiscoGapicNamer().getResourceSetterName(propertyString));
      properties.add(simpleProperty.build());
    }
    Collections.sort(
        properties,
        new Comparator<SimpleMessagePropertyView>() {
          @Override
          public int compare(SimpleMessagePropertyView o1, SimpleMessagePropertyView o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.name(), o2.name());
          };
        });
    schemaView.properties(properties);

    return schemaView.build();
  }

  private void addApiImports(DiscoGapicInterfaceContext context) {
    DiscoTypeTable typeTable = context.getDiscoTypeTable();
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("java.io.Serializable");
    typeTable.saveNicknameFor("java.util.List");
    typeTable.saveNicknameFor("javax.annotation.Generated");
  }
}
