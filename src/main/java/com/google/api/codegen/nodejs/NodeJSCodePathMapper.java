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
package com.google.api.codegen.nodejs;

import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.base.Splitter;
import java.util.List;

public class NodeJSCodePathMapper implements GapicCodePathMapper {
  @Override
  public String getOutputPath(ProtoElement element, ProductConfig config) {
    String apiVersion = "";
    List<String> packages = Splitter.on(".").splitToList(element.getFullName());
    if (packages.size() > 2) {
      String parentName = packages.get(packages.size() - 2);
      if (VersionMatcher.isVersion(parentName)) {
        apiVersion = parentName;
      }
    }
    return apiVersion.isEmpty() ? "src" : ("src/" + apiVersion);
  }
}
