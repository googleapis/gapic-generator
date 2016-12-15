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

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.viewmodel.PackageMetadataView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

public class PythonPackageMetadataTransformer implements ModelToViewTransformer {

  PackageMetadataConfig packageConfig;

  public PythonPackageMetadataTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<ViewModel> transform(final Model model, final ApiConfig apiConfig) {
    String version = packageConfig.apiVersion();
    List<ViewModel> initFiles =
        computeInitFiles(computePackages(apiConfig.getPackageName()), version);
    List<ViewModel> topLevelMetadata =
        Lists.transform(
            getTopLevelTemplateFileNames(),
            new Function<String, ViewModel>() {
              @Override
              @Nullable
              public ViewModel apply(@Nullable String templateFileName) {
                return generateMetadataView(model, apiConfig, templateFileName);
              }
            });

    List<ViewModel> metadata = new ArrayList<>();
    metadata.addAll(initFiles);
    metadata.addAll(topLevelMetadata);
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
        "py/tox.ini.snip");
  }

  public List<String> getInitTemplateFileNames() {
    return Lists.newArrayList("py/__init__.py.snip", "py/namespace__init__.py.snip");
  }

  private ViewModel generateMetadataView(Model model, ApiConfig apiConfig, String template) {
    int extensionIndex = template.lastIndexOf(".");
    String outputPath = template.substring(0, extensionIndex);

    return PackageMetadataView.newBuilder()
        .templateFileName(template)
        .outputPath(outputPath)
        .identifier(apiConfig.getDomainLayerLocation())
        .packageVersion(packageConfig.packageVersion("python"))
        .protoPath(packageConfig.protoPath())
        .shortName(packageConfig.shortName())
        .gaxVersion(packageConfig.gaxVersion("python"))
        .protoVersion(packageConfig.protoVersion("python"))
        .commonProtosVersion(packageConfig.commonProtosVersion("python"))
        .packageName(packageConfig.packageName("python"))
        .majorVersion(packageConfig.apiVersion())
        .author(packageConfig.author())
        .email(packageConfig.email())
        .homepage(packageConfig.homepage())
        .license(packageConfig.license())
        .fullName(model.getServiceConfig().getTitle())
        .serviceName(apiConfig.getPackageName())
        .namespacePackages(
            computeNamespacePackages(apiConfig.getPackageName(), packageConfig.apiVersion()))
        .build();
  }

  private List<String> computePackages(String packageName) {
    ArrayList<String> packages = new ArrayList<>();
    StringBuilder current = new StringBuilder();
    boolean first = true;
    for (String pkg : Splitter.on(".").split(packageName)) {
      if (!first) {
        current.append("." + pkg);
      } else {
        current.append(pkg);
        first = false;
      }
      packages.add(current.toString());
    }
    return packages;
  }

  private List<String> computeNamespacePackages(String packageName, final String apiVersion) {
    return Lists.newArrayList(
        Iterables.filter(
            computePackages(packageName),
            new Predicate<String>() {
              @Override
              public boolean apply(@Nullable String input) {
                return isNamespacePackage(input, apiVersion);
              }
            }));
  }

  private boolean isNamespacePackage(String packageName, String apiVersion) {
    int lastDot = packageName.lastIndexOf(".");
    return lastDot < 0 || !packageName.substring(lastDot + 1).equals(apiVersion);
  }

  private List<ViewModel> computeInitFiles(List<String> packages, final String apiVersion) {
    return Lists.transform(
        packages,
        new Function<String, ViewModel>() {
          @Override
          @Nullable
          public ViewModel apply(@Nullable final String packageName) {
            final String template;
            if (isNamespacePackage(packageName, apiVersion)) {
              template = "py/namespace__init__.py.snip";
            } else {
              template = "py/__init__.py.snip";
            }
            return new ViewModel() {
              @Override
              public String resourceRoot() {
                return SnippetSetRunner.SNIPPET_RESOURCE_ROOT;
              }

              @Override
              public String templateFileName() {
                return template;
              }

              @Override
              public String outputPath() {
                return Paths.get(packageName.replace(".", File.separator))
                    .resolve("__init__.py")
                    .toString();
              }
            };
          }
        });
  }
}
