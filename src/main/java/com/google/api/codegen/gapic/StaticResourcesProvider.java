/* Copyright 2018 Google LLC
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
package com.google.api.codegen.gapic;

import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.common.OutputProvider;
import com.google.api.codegen.util.StaticResourcesHandler;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class StaticResourcesProvider implements OutputProvider<byte[]> {
  private StaticResourcesHandler resourcesExtractor;
  private Set<String> executableFilenames;

  public StaticResourcesProvider(Map<String, String> staticFiles, Set<String> executableFilenames) {
    if (!staticFiles.values().containsAll(executableFilenames)) {
      throw new IllegalArgumentException(
          "executableFilenames contains a filename not found in staticFiles");
    }
    this.resourcesExtractor = new StaticResourcesHandler(staticFiles);
    this.executableFilenames = ImmutableSet.copyOf(executableFilenames);
  }

  @Override
  public Set<String> getInputFileNames() {
    return resourcesExtractor.getResourceFilesMap().keySet();
  }

  @Override
  public Map<String, GeneratedResult<byte[]>> generate() throws IOException {
    ImmutableMap.Builder<String, GeneratedResult<byte[]>> results = ImmutableMap.builder();
    for (Map.Entry<String, byte[]> entry : resourcesExtractor.getResources().entrySet()) {
      GeneratedResult<byte[]> result =
          GeneratedResult.create(entry.getValue(), executableFilenames.contains(entry.getKey()));
      results.put(entry.getKey(), result);
    }

    return results.build();
  }
}
