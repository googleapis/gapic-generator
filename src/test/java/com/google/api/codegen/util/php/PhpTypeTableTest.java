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
package com.google.api.codegen.util.php;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.util.TypeAlias;
import java.util.Map;
import org.junit.Test;

public class PhpTypeTableTest {

  @Test
  public void testImplicitPackageNameFiltering() {
    String implicitPackage = "foo\\bar";
    Map<String, TypeAlias> imports;
    PhpTypeTable typeTable = new PhpTypeTable(implicitPackage);
    assertThat(typeTable.getAndSaveNicknameFor(implicitPackage + "\\Baz")).isEqualTo("Baz");

    imports = typeTable.getImports();
    assertThat(imports.size()).isEqualTo(0);

    assertThat(typeTable.getAndSaveNicknameFor(implicitPackage + "\\qux\\Corge"))
        .isEqualTo("Corge");

    imports = typeTable.getImports();
    assertThat(imports.size()).isEqualTo(1);
    assertThat(imports.get(implicitPackage + "\\qux\\Corge").getNickname()).isEqualTo("Corge");
  }
}
