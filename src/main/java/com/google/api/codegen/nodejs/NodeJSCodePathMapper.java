/* Copyright 2016 Google LLC
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
import com.google.common.base.Splitter;
import java.util.List;

public class NodeJSCodePathMapper implements GapicCodePathMapper {
  @Override
  public String getOutputPath(String elementFullName, ProductConfig config) {
    String apiVersion = "";
    List<String> packages = Splitter.on(".").splitToList(elementFullName);
    if (packages.size() > 2) {
      String parentName = packages.get(packages.size() - 2);
      if (parentName.matches("v[0-9]+(p[0-9]+)?((alpha|beta)[0-9]+)?")) {
        apiVersion = parentName;
      }
    }
    return apiVersion.isEmpty() ? "src" : ("src/" + apiVersion);
  }
}
