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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.VersionBound;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
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
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.SimpleViewModel;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageDependencyView;
import com.google.api.codegen.viewmodel.metadata.ReadmeMetadataView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Responsible for producing package metadata related views for Python
 *
 * <p>TODO(geigerj): Once Python is on MVVM it will not be necessary to store copies of the other
 * GAPIC providers in this class. The only information that is needed from these providers is the
 * names of the output files they produce. With MVVM should be possible to derive this information
 * from the corresponding transformers/view models without actually rendering the templates.
 */
public class PythonPackageMetadataTransformer implements ModelToViewTransformer {
  private static final String GITHUB_DOC_HOST =
      "https://googlecloudplatform.github.io/google-cloud-python/stable";
  private static final String GITHUB_REPO_HOST =
      "https://github.com/GoogleCloudPlatform/google-cloud-python";
  private static final String AUTH_DOC_PATH = "/google-cloud-auth";
  private static final String LIB_DOC_PATH = "/%s-usage";
  private static final String MAIN_README_PATH = "/blob/master/README.rst";

  private static final List<String> TOP_LEVEL_TEMPLATE_FILES =
      ImmutableList.of(
          "LICENSE.snip",
          "py/MANIFEST.in.snip",
          "py/PUBLISHING.rst.snip",
          "py/setup.py.snip",
          "py/README.rst.snip",
          "py/tox.ini.snip",
          "py/docs/conf.py.snip",
          "py/docs/index.rst.snip");
  private static final String INIT_TEMPLATE_FILE = "py/__init__.py.snip";
  private static final String NAMESPACE_INIT_TEMPLATE_FILE = "py/namespace__init__.py.snip";
  private static final String API_DOC_TEMPLATE_FILE = "py/docs/api.rst.snip";
  private static final String TYPES_DOC_TEMPLATE_FILE = "py/docs/types.rst.snip";

  private final PackageMetadataConfig packageConfig;
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();
  private final ValueProducer valueProducer = new PythonValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);

  public PythonPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<ViewModel> transform(final Model model, final GapicProductConfig productConfig) {
    SurfaceNamer surfaceNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    return ImmutableList.<ViewModel>builder()
        .addAll(computeInitFiles(computePackages(productConfig.getPackageName()), surfaceNamer))
        .addAll(generateTopLevelFiles(model, productConfig))
        .addAll(generateDocFiles(model, productConfig))
        .build();
  }

  private List<ViewModel> generateDocFiles(Model model, GapicProductConfig productConfig) {
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
                typesOutputPath))
        .add(
            generateMetadataView(
                model, productConfig, API_DOC_TEMPLATE_FILE, namer, surfaceNamer, apiOutputPath))
        .build();
  }

  private List<ViewModel> generateTopLevelFiles(Model model, GapicProductConfig productConfig) {
    PackageMetadataNamer namer = new PackageMetadataNamer();
    SurfaceNamer surfaceNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    ImmutableList.Builder<ViewModel> metadata = ImmutableList.builder();
    for (String templateFileName : TOP_LEVEL_TEMPLATE_FILES) {
      metadata.add(
          generateMetadataView(model, productConfig, templateFileName, namer, surfaceNamer));
    }
    return metadata.build();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> templates = new ArrayList<>();
    templates.addAll(TOP_LEVEL_TEMPLATE_FILES);
    templates.add(INIT_TEMPLATE_FILE);
    templates.add(NAMESPACE_INIT_TEMPLATE_FILE);
    templates.add(API_DOC_TEMPLATE_FILE);
    templates.add(TYPES_DOC_TEMPLATE_FILE);
    return templates;
  }

  private ViewModel generateMetadataView(
      Model model,
      GapicProductConfig productConfig,
      String template,
      PackageMetadataNamer metadataNamer,
      SurfaceNamer surfaceNamer) {
    String noLeadingPyDir = template.startsWith("py/") ? template.substring(3) : template;
    int extensionIndex = noLeadingPyDir.lastIndexOf(".");
    String outputPath = noLeadingPyDir.substring(0, extensionIndex);
    return generateMetadataView(
        model, productConfig, template, metadataNamer, surfaceNamer, outputPath);
  }

  private ViewModel generateMetadataView(
      Model model,
      GapicProductConfig productConfig,
      String template,
      PackageMetadataNamer metadataNamer,
      SurfaceNamer surfaceNamer,
      String outputPath) {
    List<ApiMethodView> exampleMethods = generateExampleMethods(model, productConfig);
    return metadataTransformer
        .generateMetadataView(packageConfig, model, template, outputPath, TargetLanguage.PYTHON)
        .namespacePackages(computeNamespacePackages(productConfig.getPackageName(), surfaceNamer))
        .developmentStatus(
            surfaceNamer.getReleaseAnnotation(packageConfig.releaseLevel(TargetLanguage.PYTHON)))
        .clientModules(clientModules(surfaceNamer))
        .apiModules(apiModules(packageConfig.apiVersion()))
        .typeModules(typesModules(surfaceNamer))
        .protoPackageDependencies(generateProtoPackageDependencies())
        .additionalDependencies(generateAdditionalDependencies(model, productConfig))
        .readmeMetadata(
            ReadmeMetadataView.newBuilder()
                .moduleName("")
                .shortName(packageConfig.shortName())
                .fullName(model.getServiceConfig().getTitle())
                .apiSummary(model.getServiceConfig().getDocumentation().getSummary())
                .hasMultipleServices(false)
                .gapicPackageName("gapic-" + packageConfig.packageName(TargetLanguage.PYTHON))
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
                .build())
        .build();
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

  private List<PackageDependencyView> generateAdditionalDependencies(
      Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<PackageDependencyView> dependencies = ImmutableList.builder();
    dependencies.add(
        PackageDependencyView.create(
            "google-gax", packageConfig.gaxVersionBound(TargetLanguage.PYTHON)));
    dependencies.add(
        PackageDependencyView.create(
            "google-auth", packageConfig.authVersionBound(TargetLanguage.PYTHON)));
    dependencies.add(
        PackageDependencyView.create("requests", VersionBound.create("2.18.4", "3.0dev")));
    if (hasLongrunningMethods(model, productConfig)) {
      dependencies.add(
          PackageDependencyView.create(
              "google-cloud-core", VersionBound.create("0.27.0", "1.0dev")));
    }
    return dependencies.build();
  }

  private boolean hasLongrunningMethods(Model model, GapicProductConfig productConfig) {
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      if (productConfig.getInterfaceConfig(apiInterface).hasLongRunningOperations()) {
        return true;
      }
    }
    return false;
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

  private List<String> computeNamespacePackages(String packageName, SurfaceNamer namer) {
    List<String> namespacePackages = new ArrayList<>();
    for (String subPackage : computePackages(packageName)) {
      if (isNamespacePackage(namer, subPackage)) {
        namespacePackages.add(subPackage);
      }
    }
    namespacePackages.add("google.cloud.proto");
    namespacePackages.add(String.format("google.cloud.proto.%s", packageConfig.shortName()));
    return namespacePackages;
  }

  /** Set all packages to be namespace packages except for the version package (if present) */
  private boolean isNamespacePackage(SurfaceNamer namer, String packageName) {
    return !namer.getPackageName().equals(packageName)
        && !namer.getVersionedDirectoryNamespace().equals(packageName);
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
      if (isNamespacePackage(namer, packageName)) {
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
      Interface apiInterface, GapicProductConfig productConfig) {
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
