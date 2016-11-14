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
package com.google.api.codegen.metadatagen;

import com.google.api.codegen.CodegenContext;
import com.google.api.tools.framework.snippet.Doc;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

/*
 * A Context object to be passed into the snippet template rendering phase of package metadata
 * generation.
 */
public class PackageMetadataContext extends CodegenContext {
  private Map<String, Object> dependenciesMap = new HashMap<>();

  private Map<String, Object> defaultsMap = new HashMap<>();

  private Doc copierResults;

  private ApiNameInfo apiNameInfo;

  /**
   * Constructor.
   *
   * @param apiNameInfo Represents naming information for the given API.
   * @param copierResults Results from the copier phase of package metadata generation.
   */
  public PackageMetadataContext(ApiNameInfo apiNameInfo, Doc copierResults) {
    this.apiNameInfo = apiNameInfo;
    this.copierResults = copierResults;
  }

  /**
   * Initializes package metadata configuration. Should be called before passing this context to a
   * snippet template environment.
   *
   * @param dependenciesFile The path to the dependencies file
   * @param defaultsFile The path to the defaults file
   * @throws IOException
   */
  @SuppressWarnings("unchecked")
  public void loadConfigurationFromFile(String dependenciesFile, String defaultsFile)
      throws IOException {
    Yaml yaml = new Yaml();

    String dependencies =
        new String(Files.readAllBytes(Paths.get(dependenciesFile)), StandardCharsets.UTF_8);
    this.dependenciesMap = (Map<String, Object>) yaml.load(dependencies);

    String defaults =
        new String(Files.readAllBytes(Paths.get(defaultsFile)), StandardCharsets.UTF_8);
    this.defaultsMap = (Map<String, Object>) yaml.load(defaults);
  }

  public String getDependencyValue(String keySpecifier) {
    return getUncheckedMapValue(dependenciesMap, keySpecifier);
  }

  public String getDefaultsValue(String keySpecifier) {
    return getUncheckedMapValue(defaultsMap, keySpecifier);
  }

  @SuppressWarnings("unchecked")
  // Note: Snippets doesn't seem to support variadic args, so we use ":" as a separator.
  //
  // TODO (geigerj): this is a hack to port the dynamic Javascript configuration from Packman to
  // Java. To hew closer to Java style, investigate defining configuration information in a
  // well-defined format that can be converted properly to Java types.
  private String getUncheckedMapValue(Map<String, Object> map, String keySpecifier) {
    String[] keys = keySpecifier.split(":");
    Map<String, Object> current = map;
    for (int i = 0; i < keys.length - 1; i++) {
      current = (Map<String, Object>) current.get(keys[i]);
    }
    return (String) current.get(keys[keys.length - 1]);
  }

  public ApiNameInfo getApiNameInfo() {
    return apiNameInfo;
  }

  // TODO (geigerj): accessing apiNameInfo directly in the snippets leads to errors, hence these
  // aliases in the context. Investigate whether these are truly needed.
  public String getShortName() {
    return apiNameInfo.shortName();
  }

  public String getFullName() {
    return apiNameInfo.fullName();
  }

  public String getVersion() {
    return apiNameInfo.majorVersion();
  }

  public String getPath() {
    return apiNameInfo.path();
  }

  public String getPackageName() {
    return "grpc-google-" + apiNameInfo.shortName() + "-" + apiNameInfo.majorVersion();
  }

  public Doc getCopierResults() {
    return copierResults;
  }
}
