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

import static com.google.api.codegen.util.java.JavaTypeTable.JavaLangResolution.IGNORE_JAVA_LANG_CLASH;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.DiscoGapicInterfaceContext;
import com.google.api.codegen.discogapic.SchemaInterfaceContext;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaSchemaTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.StaticLangApiMessageFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMessageView;
import com.google.api.codegen.viewmodel.ViewModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/* Creates the ViewModel for a Discovery Doc Schema Java class. */
public class JavaDiscoGapicRequestToViewTransformer implements DocumentToViewTransformer {
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final JavaNameFormatter nameFormatter = new JavaNameFormatter();
  private static Set<String> reservedKeywords = new HashSet<>();

  static {
    reservedKeywords.addAll(JavaNameFormatter.RESERVED_IDENTIFIER_SET);
    reservedKeywords.add("Builder");
  }

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String SCHEMA_TEMPLATE_FILENAME = "java/message.snip";

  public JavaDiscoGapicRequestToViewTransformer(
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

    // Escape any schema's field names that are Java keywords.

    for (Schema schema : context.getDocument().schemas().values()) {
      Map<SchemaInterfaceContext, StaticLangApiMessageView> contextViews =
          new TreeMap<>(SchemaInterfaceContext.comparator);
      generateSchemaClasses(contextViews, context, schema);
      for (Map.Entry<SchemaInterfaceContext, StaticLangApiMessageView> contextView :
          contextViews.entrySet()) {
        surfaceSchemas.add(generateSchemaFile(contextView.getKey(), contextView.getValue()));
      }
    }
    Collections.sort(
        surfaceSchemas,
        new Comparator<ViewModel>() {
          @Override
          public int compare(ViewModel o1, ViewModel o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.outputPath(), o2.outputPath());
          }
        });
    return surfaceSchemas;
  }

  private SchemaTypeTable createTypeTable(String implicitPackageName) {
    return new SchemaTypeTable(
        new JavaTypeTable(implicitPackageName, IGNORE_JAVA_LANG_CLASH),
        new JavaSchemaTypeNameConverter(implicitPackageName, nameFormatter));
  }

  /* Given a message view, creates a top-level message file view. */
  private StaticLangApiMessageFileView generateSchemaFile(
      SchemaInterfaceContext context, StaticLangApiMessageView messageView) {
    StaticLangApiMessageFileView.Builder apiFile = StaticLangApiMessageFileView.newBuilder();
    apiFile.templateFileName(SCHEMA_TEMPLATE_FILENAME);
    addApiImports(context.getSchemaTypeTable());
    apiFile.schema(messageView);

    String outputPath = pathMapper.getOutputPath(null, context.getDocContext().getProductConfig());
    apiFile.outputPath(outputPath + File.separator + messageView.innerTypeName() + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiMessageView generateSchemaClasses(
      Map<SchemaInterfaceContext, StaticLangApiMessageView> messageViewAccumulator,
      DiscoGapicInterfaceContext documentContext,
      Schema schema) {

    SchemaTypeTable schemaTypeTable = documentContext.getSchemaTypeTable().cloneEmpty();

    SchemaInterfaceContext context =
        SchemaInterfaceContext.create(schema, schemaTypeTable, documentContext);

    StaticLangApiMessageView.Builder schemaView = StaticLangApiMessageView.newBuilder();

    // Child schemas cannot have the same symbols as parent schemas, but sibling schemas can have
    // the same symbols.
    SymbolTable symbolTableCopy = SymbolTable.fromSeed(reservedKeywords);

    String schemaId =
        Name.anyCamel(schema.id().isEmpty() ? schema.key() : schema.id()).toLowerCamel();
    String schemaName =
        nameFormatter.privateFieldName(Name.anyCamel(symbolTableCopy.getNewSymbol(schemaId)));

    schemaView.name(schemaName);
    schemaView.defaultValue(schema.defaultValue());
    schemaView.description(schema.description());
    // Getters and setters use unescaped name for better readability on public methods.
    schemaView.fieldGetFunction(context.getDiscoGapicNamer().getResourceGetterName(schemaId));
    schemaView.fieldSetFunction(context.getDiscoGapicNamer().getResourceSetterName(schemaId));
    String schemaTypeName = schemaTypeTable.getAndSaveNicknameForElementType(schema);

    schemaView.typeName(schemaTypeName);
    if (schema.type() == Type.ARRAY) {
      schemaView.innerTypeName(schemaTypeTable.getInnerTypeNameFor(schema));
    } else {
      schemaView.innerTypeName(schemaTypeName);
    }

    // Generate a Schema view from each property.
    List<StaticLangApiMessageView> viewProperties = new LinkedList<>();
    List<Schema> schemaProperties = new LinkedList<>();
    schemaProperties.addAll(schema.properties().values());
    if (schema.items() != null) {
      schemaProperties.addAll(schema.items().properties().values());
    }
    for (Schema property : schemaProperties) {
      viewProperties.add(generateSchemaClasses(messageViewAccumulator, documentContext, property));
      if (!property.properties().isEmpty() || (property.items() != null)) {
        // Add non-primitive-type property to imports.
        schemaTypeTable.getAndSaveNicknameFor(property);
      }
    }
    Collections.sort(viewProperties);
    schemaView.properties(viewProperties);

    if (!schema.properties().isEmpty()
        || (schema.items() != null && !schema.items().properties().isEmpty())) {
      // This is a top-level Schema, so add it to list of file ViewModels for rendering.
      messageViewAccumulator.put(context, schemaView.build());
    }
    return schemaView.build();
  }

  private void addApiImports(SchemaTypeTable typeTable) {
    typeTable.saveNicknameFor("com.google.api.core.BetaApi");
    typeTable.saveNicknameFor("java.io.Serializable");
    typeTable.saveNicknameFor("javax.annotation.Generated");
  }
}
