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
import com.google.api.codegen.discogapic.RequestInterfaceContext;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.SchemaTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.java.JavaFeatureConfig;
import com.google.api.codegen.transformer.java.JavaSchemaTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.TypeTable;
import com.google.api.codegen.util.java.JavaNameFormatter;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.SimpleParamView;
import com.google.api.codegen.viewmodel.StaticLangApiHttpRequestFileView;
import com.google.api.codegen.viewmodel.StaticLangApiHttpRequestView;
import com.google.api.codegen.viewmodel.StaticMemberView;
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
import org.apache.commons.lang3.StringUtils;

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
    queryParams.put("access_token", "String");
    queryParams.put("callback", "String");
    queryParams.put("fields", "String");
    queryParams.put("key", "String");
    queryParams.put("prettyPrint", "String");
    queryParams.put("quotaUser", "String");
    queryParams.put("userIp", "String");
    STANDARD_QUERY_PARAMS = queryParams.build();
  }

  static {
    reservedKeywords.addAll(JavaNameFormatter.RESERVED_IDENTIFIER_SET);
    reservedKeywords.add("Builder");
  }

  private static final String XAPI_TEMPLATE_FILENAME = "java/main.snip";
  private static final String PACKAGE_INFO_TEMPLATE_FILENAME = "java/package-info.snip";
  private static final String REQUEST_TEMPLATE_FILENAME = "java/http_request.snip";

  public JavaDiscoGapicRequestToViewTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageMetadataConfig;
    // TODO use packageMetadataConfig
  }

  public List<String> getTemplateFileNames() {
    return Arrays.asList(
        XAPI_TEMPLATE_FILENAME, PACKAGE_INFO_TEMPLATE_FILENAME, REQUEST_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Document document, GapicProductConfig productConfig) {
    List<ViewModel> surfaceRequests = new ArrayList<>();
    JavaDiscoGapicNamer discoGapicNamer = new JavaDiscoGapicNamer();

    DiscoGapicInterfaceContext context =
        DiscoGapicInterfaceContext.create(
            document,
            productConfig,
            createTypeTable(productConfig.getPackageName()),
            discoGapicNamer,
            new JavaSurfaceNamer(productConfig.getPackageName()),
            JavaFeatureConfig.newBuilder().enableStringFormatFunctions(false).build());

    // Escape any request names that are Java keywords.

    for (Method method : context.getDocument().methods()) {
      RequestInterfaceContext requestContext =
          RequestInterfaceContext.create(context, method, context.getSchemaTypeTable());
      StaticLangApiHttpRequestView requestView = generateRequestClass(requestContext, method);
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
  private StaticLangApiHttpRequestFileView generateRequestFile(
      RequestInterfaceContext context, StaticLangApiHttpRequestView messageView) {
    StaticLangApiHttpRequestFileView.Builder apiFile =
        StaticLangApiHttpRequestFileView.newBuilder();
    apiFile.templateFileName(REQUEST_TEMPLATE_FILENAME);
    addApiImports(context.getTypeTable());
    apiFile.request(messageView);

    String outputPath = pathMapper.getOutputPath(null, context.getDocContext().getProductConfig());
    apiFile.outputPath(outputPath + File.separator + messageView.typeName() + ".java");

    // must be done as the last step to catch all imports
    apiFile.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return apiFile.build();
  }

  private StaticLangApiHttpRequestView generateRequestClass(
      RequestInterfaceContext context, Method method) {
    StaticLangApiHttpRequestView.Builder requestView = StaticLangApiHttpRequestView.newBuilder();

    SymbolTable symbolTable = SymbolTable.fromSeed(reservedKeywords);

    String requestClassId = context.getDiscoGapicNamer().getRequestName(method.id());
    String requestName =
        nameFormatter.privateFieldName(Name.anyCamel(symbolTable.getNewSymbol(requestClassId)));

    requestView.name(requestName);
    requestView.description(method.description());

    //    String requestTypeName = typeTable.getAndSaveNicknameForElementType(method.id());
    String requestTypeName = nameFormatter.publicClassName(Name.anyCamel(requestClassId));
    requestView.typeName(requestTypeName);

    List<SimpleParamView> paramViews = new LinkedList<>();
    for (String str : method.parameterOrder()) {
      Schema param = method.parameters().get(str);
      paramViews.add(schemaToParamView(context, param, symbolTable));
    }
    requestView.queryParams(paramViews);

    if (method.request() != null) {
      requestView.requestObject(schemaToParamView(context, method.request(), symbolTable));
    }
    if (method.response() != null) {
      requestView.responseObject(schemaToParamView(context, method.response(), symbolTable));
    }

    List<StaticMemberView> staticFinalMembers = new LinkedList<>();
    StringBuilder scopesString = new StringBuilder("ImmutableList.<String>of(\"");
    scopesString.append(StringUtils.join(method.scopes().iterator(), "\", \""));
    scopesString.append("\")");
    staticFinalMembers.add(
        StaticMemberView.newBuilder()
            .description("Available OAuth 2.0 scopes.")
            .name("scopes")
            .typeName("List<String>")
            .value(scopesString.toString())
            .build());
    staticFinalMembers.add(
        StaticMemberView.newBuilder()
            .description("HTTP method used by this method.")
            .name("httpMethod")
            .typeName("String")
            .value(String.format("\"%s\"", method.httpMethod()))
            .build());
    staticFinalMembers.add(
        StaticMemberView.newBuilder()
            .description("The URI path of this REST method.")
            .name("path")
            .typeName("String")
            .value(String.format("\"%s\"", method.path()))
            .build());
    staticFinalMembers.add(
        StaticMemberView.newBuilder()
            .description("")
            .name("DEFAULT_INSTANCE")
            .typeName(requestTypeName)
            .value(String.format("new %s()", requestTypeName))
            .build());
    requestView.staticFinalMembers(staticFinalMembers);

    return requestView.build();
  }

  // Transforms a request/response Schema object into a SimpleParamView.
  private SimpleParamView schemaToParamView(
      RequestInterfaceContext context, Schema schema, SymbolTable symbolTable) {
    SimpleParamView.Builder paramView = SimpleParamView.newBuilder();
    paramView.description(schema.description());
    paramView.name(symbolTable.getNewSymbol(schema.getIdentifier()));
    paramView.typeName(context.getSchemaTypeTable().getAndSaveNicknameFor(schema));
    paramView.isRequired(schema.required());
    paramView.getterFunction(
        context.getDiscoGapicNamer().getResourceGetterName(schema.getIdentifier()));
    paramView.setterFunction(
        context.getDiscoGapicNamer().getResourceSetterName(schema.getIdentifier()));
    return paramView.build();
  }

  private void addApiImports(TypeTable typeTable) {
    typeTable.getAndSaveNicknameFor("com.google.api.core.BetaApi");
    typeTable.getAndSaveNicknameFor("com.google.common.collect.ImmutableList");
    typeTable.getAndSaveNicknameFor("com.google.common.collect.ImmutableMap");
    typeTable.getAndSaveNicknameFor("java.io.Serializable");
    typeTable.getAndSaveNicknameFor("java.util.AbstractMap");
    typeTable.getAndSaveNicknameFor("java.util.LinkedList");
    typeTable.getAndSaveNicknameFor("java.util.List");
    typeTable.getAndSaveNicknameFor("java.util.Map");
    typeTable.getAndSaveNicknameFor("javax.annotation.Generated");
  }

  private SchemaTypeTable createTypeTable(String implicitPackageName) {
    return new SchemaTypeTable(
        new JavaTypeTable(implicitPackageName, IGNORE_JAVA_LANG_CLASH),
        new JavaSchemaTypeNameConverter(implicitPackageName, nameFormatter));
  }
}
