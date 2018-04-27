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

import static com.google.api.codegen.util.java.JavaTypeTable.JavaLangResolution.IGNORE_JAVA_LANG_CLASH;

import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.DiscoveryField;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.SchemaTransformationContext;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.discovery.Schema.Type;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DiscoGapicInterfaceContext;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
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
public class JavaDiscoGapicSchemaToViewTransformer implements DocumentToViewTransformer {
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

  private static final String SCHEMA_TEMPLATE_FILENAME = "java/message.snip";

  public JavaDiscoGapicSchemaToViewTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageMetadataConfig;
    // TODO use packageMetadataConfig
  }

  public List<String> getTemplateFileNames() {
    return Arrays.asList(SCHEMA_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(DiscoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceSchemas = new ArrayList<>();
    String packageName = productConfig.getPackageName();
    JavaSurfaceNamer surfaceNamer = new JavaSurfaceNamer(packageName, packageName, nameFormatter);
    DiscoGapicInterfaceContext context =
        DiscoGapicInterfaceContext.createWithoutInterface(
            model,
            productConfig,
            createTypeTable(productConfig.getPackageName(), surfaceNamer),
            surfaceNamer,
            JavaFeatureConfig.newBuilder().enableStringFormatFunctions(true).build());

    Map<Schema, String> schemaNames =
        new TreeMap<>(
            new Comparator<Schema>() {
              @Override
              public int compare(Schema o1, Schema o2) {
                return schemaToStringNoDescriptionParent(o1)
                    .compareTo(schemaToStringNoDescriptionParent(o2));
              }
            });
    Comparator<String> caseInsensitiveComparator =
        (String s1, String s2) -> s1.compareToIgnoreCase(s2);
    SymbolTable classNameSymbolTable =
        SymbolTable.fromSeed(reservedKeywords, caseInsensitiveComparator);
    for (Schema schema : context.getDocument().schemas().values()) {
      Map<SchemaTransformationContext, StaticLangApiMessageView> contextViews =
          new TreeMap<>(SchemaTransformationContext.comparator);
      generateSchemaClasses(contextViews, context, schema, classNameSymbolTable, schemaNames);
      for (Map.Entry<SchemaTransformationContext, StaticLangApiMessageView> contextView :
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

  private SchemaTypeTable createTypeTable(String implicitPackageName, SurfaceNamer namer) {
    return new SchemaTypeTable(
        new JavaTypeTable(implicitPackageName, IGNORE_JAVA_LANG_CLASH),
        new JavaSchemaTypeNameConverter(implicitPackageName, nameFormatter),
        namer);
  }

  /* Given a message view, creates a top-level message file view. */
  private StaticLangApiMessageFileView generateSchemaFile(
      SchemaTransformationContext context, StaticLangApiMessageView messageView) {
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
      Map<SchemaTransformationContext, StaticLangApiMessageView> messageViewAccumulator,
      DiscoGapicInterfaceContext documentContext,
      Schema schema,
      SymbolTable classNameSymbolTable,
      Map<Schema, String> schemaNames) {

    SchemaTypeTable schemaTypeTable = documentContext.getSchemaTypeTable().cloneEmpty();
    String schemaTypeName = schemaTypeTable.getAndSaveNicknameFor(schema);

    SchemaTransformationContext context =
        SchemaTransformationContext.create(
            schema.getIdentifier(), schemaTypeTable, documentContext);

    StaticLangApiMessageView.Builder schemaView = StaticLangApiMessageView.newBuilder();
    boolean hasRequiredProperties = false;

    // Child schemas cannot have the same symbols as parent schemas, but sibling schemas can have
    // the same symbols.
    SymbolTable symbolTableCopy = SymbolTable.fromSeed(reservedKeywords);

    String schemaId = Name.anyCamel(schema.getIdentifier()).toLowerCamel();
    String schemaName =
        nameFormatter.privateFieldName(Name.anyCamel(symbolTableCopy.getNewSymbol(schemaId)));

    schemaView.name(schemaName);
    schemaView.defaultValue(schema.defaultValue());
    schemaView.description(schema.description());

    FieldModel schemaModel = DiscoveryField.create(schema, documentContext.getApiModel());
    schemaView.fieldGetFunction(context.getNamer().getFieldGetFunctionName(schemaModel));
    schemaView.fieldSetFunction(context.getNamer().getFieldSetFunctionName(schemaModel));
    schemaView.fieldAddFunction(context.getNamer().getFieldAddFunctionName(schemaModel));

    // Generate a Schema view from each property.
    List<StaticLangApiMessageView> viewProperties = new LinkedList<>();
    List<Schema> schemaProperties = new LinkedList<>();
    schemaProperties.addAll(schema.properties().values());
    if (schema.items() != null) {
      schemaProperties.addAll(schema.items().properties().values());
    }
    for (Schema property : schemaProperties) {
      viewProperties.add(
          generateSchemaClasses(
              messageViewAccumulator,
              documentContext,
              property,
              classNameSymbolTable,
              schemaNames));
      if (!property.properties().isEmpty() || (property.items() != null)) {
        // Add non-primitive-type property to imports.
        schemaTypeTable.getAndSaveNicknameFor(property);
      }
      if (property.required()) {
        hasRequiredProperties = true;
      }
    }
    Collections.sort(viewProperties);
    schemaView.properties(viewProperties);

    schemaView.canRepeat(schema.repeated() || schema.type().equals(Type.ARRAY));
    schemaView.isRequired(schema.required());
    schemaView.isRequestMessage(false);

    if (DiscoveryField.isTopLevelSchema(schema)) {
      String innerTypeName = Name.anyCamel(schemaModel.getSimpleName()).toUpperCamel();
      if (schema.repeated() || schema.type() == Type.ARRAY) {
        schemaView.typeName(schemaTypeName);
      } else {
        schemaView.typeName(innerTypeName);
      }
      schemaView.innerTypeName(innerTypeName);

      schemaNames.put(schema, schemaModel.getSimpleName());
    } else {
      // This is a primitive type.
      schemaView.typeName(schemaTypeName);
      if (schema.repeated() || schema.type() == Type.ARRAY) {
        schemaView.innerTypeName(schemaTypeTable.getInnerTypeNameFor(schema));
      } else {
        schemaView.innerTypeName(schemaTypeName);
      }
    }

    schemaView.hasRequiredProperties(hasRequiredProperties);
    StaticLangApiMessageView messageView = schemaView.build();

    // Add it to list of file ViewModels for rendering.
    if (DiscoveryField.isTopLevelSchema(schema)) {
      messageViewAccumulator.put(context, messageView);
    }
    return messageView;
  }

  private void addApiImports(ImportTypeTable typeTable) {
    typeTable.getAndSaveNicknameFor("com.google.api.core.BetaApi");
    typeTable.getAndSaveNicknameFor("com.google.api.gax.httpjson.ApiMessage");
    typeTable.getAndSaveNicknameFor("com.google.common.collect.ImmutableList");
    typeTable.getAndSaveNicknameFor("com.google.common.collect.ImmutableMap");
    typeTable.getAndSaveNicknameFor("java.util.ArrayList");
    typeTable.getAndSaveNicknameFor("java.util.Collections");
    typeTable.getAndSaveNicknameFor("java.util.HashMap");
    typeTable.getAndSaveNicknameFor("java.util.List");
    typeTable.getAndSaveNicknameFor("java.util.Map");
    typeTable.getAndSaveNicknameFor("java.util.Objects");
    typeTable.getAndSaveNicknameFor("java.util.Set");
    typeTable.getAndSaveNicknameFor("javax.annotation.Generated");
    typeTable.getAndSaveNicknameFor("javax.annotation.Nullable");
  }

  private static String schemaToStringNoDescriptionParent(Schema schema) {
    return String.format(
        "additionalProperties: %s,\n"
            + " defaultValue: %s,\n"
            + " format: %s,\n"
            + " id: %s,\n"
            + " isEnum: %s,\n"
            + " items: %s,\n"
            + " key: %s,\n"
            + " location: %s,\n"
            + " pattern: %s,\n"
            + " properties keys: %s,\n"
            + " reference: %s,\n"
            + " repeated: %s,\n"
            + " required: %s,\n"
            + " type: %s",
        schema.additionalProperties() == null ? "" : schema.additionalProperties().getIdentifier(),
        schema.defaultValue(),
        // Skip description.
        schema.format(),
        schema.id(),
        schema.isEnum(),
        schema.items() == null ? "" : schema.items().getIdentifier(),
        schema.key(),
        schema.location(),
        schema.pattern(),
        // Skip parent.
        schema.properties().keySet(),
        schema.reference(),
        schema.repeated(),
        schema.required(),
        schema.type());
  }
}
