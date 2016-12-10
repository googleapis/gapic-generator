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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.codegen.SnippetSetRunner;
import com.google.api.codegen.viewmodel.ViewModel;
import java.nio.file.Paths;
import java.util.Map;

/*
 * A Context object to be passed into the snippet template rendering phase of package metadata
 * generation.
 */
public class GrpcMetadataContext implements ViewModel {
  private final Map<String, Object> dependenciesMap;

  private final Map<String, Object> defaultsMap;

  private final GrpcPackageCopierResult.Metadata copierResults;

  private final ApiNameInfo apiNameInfo;

  private final String templateFileName;

  private final String resourceRoot = SnippetSetRunner.SNIPPET_RESOURCE_ROOT + "/metadatagen";

  /**
   * Constructor.
   *
   * @param templateFileName The name of the template to be parsed. Must have the same name as the
   *     file to be output, plus an extension. For example, a template that renders a file called
   *     "setup.py" might be named "setup.py.snip".
   * @param apiNameInfo Represents naming information for the given API.
   * @param copierResults Results from the copier phase of package metadata generation.
   * @param dependenciesMap The parsed YAML dependencies configuration file.
   * @param defaultsMap The parsed YAML defaults configuration file.
   */
  public GrpcMetadataContext(
      String templateFileName,
      ApiNameInfo apiNameInfo,
      GrpcPackageCopierResult.Metadata copierResults,
      Map<String, Object> dependenciesMap,
      Map<String, Object> defaultsMap) {
    this.templateFileName = templateFileName;
    this.apiNameInfo = apiNameInfo;
    this.copierResults = copierResults;
    this.dependenciesMap = dependenciesMap;
    this.defaultsMap = defaultsMap;
  }

  public String getDependencyValue(String keySpecifier) {
    return nestedGet(dependenciesMap, keySpecifier);
  }

  public String getDefaultsValue(String keySpecifier) {
    return nestedGet(defaultsMap, keySpecifier);
  }

  @SuppressWarnings("unchecked")
  // Note: Snippets doesn't seem to support variadic args, so we use "." as a separator.
  //
  // TODO (geigerj): this is a hack to port the dynamic Javascript configuration from Packman to
  // Java. To hew closer to Java style, investigate defining configuration information in a
  // well-defined format that can be converted properly to Java types.
  private String nestedGet(Map<String, Object> map, String keySpecifier) {
    String[] keys = keySpecifier.split("\\.");
    Map<String, Object> current = map;
    for (int i = 0; i < keys.length - 1; i++) {
      current = (Map<String, Object>) current.get(keys[i]);
    }
    return (String) current.get(keys[keys.length - 1]);
  }

  public ApiNameInfo getApiNameInfo() {
    return apiNameInfo;
  }

  public GrpcPackageCopierResult.Metadata getCopierMetadata() {
    return copierResults;
  }

  @Override
  public String resourceRoot() {
    return resourceRoot;
  }

  @Override
  public String templateFileName() {
    return templateFileName;
  }

  @Override
  public String outputPath() {
    String baseName = Paths.get(templateFileName).getFileName().toString();
    int extensionIndex = baseName.lastIndexOf(".");
    return baseName.substring(0, extensionIndex);
  }
}
