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

import com.google.api.codegen.GapicContext;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.gapic.GapicProvider;
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
import com.google.api.codegen.viewmodel.metadata.ReadmeMetadataView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.io.Files;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
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
  private static final String TEST_PREFIX = "tests.";

  private static final String GITHUB_DOC_HOST =
      "https://googlecloudplatform.github.io/google-cloud-python/stable";
  private static final String GITHUB_REPO_HOST =
      "https://github.com/GoogleCloudPlatform/google-cloud-python";
  private static final String AUTH_DOC_PATH = "/google-cloud-auth";
  private static final String LIB_DOC_PATH = "/%s-usage";
  private static final String MAIN_README_PATH = "/blob/master/README.rst";

  private final PackageMetadataConfig packageConfig;
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();
  private final ValueProducer valueProducer = new PythonValueProducer();
  private final TestCaseTransformer testCaseTransformer = new TestCaseTransformer(valueProducer);
  private final List<GapicProvider<? extends Object>> gapicProviders;
  private final PythonSurfaceNamer surfaceNamer;
  private List<String> apiModules = null;
  private List<String> typeModules = null;

  public PythonPackageMetadataTransformer(
      PackageMetadataConfig packageConfig, List<GapicProvider<? extends Object>> gapicProviders) {
    this.packageConfig = packageConfig;
    this.gapicProviders = gapicProviders;
    this.surfaceNamer = new PythonSurfaceNamer(packageConfig.packageName(TargetLanguage.PYTHON));
  }

  @Override
  public List<ViewModel> transform(final Model model, final GapicProductConfig productConfig) {
    SurfaceNamer surfaceNamer = new PythonSurfaceNamer(productConfig.getPackageName());
    List<ViewModel> metadata =
        computeInitFiles(computePackages(productConfig.getPackageName()), surfaceNamer);
    PackageMetadataNamer namer = new PackageMetadataNamer();
    for (String templateFileName : getTopLevelTemplateFileNames()) {
      metadata.add(
          generateMetadataView(model, productConfig, templateFileName, namer, surfaceNamer));
    }
    return metadata;
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> templates = new ArrayList<>();
    templates.addAll(getTopLevelTemplateFileNames());
    templates.addAll(getInitTemplateFileNames());
    return templates;
  }

  public List<String> getTopLevelTemplateFileNames() {
    return Lists.newArrayList(
        "LICENSE.snip",
        "py/MANIFEST.in.snip",
        "py/PUBLISHING.rst.snip",
        "py/setup.py.snip",
        "py/requirements.txt.snip",
        "py/README.rst.snip",
        "py/tox.ini.snip",
        "py/docs/apis.rst.snip",
        "py/docs/conf.py.snip",
        "py/docs/index.rst.snip",
        "py/docs/starting.rst.snip");
  }

  public List<String> getInitTemplateFileNames() {
    return Lists.newArrayList("py/__init__.py.snip", "py/namespace__init__.py.snip");
  }

  private ViewModel generateMetadataView(
      Model model,
      GapicProductConfig productConfig,
      String template,
      PackageMetadataNamer metadataNamer,
      SurfaceNamer surfaceNamer) {
    List<ApiMethodView> exampleMethods = generateExampleMethods(model, productConfig);
    String noLeadingPyDir = template.startsWith("py/") ? template.substring(3) : template;
    int extensionIndex = noLeadingPyDir.lastIndexOf(".");
    String outputPath = noLeadingPyDir.substring(0, extensionIndex);
    computeModules(gapicProviders);
    return metadataTransformer
        .generateMetadataView(packageConfig, model, template, outputPath, TargetLanguage.PYTHON)
        .namespacePackages(computeNamespacePackages(productConfig.getPackageName(), surfaceNamer))
        .developmentStatus(
            surfaceNamer.getReleaseAnnotation(packageConfig.releaseLevel(TargetLanguage.PYTHON)))
        .apiModules(apiModules)
        .typeModules(typeModules)
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

  /** Determines the Python files generated in the main phase of generation. */
  private void computeModules(List<GapicProvider<? extends Object>> gapicProviders) {
    // Only run generation once.
    if (apiModules != null && typeModules != null) {
      return;
    }
    apiModules = new ArrayList<>();
    typeModules = new ArrayList<>();

    for (GapicProvider<? extends Object> provider : gapicProviders) {
      Map<String, Doc> result = provider.generate();
      for (String fileName : result.keySet()) {
        if (!Files.getFileExtension(fileName).equals("py")) {
          continue;
        }
        String moduleName =
            fileName.substring(0, fileName.length() - ".py".length()).replace("/", ".");
        if (moduleName.startsWith(TEST_PREFIX)) {
          continue;
        }

        if (moduleName.endsWith(GapicContext.API_WRAPPER_SUFFIX.toLowerCase())) {
          apiModules.add(moduleName);
        } else {
          typeModules.add(moduleName);
        }
      }
    }
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
        template = "py/namespace__init__.py.snip";
      } else if (isVersionedDirectoryPackage(namer, packageName)) {
        continue;
      } else {
        template = "py/__init__.py.snip";
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
