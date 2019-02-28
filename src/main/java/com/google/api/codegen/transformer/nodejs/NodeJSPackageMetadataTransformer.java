/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.VersionBound;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.ReadmeMetadataView;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Responsible for producing package metadata related views for NodeJS */
public class NodeJSPackageMetadataTransformer implements ModelToViewTransformer<ProtoApiModel> {
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
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    NodeJSPackageMetadataNamer namer =
        new NodeJSPackageMetadataNamer(
            productConfig.getPackageName(), productConfig.getDomainLayerLocation());
    models.addAll(generateMetadataViews(model, productConfig, namer));
    models.add(generateReadmeView(model, productConfig, namer));
    return models;
  }

  private ViewModel generateReadmeView(
      ApiModel model, GapicProductConfig productConfig, NodeJSPackageMetadataNamer namer) {
    List<ApiMethodView> exampleMethods = generateExampleMethods(model, productConfig);
    return metadataTransformer
        .generateMetadataView(
            namer, packageConfig, model, README_FILE, README_OUTPUT_FILE, TargetLanguage.NODEJS)
        .identifier(namer.getMetadataIdentifier())
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig,
                ImportSectionView.newBuilder().build(),
                new NodeJSSurfaceNamer(
                    productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig))))
        .hasMultipleServices(model.hasMultipleServices())
        .readmeMetadata(
            ReadmeMetadataView.newBuilder()
                .moduleName("")
                .identifier(namer.getMetadataIdentifier())
                .shortName(packageConfig.shortName())
                .fullName(model.getTitle())
                .apiSummary(model.getDocumentationSummary())
                .hasMultipleServices(model.hasMultipleServices())
                .gapicPackageName("gapic-" + packageConfig.packageName())
                .majorVersion(packageConfig.apiVersion())
                .developmentStatusTitle(
                    namer.getReleaseAnnotation(
                        metadataTransformer.getMergedReleaseLevel(packageConfig, productConfig)))
                .targetLanguage("Node.js")
                .mainReadmeLink(GITHUB_REPO_HOST + MAIN_README_PATH)
                .libraryDocumentationLink(
                    GITHUB_DOC_HOST + String.format(LIB_DOC_PATH, packageConfig.shortName()))
                .authDocumentationLink(GITHUB_DOC_HOST + AUTH_DOC_PATH)
                .versioningDocumentationLink(GITHUB_REPO_HOST + VERSIONING_DOC_PATH)
                .exampleMethods(exampleMethods)
                .build())
        .build();
  }

  // Generates methods used as examples for the README.md file.
  // Note: This is based on sample gen method calls. In the future, the example
  // methods may be configured separately.
  private List<ApiMethodView> generateExampleMethods(
      ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ApiMethodView> exampleMethods = ImmutableList.builder();
    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      if (interfaceConfig == null || interfaceConfig.getSmokeTestConfig() == null) {
        continue;
      }

      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      MethodModel method = interfaceConfig.getSmokeTestConfig().getMethod();
      FlatteningConfig flatteningGroup =
          testCaseTransformer.getSmokeTestFlatteningGroup(
              context.getMethodConfig(method), interfaceConfig.getSmokeTestConfig());
      GapicMethodContext defaultMethodContext = context.asDynamicMethodContext(method);
      GapicMethodContext flattenedMethodContext =
          context.asFlattenedMethodContext(defaultMethodContext, flatteningGroup);
      exampleMethods.add(
          createExampleApiMethodView(flattenedMethodContext, model.hasMultipleServices()));
    }
    return exampleMethods.build();
  }

  private OptionalArrayMethodView createExampleApiMethodView(
      GapicMethodContext context, boolean packageHasMultipleServices) {
    OptionalArrayMethodView apiMethodView =
        new NodeJSMethodViewGenerator(
                new DynamicLangApiMethodTransformer(new NodeJSApiMethodParamTransformer()))
            .generateOneApiMethod(
                context,
                testCaseTransformer.createSmokeTestInitContext(context),
                packageHasMultipleServices);
    return apiMethodView;
  }

  private List<ViewModel> generateMetadataViews(
      ApiModel model, GapicProductConfig productConfig, NodeJSPackageMetadataNamer namer) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (String template : TOP_LEVEL_FILES) {
      views.add(generateMetadataView(model, productConfig, template, namer));
    }
    return views.build();
  }

  private ViewModel generateMetadataView(
      ApiModel model,
      GapicProductConfig productConfig,
      String template,
      NodeJSPackageMetadataNamer namer) {
    String noLeadingNodeDir =
        template.startsWith(NODE_PREFIX) ? template.substring(NODE_PREFIX.length()) : template;
    int extensionIndex = noLeadingNodeDir.lastIndexOf(".");
    String outputPath = noLeadingNodeDir.substring(0, extensionIndex);

    return metadataTransformer
        .generateMetadataView(
            namer, packageConfig, model, template, outputPath, TargetLanguage.NODEJS)
        .identifier(namer.getMetadataIdentifier())
        .hasMultipleServices(model.hasMultipleServices())
        .additionalDependencies(generateAdditionalDependencies(model, productConfig))
        .build();
  }

  private List<PackageDependencyView> generateAdditionalDependencies(
      ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<PackageDependencyView> dependencies = ImmutableList.builder();
    dependencies.add(
        PackageDependencyView.create(
            "google-gax", packageConfig.gaxVersionBound(TargetLanguage.NODEJS)));
    if (model.hasMultipleServices()) {
      dependencies.add(
          PackageDependencyView.create("lodash.union", VersionBound.create("4.6.0", "")));
    }
    dependencies.add(
        PackageDependencyView.create("lodash.merge", VersionBound.create("4.6.0", "")));
    if (hasLongrunning(model, productConfig) || hasBatching(model, productConfig)) {
      dependencies.add(
          PackageDependencyView.create("protobufjs", VersionBound.create("6.8.0", "")));
    }
    return dependencies.build();
  }

  private boolean hasLongrunning(ApiModel model, GapicProductConfig productConfig) {
    return model
        .getInterfaces(productConfig)
        .stream()
        .map(productConfig::getInterfaceConfig)
        .filter(Objects::nonNull)
        .anyMatch(InterfaceConfig::hasLongRunningOperations);
  }

  private boolean hasBatching(ApiModel model, GapicProductConfig productConfig) {
    return model
        .getInterfaces(productConfig)
        .stream()
        .map(productConfig::getInterfaceConfig)
        .filter(Objects::nonNull)
        .anyMatch(InterfaceConfig::hasBatchingMethods);
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
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
