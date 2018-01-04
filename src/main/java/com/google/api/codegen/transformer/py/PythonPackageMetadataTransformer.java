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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.VersionBound;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TestCaseTransformer;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.util.testing.PythonValueProducer;
import com.google.api.codegen.util.testing.ValueProducer;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.SimpleViewModel;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.codegen.viewmodel.metadata.ReadmeMetadataView;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Responsible for producing package metadata related views for Python */
public class PythonPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String GITHUB_DOC_HOST =
      "https://googlecloudplatform.github.io/google-cloud-python/stable";
  private static final String GITHUB_REPO_HOST =
      "https://github.com/GoogleCloudPlatform/google-cloud-python";
  private static final String AUTH_DOC_PATH = "/core/auth.html";
  private static final String LIB_DOC_PATH = "/%s/usage.html";
  private static final String MAIN_README_PATH = "/blob/master/README.rst";

  private static final Map<String, String> TOP_LEVEL_TEMPLATE_FILES =
      ImmutableMap.<String, String>builder()
          .put("LICENSE.snip", "LICENSE")
          .put("py/MANIFEST.in.snip", "MANIFEST.in")
          .put("py/setup.py.snip", "setup.py")
          .put("py/setup_cfg.snip", "setup.cfg")
          .put("py/README.rst.snip", "README.rst")
          .put("py/docs/conf.py.snip", "docs/conf.py")
          .put("py/docs/index.rst.snip", "docs/index.rst")
          .build();
  private static final String INIT_TEMPLATE_FILE = "py/__init__.py.snip";
  private static final String NAMESPACE_INIT_TEMPLATE_FILE = "py/namespace__init__.py.snip";
  private static final String API_DOC_TEMPLATE_FILE = "py/docs/api.rst.snip";
  private static final String TYPES_DOC_TEMPLATE_FILE = "py/docs/types.rst.snip";
  private static final String NOX_TEMPLATE_FILE = "py/nox.py.snip";

  private static final Set<String> GOOGLE_CLOUD_NAMESPACE_PACKAGES =
      ImmutableSet.of("google", "google.cloud");

  private final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);

  private final PackageMetadataConfig packageConfig;
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();
  private final ValueProducer valueProducer = new PythonValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  public PythonPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<ViewModel> transform(final ApiModel model, final GapicProductConfig productConfig) {
    SurfaceNamer surfaceNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    return ImmutableList.<ViewModel>builder()
        .addAll(computeInitFiles(computePackages(productConfig.getPackageName()), surfaceNamer))
        .addAll(generateTopLevelFiles(model, productConfig))
        .addAll(generateDocFiles(model, productConfig))
        .add(generateNoxFile(model, productConfig))
        .build();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> templates = new ArrayList<>();
    templates.addAll(TOP_LEVEL_TEMPLATE_FILES.keySet());
    templates.add(INIT_TEMPLATE_FILE);
    templates.add(NAMESPACE_INIT_TEMPLATE_FILE);
    templates.add(API_DOC_TEMPLATE_FILE);
    templates.add(TYPES_DOC_TEMPLATE_FILE);
    templates.add(NOX_TEMPLATE_FILE);
    return templates;
  }

  private List<ViewModel> generateDocFiles(ApiModel model, GapicProductConfig productConfig) {
    PackageMetadataNamer namer = new PackageMetadataNamer();
    SurfaceNamer surfaceNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    String docsGapicPath = docsGapicPath(packageConfig.apiVersion());
    String typesOutputPath = String.format("docs/%s/types.rst", docsGapicPath);
    String apiOutputPath = String.format("docs/%s/api.rst", docsGapicPath);
    return ImmutableList.<ViewModel>builder()
        .add(
            generateMetadataView(
                    model,
                    productConfig,
                    TYPES_DOC_TEMPLATE_FILE,
                    namer,
                    surfaceNamer,
                    typesOutputPath)
                .build())
        .add(
            generateMetadataView(
                    model, productConfig, API_DOC_TEMPLATE_FILE, namer, surfaceNamer, apiOutputPath)
                .build())
        .build();
  }

  private ViewModel generateNoxFile(ApiModel model, GapicProductConfig productConfig) {
    PackageMetadataNamer namer = new PackageMetadataNamer();
    SurfaceNamer surfaceNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    String outputPath = "nox.py";
    return generateMetadataView(
            model, productConfig, NOX_TEMPLATE_FILE, namer, surfaceNamer, outputPath)
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig,
                importSectionTransformer.generateNoxImportSection(),
                new PythonSurfaceNamer(productConfig.getPackageName())))
        .build();
  }

  private List<ViewModel> generateTopLevelFiles(ApiModel model, GapicProductConfig productConfig) {
    PackageMetadataNamer namer = new PackageMetadataNamer();
    SurfaceNamer surfaceNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    ImmutableList.Builder<ViewModel> metadata = ImmutableList.builder();
    for (Map.Entry<String, String> entry : TOP_LEVEL_TEMPLATE_FILES.entrySet()) {
      metadata.add(
          generateMetadataView(
                  model, productConfig, entry.getKey(), namer, surfaceNamer, entry.getValue())
              .build());
    }
    return metadata.build();
  }

  private PackageMetadataView.Builder generateMetadataView(
      ApiModel model,
      GapicProductConfig productConfig,
      String template,
      PackageMetadataNamer metadataNamer,
      SurfaceNamer surfaceNamer,
      String outputPath) {
    List<ApiMethodView> exampleMethods = generateExampleMethods(model, productConfig);
    String gapicPackageName =
        surfaceNamer.getGapicPackageName(packageConfig.packageName(TargetLanguage.PYTHON));
    return metadataTransformer
        .generateMetadataView(
            metadataNamer, packageConfig, model, template, outputPath, TargetLanguage.PYTHON)
        .namespacePackages(computeNamespacePackages(productConfig.getPackageName()))
        .developmentStatus(
            surfaceNamer.getReleaseAnnotation(packageConfig.releaseLevel(TargetLanguage.PYTHON)))
        .clientModules(clientModules(surfaceNamer))
        .apiModules(apiModules(packageConfig.apiVersion()))
        .typeModules(typesModules(surfaceNamer))
        .gapicPackageName(gapicPackageName)
        .protoPackageDependencies(generateProtoPackageDependencies())
        .additionalDependencies(generateAdditionalDependencies())
        .hasSmokeTests(hasSmokeTests(model, productConfig))
        .licenseName(packageConfig.licenseName().replace("-", " "))
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig,
                ImportSectionView.newBuilder().build(),
                new PythonSurfaceNamer(productConfig.getPackageName())))
        .readmeMetadata(
            ReadmeMetadataView.newBuilder()
                .moduleName("")
                .shortName(packageConfig.shortName())
                .fullName(model.getTitle())
                .apiSummary(model.getDocumentationSummary())
                .hasMultipleServices(false)
                .gapicPackageName(gapicPackageName)
                .majorVersion(packageConfig.apiVersion())
                .developmentStatusTitle(
                    metadataNamer.getReleaseAnnotation(
                        packageConfig.releaseLevel(TargetLanguage.PYTHON)))
                .targetLanguage("Python")
                .mainReadmeLink(GITHUB_REPO_HOST + MAIN_README_PATH)
                .libraryDocumentationLink(
                    GITHUB_DOC_HOST + String.format(LIB_DOC_PATH, packageConfig.shortName()))
                .authDocumentationLink(GITHUB_DOC_HOST + AUTH_DOC_PATH)
                .versioningDocumentationLink(GITHUB_REPO_HOST + MAIN_README_PATH)
                .exampleMethods(exampleMethods)
                .build());
  }

  private boolean hasSmokeTests(ApiModel apiModel, GapicProductConfig productConfig) {
    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
      InterfaceContext context = createContext(apiInterface, productConfig);
      if (context.getInterfaceConfig().getSmokeTestConfig() != null) {
        return true;
      }
    }
    return false;
  }

  private List<PackageDependencyView> generateProtoPackageDependencies() {
    Map<String, VersionBound> dependencies =
        packageConfig.protoPackageDependencies(TargetLanguage.PYTHON);
    if (dependencies == null) {
      return ImmutableList.of();
    }
    List<PackageDependencyView> protoPackageDependencies = new ArrayList<>();
    for (Map.Entry<String, VersionBound> entry : dependencies.entrySet()) {
      if (entry.getValue() == null || entry.getKey().startsWith("proto-google-cloud-")) {
        break;
      }
      String dependencyName = entry.getKey();
      if ("googleapis-common-protos".equals(dependencyName)) {
        dependencyName = String.format("%s[grpc]", dependencyName);
      }
      protoPackageDependencies.add(PackageDependencyView.create(dependencyName, entry.getValue()));
    }
    // Ensures deterministic test results.
    Collections.sort(protoPackageDependencies);
    return protoPackageDependencies;
  }

  private List<PackageDependencyView> generateAdditionalDependencies() {
    ImmutableList.Builder<PackageDependencyView> dependencies = ImmutableList.builder();
    dependencies.add(
        PackageDependencyView.create("google-api-core", VersionBound.create("0.1.0", "0.2.0dev")));
    dependencies.add(
        PackageDependencyView.create(
            "google-auth", packageConfig.authVersionBound(TargetLanguage.PYTHON)));
    dependencies.add(
        PackageDependencyView.create("requests", VersionBound.create("2.18.4", "3.0dev")));
    return dependencies.build();
  }

  private List<String> clientModules(SurfaceNamer surfaceNamer) {
    return ImmutableList.of(surfaceNamer.getVersionedDirectoryNamespace());
  }

  private List<String> typesModules(SurfaceNamer surfaceNamer) {
    return ImmutableList.of(
        String.format("%s.types", surfaceNamer.getVersionedDirectoryNamespace()));
  }

  private List<String> apiModules(String apiVersion) {
    String docsGapicPath = docsGapicPath(apiVersion);
    return ImmutableList.of(
        String.format("%s/api", docsGapicPath), String.format("%s/types", docsGapicPath));
  }

  private String docsGapicPath(String apiVersion) {
    return String.format("gapic/%s", apiVersion.toLowerCase());
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
        exampleMethods.add(createExampleApiMethodView(flattenedMethodContext));
      }
    }
    return exampleMethods.build();
  }

  private OptionalArrayMethodView createExampleApiMethodView(GapicMethodContext context) {
    DynamicLangApiMethodTransformer apiMethodTransformer =
        new DynamicLangApiMethodTransformer(
            new PythonApiMethodParamTransformer(),
            new InitCodeTransformer(new PythonImportSectionTransformer()));

    return apiMethodTransformer.generateMethod(context);
  }

  /**
   * Computes all Python packages present under the given package name. For example, for input
   * "foo.bar.baz", returns ["foo", "foo.bar", "foo.bar.baz"].
   */
  private List<String> computePackages(String packageName) {
    List<String> packages = new ArrayList<>();
    List<String> parts = Lists.newArrayList(Splitter.on(".").split(packageName));
    for (int i = 0; i < parts.size(); i++) {
      packages.add(Joiner.on(".").join(parts.subList(0, i + 1)));
    }
    return packages;
  }

  private List<String> computeNamespacePackages(String packageName) {
    List<String> namespacePackages = new ArrayList<>();
    for (String subPackage : computePackages(packageName)) {
      if (isNamespacePackage(subPackage)) {
        namespacePackages.add(subPackage);
      }
    }
    return namespacePackages;
  }

  /** Set all packages to be namespace packages except for the version package (if present) */
  private boolean isNamespacePackage(String packageName) {
    // TODO: Provide a way for a library producer to manually specific the namespace packages.
    return GOOGLE_CLOUD_NAMESPACE_PACKAGES.contains(packageName);
  }

  /**
   * Determines which __init__.py files to generate given a list of Python packages. Each Python
   * package corresponds to exactly one __init__.py file, although the contents of that file depend
   * on whether the package is a namespace package.
   */
  private List<ViewModel> computeInitFiles(List<String> packages, SurfaceNamer namer) {
    List<ViewModel> initFiles = new ArrayList<>();
    for (String packageName : packages) {
      final String template;
      if (isNamespacePackage(packageName)) {
        template = NAMESPACE_INIT_TEMPLATE_FILE;
      } else if (isVersionedDirectoryPackage(namer, packageName)) {
        continue;
      } else {
        template = INIT_TEMPLATE_FILE;
      }
      String outputPath =
          Paths.get(packageName.replace(".", File.separator)).resolve("__init__.py").toString();
      initFiles.add(
          SimpleViewModel.create(SnippetSetRunner.SNIPPET_RESOURCE_ROOT, template, outputPath));
    }
    return initFiles;
  }

  private boolean isVersionedDirectoryPackage(SurfaceNamer namer, String packageName) {
    return namer.getVersionedDirectoryNamespace().equals(packageName);
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new PythonTypeTable(productConfig.getPackageName()),
            new PythonModelTypeNameConverter(productConfig.getPackageName())),
        new PythonSurfaceNamer(productConfig.getPackageName()),
        new DefaultFeatureConfig());
  }
}
