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
package com.google.api.codegen.util;

import com.google.common.truth.Truth;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class TypeNameTest {

  @Test
  public void testSimple() {
    TypeName typeName = new TypeName("com.google.Foo", "Foo");
    Truth.assertThat(typeName.getFullName()).isEqualTo("com.google.Foo");
    Truth.assertThat(typeName.getNickname()).isEqualTo("Foo");
    MockTypeTable typeTable = new MockTypeTable();
    Truth.assertThat(typeName.getAndSaveNicknameIn(typeTable)).isEqualTo("Foo");
    List<TypeAlias> expectedImports = Arrays.asList(TypeAlias.create("com.google.Foo", "Foo"));
    Truth.assertThat(typeTable.imports).isEqualTo(expectedImports);
  }

  @Test
  public void testComposite() {
    TypeName typeName = new TypeName("com.google.Foo", "Foo");
    TypeName containerTypeName =
        new TypeName("com.google.Container", "Container", "%s<%i>", typeName);
    Truth.assertThat(containerTypeName.getFullName())
        .isEqualTo("com.google.Container<com.google.Foo>");
    Truth.assertThat(containerTypeName.getNickname()).isEqualTo("Container<Foo>");
    MockTypeTable typeTable = new MockTypeTable();
    Truth.assertThat(containerTypeName.getAndSaveNicknameIn(typeTable)).isEqualTo("Container<Foo>");
    List<TypeAlias> expectedImports =
        Arrays.asList(
            TypeAlias.create("com.google.Container", "Container"),
            TypeAlias.create("com.google.Foo", "Foo"));
    Truth.assertThat(typeTable.imports).isEqualTo(expectedImports);
  }

  private static class MockTypeTable implements TypeTable {
    public List<TypeAlias> imports = new ArrayList<>();

    @Override
    public String getAndSaveNicknameFor(TypeAlias alias) {
      imports.add(alias);
      return alias.getNickname();
    }

    @Override
    public TypeName getTypeName(String fullName) {
      return null;
    }

    @Override
    public TypeTable cloneEmpty() {
      return new MockTypeTable();
    }

    @Override
    public TypeTable cloneEmpty(String packageName) {
      return new MockTypeTable();
    }

    @Override
    public String getAndSaveNicknameFor(String fullName) {
      return null;
    }

    @Override
    public String getAndSaveNicknameFor(TypeName typeName) {
      return null;
    }

    @Override
    public Map<String, TypeAlias> getImports() {
      return null;
    }

    @Override
    public Map<String, TypeAlias> getAllImports() {
      return null;
    }

    @Override
    public NamePath getNamePath(String fullName) {
      return null;
    }

    @Override
    public TypeName getContainerTypeName(String containerFullName, String... elementFullNames) {
      return null;
    }

    @Override
    public TypeName getTypeNameInImplicitPackage(String shortName) {
      return null;
    }

    @Override
    public String getAndSaveNicknameForInnerType(
        String containerFullName, String innerTypeShortName) {
      return null;
    }
  }
}
