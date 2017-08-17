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
import com.google.api.codegen.discogapic.SchemaTransformationContext;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
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
import com.google.common.collect.ImmutableMap;
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

/* Creates the ViewModel for a Discovery Doc request object Java class. */
public class JavaDiscoGapicRequestToViewTransformer implements DocumentToViewTransformer {
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final JavaNameFormatter nameFormatter = new JavaNameFormatter();
  private static Set<String> reservedKeywords = new HashSet<>();

  /**
   * Query parameters that may be accepted by any method. See
   * https://cloud.google.com/compute/docs/reference/parameters.
   */
  private static final Map<String, String> STANDARD_QUERY_PARAMS;

  static {
    ImmutableMap.Builder<String, String> queryParams = ImmutableMap.builder();
    queryParams.put("access_token", "OAuth 2.0 token for the current user.");
    queryParams.put(
        "callback", "Name of the JavaScript callback function that handles the response.");
    queryParams.put("fields", "Selector specifying a subset of fields to include in the response.");
    queryParams.put("key", "API key. Required unless you provide an OAuth 2.0 token.");
    queryParams.put("prettyPrint", "Returns response with indentations and line breaks.");
    queryParams.put("quotaUser", "Alternative to userIp.");
    queryParams.put("userIp", "IP address of the end user for whom the API call is being made.");
    STANDARD_QUERY_PARAMS = queryParams.build();
  }

  static {
    reservedKeywords.addAll(JavaNameFormatter.RESERVED_IDENTIFIER_SET);
    reservedKeywords.add("Builder");
  }

  private static final String REQUEST_TEMPLATE_FILENAME = "java/message.snip";

  public JavaDiscoGapicRequestToViewTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageMetadataConfig;
    // TODO use packageMetadataConfig
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(REQUEST_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Document document, GapicProductConfig productConfig) {
    List<ViewModel> surfaceRequests = new ArrayList<>();
    String packageName = productConfig.getPackageName();
    SurfaceNamer surfaceNamer = new JavaSurfaceNamer(packageName, packageName, nameFormatter);
    DiscoGapicNamer discoGapicNamer = new DiscoGapicNamer(surfaceNamer);

    DiscoGapicInterfaceContext context =
        DiscoGapicInterfaceContext.createWithoutInterface(
            document,
            productConfig,
            createTypeTable(productConfig.getPackageName()),
            discoGapicNamer,
            JavaFeatureConfig.newBuilder().enableStringFormatFunctions(false).build());

    for (Method method : context.getDocument().methods()) {
      SchemaTransformationContext requestContext =
          SchemaTransformationContext.create(method.id(), context.getSchemaTypeTable(), context);
      StaticLangApiMessageView requestView = generateRequestClass(requestContext, method);
      surfaceRequests.add(generateRequestFile(requestContext, requestView));
    }
    Collections.sort(
        surfaceRequests,
        new Comparator<ViewModel>() {
          @Override
          public int compare(ViewModel o1, ViewModel o2) {
            return String.CASE_INSENSITIVE_ORDER.compare(o1.outputPath(), o2.outputPath());
          }
        });
    return surfaceRequests;
  }

  /* Given a message view, creates a top-level message file view. */
  private StaticLangApiMessageFileView generateRequestFile(
      SchemaTransformationContext context, StaticLangApiMessageView messageView) {
    StaticLangApiMessageFileView.Builder apiFile = StaticLangApiMessageFileView.newBuilder();
    apiFile.templateFileName(REQUEST_TEMPLATE_FILENAME);
    addApiImports(context.getImportTypeTable());
    apiFile.schema(messageView);

    String outputPath = pathMapper.getOutputPath(null, context.getDocContext().getProductConfig());
    apiFile.outputPath(outputPath + File.separator + messageView.typeName() + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiMessageView generateRequestClass(
      SchemaTransformationContext context, Method method) {
    StaticLangApiMessageView.Builder requestView = StaticLangApiMessageView.newBuilder();

    SymbolTable symbolTable = SymbolTable.fromSeed(reservedKeywords);

    String requestClassId =
        context.getNamer().privateFieldName(DiscoGapicNamer.getRequestName(method));
    String requestName =
        nameFormatter.privateFieldName(Name.anyCamel(symbolTable.getNewSymbol(requestClassId)));
    boolean hasRequiredProperties = false;

    requestView.name(requestName);
    requestView.description(method.description());

    String requestTypeName = nameFormatter.publicClassName(Name.anyCamel(requestClassId));
    requestView.typeName(requestTypeName);
    requestView.innerTypeName(requestTypeName);

    List<StaticLangApiMessageView> properties = new LinkedList<>();

    // Add the standard query parameters.
    for (String param : STANDARD_QUERY_PARAMS.keySet()) {
      if (method.parameters().containsKey(param)) {
        continue;
      }
      StaticLangApiMessageView.Builder paramView = StaticLangApiMessageView.newBuilder();
      paramView.description(STANDARD_QUERY_PARAMS.get(param));
      paramView.name(symbolTable.getNewSymbol(param));
      paramView.typeName("String");
      paramView.innerTypeName("String");
      paramView.isRequired(false);
      paramView.canRepeat(false);
      paramView.fieldGetFunction(context.getDiscoGapicNamer().getResourceGetterName(param));
      paramView.fieldSetFunction(context.getDiscoGapicNamer().getResourceSetterName(param));
      paramView.properties(new LinkedList<StaticLangApiMessageView>());
      paramView.isRequestMessage(false);
      paramView.hasRequiredProperties(false);
      properties.add(paramView.build());
    }

    for (Map.Entry<String, Schema> entry : method.parameters().entrySet()) {
      Schema param = entry.getValue();
      properties.add(schemaToParamView(context, param, symbolTable));
      if (param.required()) {
        hasRequiredProperties = true;
      }
    }

    if (method.request() != null) {
      properties.add(
          schemaToParamView(
              context,
              method.request(),
              method.request().dereference().getIdentifier(),
              symbolTable));
    }
    Collections.sort(properties);

    requestView.canRepeat(false);
    requestView.isRequired(true);
    requestView.properties(properties);
    requestView.hasRequiredProperties(hasRequiredProperties);
    requestView.isRequestMessage(true);

    return requestView.build();
  }

  // Transforms a request/response Schema object into a StaticLangApiMessageView.
  private StaticLangApiMessageView schemaToParamView(
      SchemaTransformationContext context, Schema schema, SymbolTable symbolTable) {
    return schemaToParamView(context, schema, schema.getIdentifier(), symbolTable);
  }

  // Transforms a request/response Schema object into a StaticLangApiMessageView.
  private StaticLangApiMessageView schemaToParamView(
      SchemaTransformationContext context,
      Schema schema,
      String preferredName,
      SymbolTable symbolTable) {
    StaticLangApiMessageView.Builder paramView = StaticLangApiMessageView.newBuilder();
    String typeName = context.getSchemaTypeTable().getAndSaveNicknameFor(schema);
    paramView.description(schema.description());
    String name = context.getNamer().privateFieldName(Name.anyCamel(preferredName));
    paramView.name(symbolTable.getNewSymbol(name));
    paramView.typeName(typeName);
    paramView.innerTypeName(typeName);
    paramView.isRequired(schema.required());
    paramView.canRepeat(schema.repeated());
    paramView.fieldGetFunction(context.getDiscoGapicNamer().getResourceGetterName(name));
    paramView.fieldSetFunction(context.getDiscoGapicNamer().getResourceSetterName(name));
    paramView.properties(new LinkedList<StaticLangApiMessageView>());
    paramView.isRequestMessage(false);
    paramView.hasRequiredProperties(false);
    return paramView.build();
  }

  private void addApiImports(ImportTypeTable typeTable) {
    typeTable.getAndSaveNicknameFor("com.google.api.core.BetaApi");
    typeTable.getAndSaveNicknameFor("com.google.common.collect.ImmutableList");
    typeTable.getAndSaveNicknameFor("com.google.api.gax.httpjson.ApiMessage");
    typeTable.getAndSaveNicknameFor("java.io.Serializable");
    typeTable.getAndSaveNicknameFor("java.util.Collections");
    typeTable.getAndSaveNicknameFor("java.util.List");
    typeTable.getAndSaveNicknameFor("java.util.HashMap");
    typeTable.getAndSaveNicknameFor("java.util.Map");
    typeTable.getAndSaveNicknameFor("java.util.Objects");
    typeTable.getAndSaveNicknameFor("java.util.Set");
    typeTable.getAndSaveNicknameFor("javax.annotation.Generated");
  }

  private SchemaTypeTable createTypeTable(String implicitPackageName) {
    return new SchemaTypeTable(
        new JavaTypeTable(implicitPackageName, IGNORE_JAVA_LANG_CLASH),
        new JavaSchemaTypeNameConverter(implicitPackageName, nameFormatter));
  }
}
