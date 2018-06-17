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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.util.testing.StandardValueProducer;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.ReadmeMetadataView;
import com.google.api.codegen.viewmodel.metadata.TocContentView;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/** Responsible for producing package metadata related views for Ruby */
public class RubyPackageMetadataTransformer implements ModelToViewTransformer<ProtoApiModel> {
  private static final String GEMSPEC_FILE = "ruby/gemspec.snip";
  private static final String README_FILE = "ruby/README.md.snip";
  private static final String README_OUTPUT_FILE = "README.md";
  private static final List<String> TOP_LEVEL_FILES =
      ImmutableList.of("ruby/Gemfile.snip", "ruby/Rakefile.snip", "LICENSE.snip");
  private static final List<String> TOP_LEVEL_DOT_FILES =
      ImmutableList.of("ruby/gitignore.snip", "ruby/rubocop.yml.snip", "ruby/yardopts.snip");

  private static final String GITHUB_DOC_HOST =
      "https://googlecloudplatform.github.io/google-cloud-ruby";
  private static final String GITHUB_REPO_HOST =
      "https://github.com/GoogleCloudPlatform/google-cloud-ruby";
  private static final String AUTH_DOC_PATH = "/#/docs/google-cloud/master/guides/authentication";
  private static final String LIB_DOC_PATH = "/#/docs/%s/latest/%s";
  private static final String MAIN_README_PATH = "/blob/master/README.md";
  private static final String VERSIONING_DOC_PATH = "#versioning";

  private final RubyImportSectionTransformer importSectionTransformer =
      new RubyImportSectionTransformer();

  // These dependencies are pulled in transitively through GAX and should not be declared
  // explicitly to reduce the chance of version conflicts
  private static final Set<String> BLACKLISTED_DEPENDENCIES =
      ImmutableSet.of("googleapis-common-protos");

  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final PackageMetadataConfig packageConfig;
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();
  private final ValueProducer valueProducer = new StandardValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  private static final String RUBY_PREFIX = "ruby/";

  public RubyPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.<String>builder()
        .add(GEMSPEC_FILE)
        .add(README_FILE)
        .addAll(TOP_LEVEL_FILES)
        .addAll(TOP_LEVEL_DOT_FILES)
        .build();
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(productConfig.getPackageName());
    return ImmutableList.<ViewModel>builder()
        .add(generateGemspecView(model, namer))
        .add(generateReadmeView(model, productConfig, namer))
        .addAll(generateMetadataViews(model, productConfig, namer, TOP_LEVEL_FILES))
        .addAll(generateMetadataViews(model, productConfig, namer, TOP_LEVEL_DOT_FILES, "."))
        .build();
  }

  public ReadmeMetadataView.Builder generateReadmeMetadataView(
      ApiModel model, GapicProductConfig productConfig, RubyPackageMetadataNamer namer) {
    return ReadmeMetadataView.newBuilder()
        .identifier(namer.getMetadataIdentifier())
        .shortName(packageConfig.shortName())
        .fullName(model.getTitle())
        .apiSummary(model.getDocumentationSummary())
        .gapicPackageName("gapic-" + packageConfig.packageName())
        .majorVersion(packageConfig.apiVersion())
        .hasMultipleServices(false)
        .developmentStatusTitle(
            namer.getReleaseAnnotation(
                metadataTransformer.getMergedReleaseLevel(packageConfig, productConfig)))
        .targetLanguage("Ruby")
        .mainReadmeLink(GITHUB_REPO_HOST + MAIN_README_PATH)
        .libraryDocumentationLink("")
        .authDocumentationLink(GITHUB_DOC_HOST + AUTH_DOC_PATH)
        .versioningDocumentationLink(GITHUB_REPO_HOST + VERSIONING_DOC_PATH)
        .exampleMethods(generateExampleMethods(model, productConfig));
  }

  public TocContentView generateTocContent(
      String description,
      RubyPackageMetadataNamer namer,
      String packageFilePath,
      String clientName) {
    return TocContentView.newBuilder()
        .name(clientName)
        .description(description)
        .link(
            GITHUB_DOC_HOST
                + String.format(LIB_DOC_PATH, namer.getMetadataIdentifier(), packageFilePath)
                + '/'
                + clientName.replace(" ", "").toLowerCase())
        .build();
  }

  private ViewModel generateGemspecView(ApiModel model, RubyPackageMetadataNamer namer) {
    // Whitelist is just the complement of the blacklist
    Set<String> whitelist =
        packageConfig
            .protoPackageDependencies(TargetLanguage.RUBY)
            .entrySet()
            .stream()
            .map(v -> v.getKey())
            .filter(s -> !BLACKLISTED_DEPENDENCIES.contains(s))
            .collect(Collectors.toSet());
    return metadataTransformer
        .generateMetadataView(
            namer,
            packageConfig,
            model,
            GEMSPEC_FILE,
            namer.getOutputFileName(),
            TargetLanguage.RUBY,
            whitelist)
        .identifier(namer.getMetadataIdentifier())
        .additionalDependencies(
            ImmutableList.of(
                PackageDependencyView.create(
                    "google-gax", packageConfig.gaxVersionBound(TargetLanguage.RUBY))))
        .build();
  }

  private ViewModel generateReadmeView(
      ApiModel model, GapicProductConfig productConfig, RubyPackageMetadataNamer namer) {
    return metadataTransformer
        .generateMetadataView(
            namer, packageConfig, model, README_FILE, README_OUTPUT_FILE, TargetLanguage.RUBY)
        .identifier(namer.getMetadataIdentifier())
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig,
                ImportSectionView.newBuilder().build(),
                new RubySurfaceNamer(productConfig.getPackageName())))
        .readmeMetadata(
            generateReadmeMetadataView(model, productConfig, namer)
                .moduleName("")
                .libraryDocumentationLink(
                    GITHUB_DOC_HOST
                        + String.format(
                            LIB_DOC_PATH, namer.getMetadataIdentifier(), packageConfig.protoPath()))
                .build())
        .build();
  }

  // Generates methods used as examples for the README.md file.
  // This currently generates a list of methods that have smoke test configuration. In the future,
  //  the example methods may be configured separately.
  private List<ApiMethodView> generateExampleMethods(
      ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ApiMethodView> exampleMethods = ImmutableList.builder();
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        MethodModel method = context.getInterfaceConfig().getSmokeTestConfig().getMethod();
        FlatteningConfig flatteningGroup =
            testCaseTransformer.getSmokeTestFlatteningGroup(
                context.getMethodConfig(method), context.getInterfaceConfig().getSmokeTestConfig());
        GapicMethodContext flattenedMethodContext =
            context.asFlattenedMethodContext(method, flatteningGroup);
        exampleMethods.add(
            createExampleApiMethodView(flattenedMethodContext, model.hasMultipleServices()));
      }
    }
    return exampleMethods.build();
  }

  private OptionalArrayMethodView createExampleApiMethodView(
      GapicMethodContext context, boolean packageHasMultipleServices) {
    OptionalArrayMethodView initialApiMethodView =
        new DynamicLangApiMethodTransformer(new RubyApiMethodParamTransformer())
            .generateMethod(context, packageHasMultipleServices);

    OptionalArrayMethodView.Builder apiMethodView = initialApiMethodView.toBuilder();

    InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
    InitCodeView initCodeView =
        initCodeTransformer.generateInitCode(
            context, testCaseTransformer.createSmokeTestInitContext(context));
    apiMethodView.initCode(initCodeView);

    return apiMethodView.build();
  }

  private List<ViewModel> generateMetadataViews(
      ApiModel model,
      GapicProductConfig productConfig,
      RubyPackageMetadataNamer namer,
      List<String> snippets) {
    return generateMetadataViews(model, productConfig, namer, snippets, null);
  }

  private List<ViewModel> generateMetadataViews(
      ApiModel model,
      GapicProductConfig productConfig,
      RubyPackageMetadataNamer namer,
      List<String> snippets,
      String filePrefix) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    for (String template : snippets) {
      views.add(generateMetadataView(model, productConfig, template, namer, filePrefix));
    }
    return views.build();
  }

  private ViewModel generateMetadataView(
      ApiModel model,
      GapicProductConfig productConfig,
      String template,
      RubyPackageMetadataNamer namer,
      String filePrefix) {
    String noLeadingRubyDir =
        template.startsWith(RUBY_PREFIX) ? template.substring(RUBY_PREFIX.length()) : template;
    if (!Strings.isNullOrEmpty(filePrefix)) {
      noLeadingRubyDir = filePrefix + noLeadingRubyDir;
    }
    int extensionIndex = noLeadingRubyDir.lastIndexOf(".");
    String outputPath = noLeadingRubyDir.substring(0, extensionIndex);

    boolean hasSmokeTests = false;
    List<InterfaceModel> interfaceModels = new LinkedList<>();
    List<GapicInterfaceContext> contexts = new LinkedList<>();
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      interfaceModels.add(context.getInterfaceModel());
      contexts.add(context);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        hasSmokeTests = true;
      }
    }

    SurfaceNamer surfaceNamer = new RubySurfaceNamer(productConfig.getPackageName());
    ImportSectionView importSection =
        importSectionTransformer.generateRakefileAcceptanceTaskImportSection(contexts);

    return metadataTransformer
        .generateMetadataView(
            namer, packageConfig, model, template, outputPath, TargetLanguage.RUBY)
        .identifier(namer.getMetadataIdentifier())
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(productConfig, importSection, surfaceNamer))
        .hasSmokeTests(hasSmokeTests)
        .versionPath(surfaceNamer.getVersionIndexFileImportName())
        .versionNamespace(validVersionNamespace(interfaceModels, surfaceNamer))
        .credentialsClassName(surfaceNamer.getFullyQualifiedCredentialsClassName())
        .smokeTestProjectVariable(namer.getProjectVariable(true))
        .smokeTestKeyfileVariable(namer.getKeyfileVariable(true))
        .smokeTestJsonKeyVariable(namer.getJsonKeyVariable(true))
        .projectVariable(namer.getProjectVariable(false))
        .keyfileVariable(namer.getKeyfileVariable(false))
        .jsonKeyVariable(namer.getJsonKeyVariable(false))
        .build();
  }

  private String validVersionNamespace(Iterable<InterfaceModel> interfaces, SurfaceNamer namer) {
    Set<String> versionNamespaces = new HashSet<>();
    for (InterfaceModel apiInterface : interfaces) {
      versionNamespaces.add(namer.getNamespace(apiInterface));
    }
    if (versionNamespaces.size() > 1) {
      throw new IllegalArgumentException("Multiple versionNamespaces found for the package.");
    }
    return versionNamespaces.iterator().next();
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new RubyTypeTable(productConfig.getPackageName()),
            new RubyModelTypeNameConverter(productConfig.getPackageName())),
        new RubySurfaceNamer(productConfig.getPackageName()),
        new RubyFeatureConfig());
  }
}
