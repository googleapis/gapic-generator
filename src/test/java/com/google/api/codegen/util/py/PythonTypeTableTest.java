/* Copyright 2017 Google LLC
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
package com.google.api.codegen.util.py;

import com.google.api.codegen.util.TypeAlias;
import com.google.common.truth.Truth;
import java.util.Map;
import org.junit.Test;

public class PythonTypeTableTest {

  @Test
  public void testDisambiguate_movePackage() {
    PythonTypeTable typeTable = new PythonTypeTable("foo.bar");
    Truth.assertThat(typeTable.getAndSaveNicknameFor(TypeAlias.create("a.c.D", "c.D")))
        .isEqualTo("c.D");
    Truth.assertThat(typeTable.getAndSaveNicknameFor(TypeAlias.create("b.c.E", "c.E")))
        .isEqualTo("b_c.E");
    Map<String, TypeAlias> imports = typeTable.getImports();
    Truth.assertThat(imports.get("a.c.D").getNickname()).isEqualTo("a_c.D");
    Truth.assertThat(imports.get("b.c.E").getNickname()).isEqualTo("b_c.E");
  }

  @Test
  public void testDisambiguate_move2Packages() {
    PythonTypeTable typeTable = new PythonTypeTable("foo.bar");
    Truth.assertThat(typeTable.getAndSaveNicknameFor(TypeAlias.create("a.c.d.E", "c_d.E")))
        .isEqualTo("c_d.E");
    Truth.assertThat(typeTable.getAndSaveNicknameFor(TypeAlias.create("b.c.d.F", "c_d.F")))
        .isEqualTo("b_c_d.F");
    Map<String, TypeAlias> imports = typeTable.getImports();
    Truth.assertThat(imports.get("a.c.d.E").getNickname()).isEqualTo("a_c_d.E");
    Truth.assertThat(imports.get("b.c.d.F").getNickname()).isEqualTo("b_c_d.F");
  }

  @Test
  public void testDisambiguate_move3Packages() {
    PythonTypeTable typeTable = new PythonTypeTable("foo.bar");
    Truth.assertThat(typeTable.getAndSaveNicknameFor(TypeAlias.create("a.c.d.e.F", "c_d_e.F")))
        .isEqualTo("c_d_e.F");
    Truth.assertThat(typeTable.getAndSaveNicknameFor(TypeAlias.create("b.c.d.e.G", "c_d_e.G")))
        .isEqualTo("b_c_d_e.G");
    Map<String, TypeAlias> imports = typeTable.getImports();
    Truth.assertThat(imports.get("a.c.d.e.F").getNickname()).isEqualTo("a_c_d_e.F");
    Truth.assertThat(imports.get("b.c.d.e.G").getNickname()).isEqualTo("b_c_d_e.G");
  }
}
