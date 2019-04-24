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
package com.google.api.codegen.nodejs;

import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.util.VersionMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.util.ArrayList;
import java.util.List;

public class NodeJSCodePathMapper implements GapicCodePathMapper {
  @Override
  public String getOutputPath(String elementFullName, ProductConfig config) {
    ArrayList<String> dirs = new ArrayList<>();
    dirs.add("src");
    String apiVersion = getVersion(elementFullName);
    if (!apiVersion.isEmpty()) {
      dirs.add(apiVersion);
    }
    return Joiner.on("/").join(dirs);
  }

  @Override
  public String getSamplesOutputPath(String elementFullName, ProductConfig config, String method) {
    ArrayList<String> dirs = new ArrayList<>();
    dirs.add("samples");
    String apiVersion = getVersion(elementFullName);
    if (!apiVersion.isEmpty()) {
      dirs.add(apiVersion);
    }
    return Joiner.on("/").join(dirs);
  }

  private String getVersion(String elementFullName) {
    String apiVersion = "";
    List<String> packages = Splitter.on(".").splitToList(elementFullName);
    for (String p : packages) {
      if (VersionMatcher.isVersion(p)) {
        apiVersion = p;
      }
    }
    return apiVersion;
  }
}
