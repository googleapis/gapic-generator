/* Copyright 2019 Google LLC
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

import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.VersionMatcher;
import com.google.common.base.Splitter;
import java.util.List;

/** An implementation of GapicCodePathMapper that is used by Node.js, Python and Ruby Samples. */
public class CommonSampleCodePathMapper implements GapicCodePathMapper {

  @Override
  public String getOutputPath(String elementFullName, ProductConfig config) {
    return String.join("/", new String[] {"samples", getVersion(config.getPackageName())});
  }

  private String getVersion(String packageName) {
    packageName = packageName.replace("::", ".").replace("_", ".");
    String apiVersion = "";
    List<String> packages = Splitter.on(".").splitToList(packageName);
    for (String p : packages) {
      if (VersionMatcher.isVersion(p)) {
        apiVersion = p;
      }
    }
    if (!apiVersion.isEmpty() && Character.isUpperCase(apiVersion.charAt(0))) {
      return Name.upperCamel(apiVersion).toLowerCamel().toString();
    }
    return apiVersion;
  }
}
