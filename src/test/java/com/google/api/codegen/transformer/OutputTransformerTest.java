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

import static com.google.api.codegen.transformer.OutputTransformer.accessorNewVariable;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.util.Scanner;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.tools.framework.model.TypeRef;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class OutputTransformerTest {

  private OutputTransformer.ScopeTable parent;
  private OutputTransformer.ScopeTable child;
  private SampleValueSet valueSet;

  @Mock private MethodModel model;
  @Mock private MethodContext context;
  @Mock private MethodConfig config;
  @Mock private PageStreamingConfig pageStreamingConfig;
  @Mock private FeatureConfig featureConfig;
  @Mock private SurfaceNamer namer;
  @Mock private FieldConfig resourceFieldConfig;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    valueSet = SampleValueSet.newBuilder().setId("test-sample-value-set-id").build();
    parent = new OutputTransformer.ScopeTable();
    child = new OutputTransformer.ScopeTable(parent);
  }

  @Test
  public void testAccessorNewVariableResourceName() {
    String input = "$resp";
    Scanner scanner = new Scanner(input);
    String newVar = "variable";
    when(context.getMethodConfig()).thenReturn(config);
    when(context.getMethodModel()).thenReturn(model);
    when(context.getFeatureConfig()).thenReturn(featureConfig);
    when(model.getSimpleName()).thenReturn("methodSimpleName");
    when(config.getPageStreaming()).thenReturn(pageStreamingConfig);
    when(pageStreamingConfig.getResourcesFieldConfig()).thenReturn(resourceFieldConfig);
    when(context.getNamer()).thenReturn(namer);
    when(namer.getSampleResponseVarName(any(MethodContext.class))).thenReturn("variable");
    when(featureConfig.useResourceNameFormatOption(any(FieldConfig.class))).thenReturn(true);
    OutputView.VariableView variableView =
        accessorNewVariable(scanner, context, valueSet, parent, newVar, false);
    System.out.println(variableView);
  }

  @Test
  public void testAccessorNewVariablePageStreaming() {}

  @Test
  public void testAccessorNewVariableResponseType() {}

  @Test
  public void testAccessorNewVariableFromScopeTable() {}

  @Test
  public void testResponseResourceNameType() {}

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
