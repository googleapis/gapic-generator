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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.php.PhpPackageUtil;

/** PHPPackageMetadataNamer provides PHP specific names for metadata views. */
public class PhpPackageMetadataNamer extends PackageMetadataNamer {
  private Name serviceName;
  private String domainLayerLocation;

  public PhpPackageMetadataNamer(String packageName, String domainLayerLocation) {
    // Get the service name from the package name by removing the version suffix (if any).
    this.serviceName = getApiNameFromPackageName(packageName);
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

  public static Name getApiNameFromPackageName(String packageName) {
    String[] names = PhpPackageUtil.splitPackageName(packageName);
    if (names.length < 2) {
      return Name.upperCamel(packageName);
    } else {
      String serviceName = names[names.length - 1];
      if (PhpPackageUtil.isPackageVersion(serviceName)) {
        serviceName = names[names.length - 2];
      }
      return Name.upperCamel(serviceName);
    }
  }
}
