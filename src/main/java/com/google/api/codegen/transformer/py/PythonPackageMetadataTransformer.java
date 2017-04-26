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
import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.gapic.GapicProvider;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.viewmodel.SimpleViewModel;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.snippet.Doc;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
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
  private static final String TEST_PREFIX = "test.";

  private final PackageMetadataConfig packageConfig;
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();
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
    String version = packageConfig.apiVersion();
    List<ViewModel> metadata =
        computeInitFiles(computePackages(productConfig.getPackageName()), version);
    for (String templateFileName : getTopLevelTemplateFileNames()) {
      metadata.add(generateMetadataView(model, productConfig, templateFileName));
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
      Model model, GapicProductConfig productConfig, String template) {
    String noLeadingPyDir = template.startsWith("py/") ? template.substring(3) : template;
    int extensionIndex = noLeadingPyDir.lastIndexOf(".");
    String outputPath = noLeadingPyDir.substring(0, extensionIndex);
    computeModules(gapicProviders);

    return metadataTransformer
        .generateMetadataView(packageConfig, model, template, outputPath, TargetLanguage.PYTHON)
        .namespacePackages(
            computeNamespacePackages(productConfig.getPackageName(), packageConfig.apiVersion()))
        .developmentStatus(
            surfaceNamer.getReleaseAnnotation(packageConfig.releaseLevel(TargetLanguage.PYTHON)))
        .apiModules(apiModules)
        .typeModules(typeModules)
        .build();
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

  private List<String> computeNamespacePackages(String packageName, final String apiVersion) {
    List<String> namespacePackages = new ArrayList<>();
    for (String subPackage : computePackages(packageName)) {
      if (isNamespacePackage(subPackage, apiVersion)) {
        namespacePackages.add(subPackage);
      }
    }
    return namespacePackages;
  }

  /** Set all packages to be namespace packages except for the version package (if present) */
  private boolean isNamespacePackage(String packageName, String apiVersion) {
    int lastDot = packageName.lastIndexOf(".");
    return lastDot < 0 || !packageName.substring(lastDot + 1).equals(apiVersion);
  }

  /**
   * Determines which __init__.py files to generate given a list of Python packages. Each Python
   * package corresponds to exactly one __init__.py file, although the contents of that file depend
   * on whether the package is a namespace package.
   */
  private List<ViewModel> computeInitFiles(List<String> packages, final String apiVersion) {
    List<ViewModel> initFiles = new ArrayList<>();
    for (String packageName : packages) {
      final String template;
      if (isNamespacePackage(packageName, apiVersion)) {
        template = "py/namespace__init__.py.snip";
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
}
