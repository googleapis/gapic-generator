/* Copyright 2018 Google LLC
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
package com.google.api.codegen.transformer;

import static com.google.api.codegen.transformer.OutputTransformer.ScopeTable;
import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.tools.framework.model.TypeRef;
import org.junit.Before;
import org.junit.Test;

public class OutputTransformerTest {

  private ScopeTable outer;
  private ScopeTable inner;

  @Before
  public void setUp() {
    outer = new ScopeTable();
    inner = new ScopeTable(outer);
  }

  @Test
  public void testScopeTablePut() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(outer.put("str", stringTypeModel, "String")).isTrue();
  }

  @Test
  public void testScopeTablePutFail() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(outer.put("str", stringTypeModel, "String")).isTrue();
    assertThat(outer.put("str", stringTypeModel, "String")).isFalse();
  }

  @Test
  public void testScopeTableGetTypeModel() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(outer.put("str", stringTypeModel, "String")).isTrue();
    assertThat(outer.getTypeModel("str")).isEqualTo(stringTypeModel);
    assertThat(outer.getTypeName("str")).isEqualTo("String");

    assertThat(outer.getTypeModel("book")).isNull();
    assertThat(outer.getTypeName("book")).isNull();
  }

  @Test
  public void testScopeTablePutAndGetResourceName() {
    assertThat(outer.put("book", null, "ShelfBookName")).isTrue();
    assertThat(outer.getTypeModel("book")).isEqualTo(null);
    assertThat(outer.getTypeName("book")).isEqualTo("ShelfBookName");
  }

  @Test
  public void testScopeTableGetFromParent() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(outer.put("str", stringTypeModel, "String")).isTrue();
    assertThat(outer.put("book", null, "ShelfBookName")).isTrue();

    assertThat(inner.getTypeModel("str")).isEqualTo(stringTypeModel);
    assertThat(inner.getTypeName("str")).isEqualTo("String");
    assertThat(inner.getTypeModel("book")).isEqualTo(null);
    assertThat(inner.getTypeName("book")).isEqualTo("ShelfBookName");
  }
}
