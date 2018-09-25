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
package com.google.api.codegen.metacode;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth8.assertThat;

import com.google.api.codegen.CodegenTestUtil;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.model.testing.TestConfig;
import com.google.api.tools.framework.model.testing.TestDataLocator;
import com.google.api.tools.framework.setup.StandardSetup;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class SampleInitCodeTest {

  @ClassRule
  public static TemporaryFolder tempDir = new TemporaryFolder();

  private static TestDataLocator testDataLocator;
  private static TestConfig testConfig;
  private static Model model;
  private static Interface apiInterface;
  private static Method method;

  @BeforeClass
  public static void setupClass() {
    List<String> protoFiles = Lists.newArrayList("myproto.proto");
    List<String> yamlFiles = Lists.newArrayList("myproto.yaml");
    testDataLocator = TestDataLocator.create(SampleInitCodeTest.class);
    testConfig = new TestConfig(testDataLocator, tempDir.getRoot().getPath(), protoFiles);
    model = testConfig.createModel(yamlFiles);
    StandardSetup.registerStandardProcessors(model);
    StandardSetup.registerStandardConfigAspects(model);
    model.establishStage(Merged.KEY);
    apiInterface = model.getSymbolTable().getInterfaces().asList().get(0);
    method = apiInterface.getMethods().get(0);
  }

  private InitCodeContext.Builder getContextBuilder() {
    return InitCodeContext.newBuilder()
        .symbolTable(new SymbolTable())
        .initObjectType(ProtoTypeRef.create(method.getInputType()))
        .suggestedName(Name.from("request"));
  }

  // TODO(pongad): Consider using createTree everywhere.

  @Test
  public void testSimpleField() throws Exception {
    String fieldSpec = "myfield";

    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(InitCodeNode.create(fieldSpec));

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testEmbeddedField() throws Exception {
    String fieldSpec = "myobj.myfield";

    InitCodeNode innerStructure = InitCodeNode.create("myfield");
    InitCodeNode outerStructure =
        InitCodeNode.createWithChildren(
            "myobj", InitCodeLineType.StructureInitLine, innerStructure);
    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testListField() throws Exception {
    String fieldSpec = "mylist[0]";

    InitCodeNode innerStructure = InitCodeNode.create("0");
    InitCodeNode outerStructure =
        InitCodeNode.createWithChildren("mylist", InitCodeLineType.ListInitLine, innerStructure);
    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testMapField() throws Exception {
    String fieldSpec = "mymap{key}";

    InitCodeNode innerStructure = InitCodeNode.create("key");
    InitCodeNode outerStructure =
        InitCodeNode.createWithChildren("mymap", InitCodeLineType.MapInitLine, innerStructure);
    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testNestedListField() throws Exception {
    String fieldSpec = "mylist[0][0]";

    InitCodeNode innerList = InitCodeNode.create("0");
    InitCodeNode outerList =
        InitCodeNode.createWithChildren("0", InitCodeLineType.ListInitLine, innerList);
    InitCodeNode outerStructure =
        InitCodeNode.createWithChildren("mylist", InitCodeLineType.ListInitLine, outerList);
    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testFormattedField() throws Exception {
    String fieldSpec = "name%entity";

    InitValueConfig initValueConfig = InitValueConfig.create("test-api", null);
    InitCodeNode outerStructure = InitCodeNode.createWithValue("name", initValueConfig);

    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(
        actualStructure, fieldSpec, ImmutableMap.of("name", initValueConfig));

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testFormattedFieldWithValues() throws Exception {
    List<String> fieldSpecs =
        Arrays.asList("formatted_field%entity1=test1", "formatted_field%entity2=test2");

    InitValueConfig initValueConfig = InitValueConfig.create("test-api", null);

    InitCodeContext context =
        getContextBuilder()
            .initFieldConfigStrings(fieldSpecs)
            .initValueConfigMap(ImmutableMap.of("formatted_field", initValueConfig))
            .build();
    InitCodeNode actualStructure = InitCodeNode.createTree(context);
    assertThat(actualStructure.getChildren().isEmpty()).isFalse();
    InitCodeNode actualFormattedFieldNode = actualStructure.getChildren().get("formatted_field");
    assertThat(actualFormattedFieldNode.getInitValueConfig().getResourceNameBindingValues())
        .containsExactly(
            "entity1",
            InitValue.createLiteral("test1"),
            "entity2",
            InitValue.createLiteral("test2"));
  }

  @Test
  public void testNestedMixedField() throws Exception {
    String fieldSpec = "mylist[0]{key}";

    InitCodeNode innerMap = InitCodeNode.create("key");
    InitCodeNode innerList =
        InitCodeNode.createWithChildren("0", InitCodeLineType.MapInitLine, innerMap);
    InitCodeNode outerStructure =
        InitCodeNode.createWithChildren("mylist", InitCodeLineType.ListInitLine, innerList);

    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testAssignment() throws Exception {
    String fieldSpec = "myfield=\"default\"";

    InitCodeNode outerStructure =
        InitCodeNode.createWithValue(
            "myfield", InitValueConfig.createWithValue(InitValue.createLiteral("default")));

    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testListEmbeddedField() throws Exception {
    String fieldSpec = "mylist[0].myfield";

    InitCodeNode innerStructure = InitCodeNode.create("myfield");
    InitCodeNode innerList =
        InitCodeNode.createWithChildren("0", InitCodeLineType.StructureInitLine, innerStructure);
    InitCodeNode outerStructure =
        InitCodeNode.createWithChildren("mylist", InitCodeLineType.ListInitLine, innerList);

    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test
  public void testEmbeddedFieldList() throws Exception {
    String fieldSpec = "myfield.mylist[0]";

    InitCodeNode innerList = InitCodeNode.create("0");
    InitCodeNode innerStructure =
        InitCodeNode.createWithChildren("mylist", InitCodeLineType.ListInitLine, innerList);
    InitCodeNode outerStructure =
        InitCodeNode.createWithChildren(
            "myfield", InitCodeLineType.StructureInitLine, innerStructure);

    InitCodeNode expectedStructure = InitCodeNode.newRoot();
    expectedStructure.mergeChild(outerStructure);

    InitCodeNode actualStructure = InitCodeNode.newRoot();
    FieldStructureParser.parse(actualStructure, fieldSpec);

    assertNodeEqual(actualStructure, expectedStructure);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormattedFieldBadField() throws Exception {
    // Error because `name` is not configured to be a resource name field.
    String fieldSpec = "name%entity";
    FieldStructureParser.parse(InitCodeNode.newRoot(), fieldSpec);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testFormattedFieldBadFieldWithValue() throws Exception {
    // Error because `name` is not configured to be a resource name field.
    String fieldSpec = "name%entity=test";
    FieldStructureParser.parse(InitCodeNode.newRoot(), fieldSpec);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldBadIndex() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[1]");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldIndexGap() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]", "mylist[2]");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldMismatchedListThenField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield[0]", "myfield.subfield");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldMismatchedFieldThenList() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield.subfield", "myfield[0]");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("notafield");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testBadSubField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield.notafield");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMapFieldBadStringIndexUnclosedDoubleQuotes() throws Exception {
    List<String> fieldSpecs = Arrays.asList("stringmap{\"key}");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMapFieldBadStringIndexUnclosedSingleQuotes() throws Exception {
    List<String> fieldSpecs = Arrays.asList("stringmap{'key}");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testMapFieldBadIntIndex() throws Exception {
    List<String> fieldSpecs = Arrays.asList("intmap{\"key\"}");
    InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
  }

  @Test
  public void testMultipleFields() throws Exception {
    List<String> fieldSpecs =
        Arrays.asList("mylist", "myfield", "secondfield", "stringmap", "intmap");

    InitCodeNode rootNode =
        InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
    assertThat(rootNode.listInInitializationOrder().stream().map(node -> node.getKey()))
        .containsExactly("mylist", "myfield", "secondfield", "stringmap", "intmap", "root")
        .inOrder();
  }

  @Test
  public void testMultipleListEntries() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]", "mylist[1]");

    InitCodeNode rootNode =
        InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
    assertThat(rootNode.listInInitializationOrder().stream().map(node -> node.getKey()))
        .containsExactly("0", "1", "mylist", "root")
        .inOrder();
  }

  @Test
  public void testMultipleMapEntries() throws Exception {
    List<String> fieldSpecs =
        Arrays.asList("stringmap{\"key1\"}", "stringmap{\"key2\"}", "intmap{123}", "intmap{456}");

    InitCodeNode rootNode =
        InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
    assertThat(rootNode.listInInitializationOrder().stream().map(node -> node.getKey()))
        .containsExactly("key1", "key2", "stringmap", "123", "456", "intmap", "root")
        .inOrder();
  }

  @Test
  public void testMultipleFormattedEntries() throws Exception {
    List<String> fieldSpecs =
        Arrays.asList(
            "formatted_field%entity1", "formatted_field%entity2", "formatted_field%entity3");

    InitValueConfig initValueConfig = InitValueConfig.create("test-api", null);

    InitCodeContext context =
        getContextBuilder()
            .initFieldConfigStrings(fieldSpecs)
            .initValueConfigMap(ImmutableMap.of("formatted_field", initValueConfig))
            .build();
    InitCodeNode rootNode = InitCodeNode.createTree(context);
    List<InitCodeNode> initOrder = rootNode.listInInitializationOrder();
    assertThat(initOrder.stream().map(node -> node.getKey()))
        .containsExactly("formatted_field", "root")
        .inOrder();
  }

  @Test
  public void testListEmbeddedMultipleFields() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0].subfield", "mylist[0].subsecondfield");

    InitCodeNode rootNode =
        InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
    assertThat(rootNode.listInInitializationOrder().stream().map(node -> node.getKey()))
        .containsExactly("subfield", "subsecondfield", "0", "mylist", "root")
        .inOrder();
  }

  @Test
  public void testCompoundingStructure() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield", "myfield.subfield");

    InitCodeNode rootNode =
        InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
    assertThat(rootNode.listInInitializationOrder().stream().map(node -> node.getKey()))
        .containsExactly("subfield", "myfield", "root")
        .inOrder();
  }

  @Test
  public void testCompoundingStructureList() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist", "mylist[0]", "mylist[0].subfield");

    InitCodeNode rootNode =
        InitCodeNode.createTree(getContextBuilder().initFieldConfigStrings(fieldSpecs).build());
    assertThat(rootNode.listInInitializationOrder().stream().map(node -> node.getKey()))
        .containsExactly("subfield", "0", "mylist", "root")
        .inOrder();
  }

  private static void assertNodeEqual(InitCodeNode a, InitCodeNode b) {
    assertThat(a.getKey()).isEqualTo(b.getKey());
    assertThat(a.getLineType()).isEqualTo(b.getLineType());
    assertThat(a.getInitValueConfig()).isEqualTo(b.getInitValueConfig());
    assertThat(a.getType()).isEqualTo(b.getType());
    assertThat(a.getIdentifier()).isEqualTo(b.getIdentifier());

    assertThat(a.getChildren().keySet()).isEqualTo(b.getChildren().keySet());
    for (String key : a.getChildren().keySet()) {
      assertNodeEqual(a.getChildren().get(key), b.getChildren().get(key));
    }
  }
}
