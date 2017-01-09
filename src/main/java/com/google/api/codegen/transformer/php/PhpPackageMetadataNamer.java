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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.util.Name;
import com.google.common.base.Splitter;
import java.util.List;

/** PHPPackageMetadataNamer provides PHP specific names for metadata views. */
public class PhpPackageMetadataNamer extends PackageMetadataNamer {
  private Name serviceName;
  private String domainLayerLocation;

  public PhpPackageMetadataNamer(String packageName, String domainLayerLocation) {
    // Get the service name from the package name by removing the version suffix (if any).
    List<String> names = Splitter.on("\\").splitToList(packageName);
    if (names.size() < 2) {
      this.serviceName = Name.upperCamel(packageName);
    } else {
      String serviceName = names.get(names.size() - 1);
      if (serviceName.matches("V\\d+.*")) {
        serviceName = names.get(names.size() - 2);
      }
      this.serviceName = Name.upperCamel(serviceName);
    }
    this.domainLayerLocation = domainLayerLocation;
  }

  @Override
  public String getMetadataName() {
    return serviceName.toUpperCamel();
  }

  @Override
  public String getMetadataIdentifier() {
    String serviceNameLower = serviceName.toSeparatedString("");
    if (domainLayerLocation != null && !domainLayerLocation.isEmpty()) {
      return domainLayerLocation + "/" + serviceNameLower;
    } else {
      return serviceNameLower + "/" + serviceNameLower;
    }
  }
}
