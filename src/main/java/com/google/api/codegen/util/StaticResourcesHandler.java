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
package com.google.api.codegen.util;

import com.google.api.codegen.SnippetSetRunner;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteStreams;
import java.io.IOException;
import java.util.Map;

public class StaticResourcesHandler {
  private Map<String, String> resourceFilesMap;

  public StaticResourcesHandler(Map<String, String> resourceFilesMap) {
    this.resourceFilesMap = ImmutableMap.copyOf(resourceFilesMap);
  }

  public Map<String, String> getResourceFilesMap() {
    return this.resourceFilesMap;
  }

  public Map<String, byte[]> getResources() throws IOException {
    ClassLoader cl = getClass().getClassLoader();
    ImmutableMap.Builder<String, byte[]> resources = ImmutableMap.builder();
    for (Map.Entry<String, String> entry : resourceFilesMap.entrySet()) {
      String resourcePath = SnippetSetRunner.SNIPPET_RESOURCE_ROOT + '/' + entry.getKey();
      byte[] resource = ByteStreams.toByteArray(cl.getResourceAsStream(resourcePath));
      resources.put(entry.getValue(), resource);
    }

    return resources.build();
  }
}
