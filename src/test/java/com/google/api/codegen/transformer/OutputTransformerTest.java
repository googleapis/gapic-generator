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

  private static final TypeModel stringTypeModel =
      ProtoTypeRef.create(TypeRef.fromPrimitiveName("string"));
  private static final String stringTypeName = "String";
  private static final String stringVariable = "str";

  private static final String resourceNameTypeName = "ShelfBookName";
  private static final String resourceNameVariable = "book";

  @Before
  public void setUp() {
    outer = new ScopeTable();
    inner = new ScopeTable(outer);
  }

  @Test
  public void testScopeTablePut() {
    assertThat(outer.put(stringVariable, stringTypeModel, stringTypeName)).isTrue();
  }

  @Test
  public void testScopeTablePutFail() {
    assertThat(outer.put(stringVariable, stringTypeModel, stringTypeName)).isTrue();
    assertThat(outer.put(stringVariable, stringTypeModel, stringTypeName)).isFalse();
  }

  @Test
  public void testScopeTableGetTypeModel() {
    assertThat(outer.put(stringVariable, stringTypeModel, stringTypeName)).isTrue();
    assertThat(outer.getTypeModel(stringVariable)).isEqualTo(stringTypeModel);
    assertThat(outer.getTypeName(stringVariable)).isEqualTo(stringTypeName);
    
    assertThat(outer.getTypeModel(resourceNameVariable)).isNull();
    assertThat(outer.getTypeName(resourceNameVariable)).isNull();
  }

  @Test
  public void testScopeTablePutAndGetResourceName() {
    assertThat(outer.put(resourceNameVariable, null, resourceNameTypeName)).isTrue();
    assertThat(outer.getTypeModel(resourceNameVariable)).isEqualTo(null);
    assertThat(outer.getTypeName(resourceNameVariable)).isEqualTo(resourceNameTypeName);
  }

  @Test
  public void testScopeTableGetFromParent() {
    assertThat(outer.put(stringVariable, stringTypeModel, stringTypeName)).isTrue();
    assertThat(outer.put(resourceNameVariable, null, resourceNameTypeName)).isTrue();
    
    assertThat(inner.getTypeModel(stringVariable)).isEqualTo(stringTypeModel);
    assertThat(inner.getTypeName(stringVariable)).isEqualTo(stringTypeName);
    assertThat(inner.getTypeModel(resourceNameVariable)).isEqualTo(null);
    assertThat(inner.getTypeName(resourceNameVariable)).isEqualTo(resourceNameTypeName);
  }
}
