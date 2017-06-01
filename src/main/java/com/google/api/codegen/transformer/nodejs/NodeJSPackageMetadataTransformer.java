/* Copyright 2016 Google Inc
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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.List;

/** Responsible for producing package metadata related views for NodeJS */
public class NodeJSPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String README_FILE = "nodejs/README.md.snip";
  private static final String README_OUTPUT_FILE = "README.md";
  private static final List<String> TOP_LEVEL_FILES = ImmutableList.of("nodejs/package.json.snip");

  private static final String GITHUB_DOC_HOST =
      "https://googlecloudplatform.github.io/google-cloud-node";
  private static final String GITHUB_REPO_HOST =
      "https://github.com/GoogleCloudPlatform/google-cloud-node";
  private static final String AUTH_DOC_PATH = "/#/docs/google-cloud/master/guides/authentication";
  private static final String LIB_DOC_PATH = "/#/docs/%s";
  private static final String MAIN_README_PATH = "/blob/master/README.md";
  private static final String VERSIONING_DOC_PATH = "#versioning";

  private static String NODE_PREFIX = "nodejs/";

  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new NodeJSImportSectionTransformer());
  private final PackageMetadataConfig packageConfig;
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  public NodeJSPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.<String>builder().addAll(TOP_LEVEL_FILES).add(README_FILE).build();
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    NodeJSPackageMetadataNamer namer =
        new NodeJSPackageMetadataNamer(
            productConfig.getPackageName(), productConfig.getDomainLayerLocation());
    models.addAll(generateMetadataViews(model, namer));
    models.add(generateReadmeView(model, productConfig, namer));
    return models;
  }

  private ViewModel generateReadmeView(
      Model model, GapicProductConfig productConfig, NodeJSPackageMetadataNamer namer) {
    List<ApiMethodView> exampleMethods = generateExampleMethods(model, productConfig);
    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    boolean hasMultipleServices = Iterables.size(services) > 1;
    return metadataTransformer
        .generateMetadataView(
            packageConfig, model, README_FILE, README_OUTPUT_FILE, TargetLanguage.NODEJS)
        .identifier(namer.getMetadataIdentifier())
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig,
                ImportSectionView.newBuilder().build(),
                new NodeJSSurfaceNamer(
                    productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig))))
        .developmentStatusTitle(
            namer.getReleaseAnnotation(packageConfig.releaseLevel(TargetLanguage.NODEJS)))
        .exampleMethods(exampleMethods)
        .hasMultipleServices(hasMultipleServices)
        .targetLanguage("Node.js")
        .mainReadmeLink(GITHUB_REPO_HOST + MAIN_README_PATH)
        .libraryDocumentationLink(
            GITHUB_DOC_HOST + String.format(LIB_DOC_PATH, packageConfig.shortName()))
        .authDocumentationLink(GITHUB_DOC_HOST + AUTH_DOC_PATH)
        .versioningDocumentationLink(GITHUB_REPO_HOST + VERSIONING_DOC_PATH)
        .build();
  }

  // Generates methods used as examples for the README.md file.
  // Note: This is based on sample gen method calls. In the future, the example
  // methods may be configured separately.
  private List<ApiMethodView> generateExampleMethods(
      Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ApiMethodView> exampleMethods = ImmutableList.builder();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        Method method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
        FlatteningConfig flatteningGroup =
            testCaseTransformer.getSmokeTestFlatteningGroup(
                context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
        GapicMethodContext flattenedMethodContext =
            context.asFlattenedMethodContext(method, flatteningGroup);
        exampleMethods.add(createExampleApiMethodView(flattenedMethodContext));
      }
    }
    return exampleMethods.build();
  }

  private OptionalArrayMethodView createExampleApiMethodView(GapicMethodContext context) {
    OptionalArrayMethodView initialApiMethodView =
        new DynamicLangApiMethodTransformer(new NodeJSApiMethodParamTransformer())
            .generateMethod(context);

    OptionalArrayMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();

    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            context, testCaseTransformer.createSmokeTestInitContext(context));
    apiMethodView.initCode(initCodeView);

    return apiMethodView.build();
  }

  private List<ViewModel> generateMetadataViews(Model model, NodeJSPackageMetadataNamer namer) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (String template : TOP_LEVEL_FILES) {
      views.add(generateMetadataView(model, template, namer));
    }
    return views.build();
  }

  private ViewModel generateMetadataView(
      Model model, String template, NodeJSPackageMetadataNamer namer) {
    String noLeadingNodeDir =
        template.startsWith(NODE_PREFIX) ? template.substring(NODE_PREFIX.length()) : template;
    int extensionIndex = noLeadingNodeDir.lastIndexOf(".");
    String outputPath = noLeadingNodeDir.substring(0, extensionIndex);

    Iterable<Interface> services = new InterfaceView().getElementIterable(model);
    boolean hasMultipleServices = Iterables.size(services) > 1;

    return metadataTransformer
        .generateMetadataView(packageConfig, model, template, outputPath, TargetLanguage.NODEJS)
        .identifier(namer.getMetadataIdentifier())
        .hasMultipleServices(hasMultipleServices)
        .build();
  }

  private GapicInterfaceContext createContext(
      Interface apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new JSTypeTable(productConfig.getPackageName()),
            new NodeJSModelTypeNameConverter(productConfig.getPackageName())),
        new NodeJSSurfaceNamer(productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig)),
        new NodeJSFeatureConfig());
  }
}
