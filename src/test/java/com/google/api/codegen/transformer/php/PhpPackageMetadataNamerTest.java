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
package com.google.api.codegen.transformer.php;

import com.google.common.truth.Truth;
import org.junit.Test;

public class PhpPackageMetadataNamerTest {

  @Test
  public void testWithoutDomainLayerLocation() {
    PhpPackageMetadataNamer namer = new PhpPackageMetadataNamer("Package", null);
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("package/package");

    namer = new PhpPackageMetadataNamer("Some\\Package", null);
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("package/package");

    namer = new PhpPackageMetadataNamer("Some\\Package\\V1", null);
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("package/package");

    namer = new PhpPackageMetadataNamer("Some\\Package\\V1beta1", null);
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("package/package");

    namer = new PhpPackageMetadataNamer("Some\\CamelCasePackage\\V1", null);
    Truth.assertThat(namer.getMetadataName()).isEqualTo("CamelCasePackage");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("camelcasepackage/camelcasepackage");
  }

  @Test
  public void testWithDomainLayerLocation() {
    PhpPackageMetadataNamer namer = new PhpPackageMetadataNamer("Package", "domain");
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("domain/package");

    namer = new PhpPackageMetadataNamer("Some\\Package", "domain");
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("domain/package");

    namer = new PhpPackageMetadataNamer("Some\\Package\\V1", "domain");
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("domain/package");

    namer = new PhpPackageMetadataNamer("Some\\Package\\V1beta1", "domain");
    Truth.assertThat(namer.getMetadataName()).isEqualTo("Package");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("domain/package");

    namer = new PhpPackageMetadataNamer("Some\\CamelCasePackage\\V1", "domain");
    Truth.assertThat(namer.getMetadataName()).isEqualTo("CamelCasePackage");
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("domain/camelcasepackage");
  }

  @Test(expected = IllegalArgumentException.class)
  public void packageLowerCamel() {
    PhpPackageMetadataNamer namer = new PhpPackageMetadataNamer("somePackage", null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void packageLowerCase() {
    PhpPackageMetadataNamer namer = new PhpPackageMetadataNamer("Some\\package", null);
  }
}
