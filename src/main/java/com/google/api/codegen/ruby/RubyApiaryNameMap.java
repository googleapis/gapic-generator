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
package com.google.api.codegen.ruby;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RubyApiaryNameMap {

  private final ImmutableMap<ResourceId, String> NAME_MAP;

  public RubyApiaryNameMap() {
    try {
      NAME_MAP = getNameMap();
    } catch (IOException ex) {
      throw new IllegalStateException(ex);
    }
  }

  public String getName(String apiName, String apiVersion, String resourceName) {
    ResourceId id = ResourceId.create(apiName, apiVersion, resourceName);
    return NAME_MAP.get(id);
  }

  private static ImmutableMap<ResourceId, String> getNameMap() throws IOException {
    String data =
        Resources.toString(
            Resources.getResource(RubyApiaryNameMap.class, "apiary_names.yaml"),
            StandardCharsets.UTF_8);
    Map<String, String> nameData = (Map<String, String>) (new Yaml().load(data));
    ImmutableMap.Builder<ResourceId, String> builder = ImmutableMap.<ResourceId, String>builder();
    for (Map.Entry<String, String> entry : nameData.entrySet()) {
      builder.put(parseKey(entry.getKey()), entry.getValue());
    }
    return builder.build();
  }

  private static final Pattern keyPattern = Pattern.compile("^/(.*?):(.*?)[/?](.*)$");

  private static ResourceId parseKey(String key) {
    // Format: /adexchangebuyer:v1.4/adexchangebuyer.proposals.setupcomplete
    Matcher matcher = keyPattern.matcher(key);
    if (!matcher.matches()) {
      throw new IllegalArgumentException("malformed key: " + key);
    }
    return ResourceId.create(matcher.group(1), matcher.group(2), matcher.group(3));
  }

  @AutoValue
  abstract static class ResourceId {
    abstract String getApiName();

    abstract String getApiVersion();

    abstract String getResourceName();

    private static ResourceId create(String apiName, String apiVersion, String resourceName) {
      return new AutoValue_RubyApiaryNameMap_ResourceId(apiName, apiVersion, resourceName);
    }
  }
}
