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
import com.google.common.base.Charsets;
import com.google.common.base.Joiner;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.yaml.snakeyaml.Yaml;

public class RubyTypeNameGenerator extends TypeNameGenerator {

  private static final LoadingCache<String, ImmutableMap<String, String>> NAME_MAP_CACHE =
      CacheBuilder.newBuilder()
          .expireAfterWrite(15, TimeUnit.SECONDS)
          .maximumSize(10)
          .build(
              new CacheLoader<String, ImmutableMap<String, String>>() {
                @Override
                @SuppressWarnings("unchecked")
                public ImmutableMap<String, String> load(String key) throws IOException {
                  String data = null;
                  if (!key.isEmpty()) {
                    File namesFile = new File(key);
                    if (namesFile.exists() && !namesFile.isDirectory()) {
                      data = Files.toString(namesFile, Charsets.UTF_8);
                    }
                  }
                  if (data == null) {
                    data =
                        Resources.toString(
                            Resources.getResource(RubyApiaryNameMap.class, "apiary_names.yaml"),
                            Charsets.UTF_8);
                  }
                  // Unchecked cast here.
                  return ImmutableMap.copyOf((Map<String, String>) (new Yaml().load(data)));
                }
              });

  private String apiName;
  private final ImmutableMap<String, String> nameMap;

  public RubyTypeNameGenerator(String rubyNamesFile) {
    // Caches don't like nulls. So turn nulls into empty strings.
    // Both are sentinels for using the default.
    if (rubyNamesFile == null) {
      rubyNamesFile = "";
    }
    nameMap = NAME_MAP_CACHE.getUnchecked(rubyNamesFile);
  }

  @Override
  public String stringDelimiter() {
    return "'";
  }

  @Override
  public void setApiCanonicalNameAndVersion(String apiCanonicalName, String apiVersion) {
    super.setApiCanonicalNameAndVersion(apiCanonicalName, apiVersion);
    apiName = this.apiCanonicalName.toLowerCase();
  }

  @Override
  public List<String> getMethodNameComponents(List<String> nameComponents) {
    // Generate the key by joining apiName, apiVersion and nameComponents on '.'
    // Ex: "/admin:directory_v1/admin.channels.stop"
    String key = "/" + apiName + ":" + apiVersion + "/" + Joiner.on('.').join(nameComponents);
    if (!nameMap.containsKey(key)) {
      throw new IllegalArgumentException("\"" + key + "\"" + " not in method name map");
    }
    return ImmutableList.of(nameMap.get(key));
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
    if (!nameMap.containsKey(key)) {
      throw new IllegalArgumentException("\"" + key + "\"" + " not in method name map");
    }
    return Name.from(nameMap.get(key)).toUpperCamel();
  }
}
