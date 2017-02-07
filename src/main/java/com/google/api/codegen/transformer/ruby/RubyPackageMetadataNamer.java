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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.util.Name;
import com.google.common.base.Splitter;
import java.util.List;

/** A RubyPackageMetadataNamer provides ruby specific names for metadata views. */
public class RubyPackageMetadataNamer extends PackageMetadataNamer {
  private Name serviceName;

  public RubyPackageMetadataNamer(String packageName) {
    // Get the service name from the package name by removing the version suffix (if any).
    List<String> names = Splitter.on("::").splitToList(packageName);
    if (names.size() < 2) {
      this.serviceName = Name.upperCamel(packageName);
    } else {
      this.serviceName = Name.upperCamel(names.get(0));
    }
  }

  @Override
  public String getMetadataName() {
    return serviceName.toUpperCamel();
  }

  @Override
  public String getMetadataIdentifier() {
    return serviceName.toLowerCamel();
  }

  @Override
  public String getOutputFileName() {
    return serviceName.toLowerCamel() + ".gemspec";
  }
}
