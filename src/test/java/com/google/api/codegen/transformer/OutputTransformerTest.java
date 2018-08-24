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

  private ScopeTable parent;
  private ScopeTable child;

  @Before
  public void setUp() {
    parent = new ScopeTable();
    child = new ScopeTable(parent);
  }

  @Test
  public void testScopeTablePut() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(parent.put("str", stringTypeModel, "String")).isTrue();
  }

  @Test
  public void testScopeTablePutFail() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(parent.put("str", stringTypeModel, "String")).isTrue();
    assertThat(parent.put("str", stringTypeModel, "String")).isFalse();
  }

  @Test
  public void testScopeTableGetTypeModel() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(parent.put("str", stringTypeModel, "String")).isTrue();
    assertThat(parent.getTypeModel("str")).isEqualTo(stringTypeModel);
    assertThat(parent.getTypeName("str")).isEqualTo("String");

    assertThat(parent.getTypeModel("book")).isNull();
    assertThat(parent.getTypeName("book")).isNull();
  }

  @Test
  public void testScopeTablePutAndGetResourceName() {
    assertThat(parent.put("book", null, "ShelfBookName")).isTrue();
    assertThat(parent.getTypeModel("book")).isEqualTo(null);
    assertThat(parent.getTypeName("book")).isEqualTo("ShelfBookName");
  }

  @Test
  public void testScopeTableGetFromParent() {
    TypeModel stringTypeModel = ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
    assertThat(parent.put("str", stringTypeModel, "String")).isTrue();
    assertThat(parent.put("book", null, "ShelfBookName")).isTrue();

    assertThat(child.getTypeModel("str")).isEqualTo(stringTypeModel);
    assertThat(child.getTypeName("str")).isEqualTo("String");
    assertThat(child.getTypeModel("book")).isEqualTo(null);
    assertThat(child.getTypeName("book")).isEqualTo("ShelfBookName");
  }
}
