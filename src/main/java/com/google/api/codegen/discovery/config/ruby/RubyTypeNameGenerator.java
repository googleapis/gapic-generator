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
package com.google.api.codegen.discovery.config.ruby;

import com.google.api.codegen.discovery.config.TypeNameGenerator;
import com.google.api.codegen.ruby.RubyApiaryNameMap;
import com.google.api.codegen.util.Name;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.yaml.snakeyaml.Yaml;

public class RubyTypeNameGenerator extends TypeNameGenerator {

  private String apiName;
  private String apiVersion;
  private final ImmutableMap<String, String> NAME_MAP;

  public RubyTypeNameGenerator() {
    try {
      NAME_MAP = getMethodNameMap();
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  @SuppressWarnings("unchecked")
  private ImmutableMap<String, String> getMethodNameMap() throws IOException {
    String data =
        Resources.toString(
            Resources.getResource(RubyApiaryNameMap.class, "apiary_names.yaml"),
            StandardCharsets.UTF_8);
    // Unchecked cast here.
    return ImmutableMap.copyOf((Map<String, String>) (new Yaml().load(data)));
  }

  @Override
  public String stringDelimiter() {
    return "'";
  }

  @Override
  public void setApiNameAndVersion(String apiName, String apiVersion) {
    this.apiName = apiName;
    this.apiVersion = apiVersion;
  }

  @Override
  public List<String> getMethodNameComponents(List<String> nameComponents) {
    // Generate the key by joining apiName, apiVersion and nameComponents on '.'
    // Ex: "/admin:directory_v1/admin.channels.stop"
    String key = "/" + apiName + ":" + apiVersion + "/" + Joiner.on('.').join(nameComponents);
    if (!NAME_MAP.containsKey(key)) {
      throw new IllegalArgumentException("\"" + key + "\"" + " not in method name map");
    }
    return ImmutableList.of(NAME_MAP.get(key));
  }

  @Override
  public String getApiVersion(String apiVersion) {
    return apiVersion.replace('.', '_'); // v1.4 to v1_4
  }

  @Override
  public String getPackagePrefix(String apiName, String apiCanonicalName, String apiVersion) {
    return "google/apis/" + Name.from(apiName, apiVersion).toLowerUnderscore();
  }

  @Override
  public String getApiTypeName(String apiName) {
    return Name.upperCamel(apiName.replace(" ", ""), "Service").toUpperCamel();
  }

  @Override
  public String getMessageTypeName(String messageTypeName) {
    // Avoid cases like "DatasetList.Datasets"
    String pieces[] = messageTypeName.split("\\.");
    messageTypeName = pieces[0];
    // Generate the key by joining apiName, apiVersion and messageTypeName.
    // Ex: "/bigquery:v2/DatasetList"
    String key = "/" + apiName + ":" + apiVersion + "/" + messageTypeName;
    if (!NAME_MAP.containsKey(key)) {
      throw new IllegalArgumentException("\"" + key + "\"" + " not in method name map");
    }
    return Name.from(NAME_MAP.get(key)).toUpperCamel();
  }
}
