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

import com.google.common.truth.Truth;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JUnitParamsRunner.class)
public class PhpPackageMetadataNamerTest {

  @Test
  @Parameters({
    "Package, Package, package/package",
    "Some\\Package, SomePackage, some/package",
    "\\Some\\Package, SomePackage, some/package",
    "Some\\Package\\V1, SomePackage, some/package",
    "Some\\Package\\V1beta1, SomePackage, some/package",
    "Some\\CamelCasePackage\\V1, SomeCamelCasePackage, some/camelcasepackage",
    "Some\\Deep\\Package\\V1, SomeDeepPackage, some/deep-package"
  })
  public void testWithoutDomainLayerLocationParams(
      String packageName, String expectedMetadataName, String expectedMetadataIdentifier) {
    PhpPackageMetadataNamer namer = new PhpPackageMetadataNamer(packageName, null);
    Truth.assertThat(namer.getMetadataName()).isEqualTo(expectedMetadataName);
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo(expectedMetadataIdentifier);
  }

  @Test
  @Parameters({
    "Package, Package, domain/package",
    "Some\\Package, SomePackage, domain/some-package",
    "\\Some\\Package, SomePackage, domain/some-package",
    "Some\\Package\\V1, SomePackage, domain/some-package",
    "Some\\Package\\V1beta1, SomePackage, domain/some-package",
    "Some\\CamelCasePackage\\V1, SomeCamelCasePackage, domain/some-camelcasepackage",
    "Some\\Deep\\Package\\V1, SomeDeepPackage, domain/some-deep-package"
  })
  public void testWithDomainLayerLocationParams(
      String packageName, String expectedMetadataName, String expectedMetadataIdentifier) {
    PhpPackageMetadataNamer namer = new PhpPackageMetadataNamer(packageName, "domain");
    Truth.assertThat(namer.getMetadataName()).isEqualTo(expectedMetadataName);
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo(expectedMetadataIdentifier);
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
