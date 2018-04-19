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
  private String metadataIdenfifier;

  public PhpPackageMetadataNamer(String packageName, String domainLayerLocation) {
    String packageNameWithoutVersion = PhpPackageUtil.getPackageNameBeforeVersion(packageName);
    String[] packagePieces = PhpPackageUtil.splitPackageName(packageNameWithoutVersion);

    this.serviceName = Name.upperCamel(packagePieces);

    // To build metadata identifier, update pieces to be lowercase, so each piece is treated as a
    // single piece in the Name object
    for (int i = 0; i < packagePieces.length; i++) {
      packagePieces[i] = packagePieces[i].toLowerCase();
    }

    Name composerVendor;
    Name composerPackage;
    // The metadataIdentifier for composer is formatted as "vendor/package". If a
    // domainLayerLocation is provided, set that as the vendor. Otherwise, take the first piece of
    // packagePieces to use as the vendor.
    if (domainLayerLocation != null && !domainLayerLocation.equals("")) {
      composerVendor = Name.upperCamel(domainLayerLocation);
      composerPackage = Name.anyLower(packagePieces);
    } else if (packagePieces.length == 1) {
      composerVendor = Name.anyLower(packagePieces[0]);
      composerPackage = composerVendor;
    } else {
      composerVendor = Name.anyLower(packagePieces[0]);
      composerPackage = Name.from();
      for (int i = 1; i < packagePieces.length; i++) {
        composerPackage = composerPackage.join(Name.anyLower(packagePieces[i]));
      }
    }
    metadataIdenfifier =
        composerVendor.toSeparatedString("-") + "/" + composerPackage.toSeparatedString("-");
  }

  @Override
  public String getMetadataName() {
    return serviceName.toUpperCamel();
  }

  public Name getServiceName() {
    return serviceName;
  }

  @Override
  public String getMetadataIdentifier() {
    return metadataIdenfifier;
  }
}
