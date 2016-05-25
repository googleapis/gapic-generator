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
package com.google.api.codegen.metacode;

import com.google.api.codegen.metacode.FieldStructureParser;
import com.google.common.truth.Truth;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

public class FieldStructureTest {

  @Test
  public void testSimpleField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield");

    Map<String, Object> expectedStructure = Collections.singletonMap("myfield", (Object)InitValueConfig.create());

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testEmbeddedField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myobj.myfield");

    Map<String, Object> innerStructure = Collections.singletonMap("myfield", (Object)InitValueConfig.create());
    Map<String, Object> expectedStructure = Collections.singletonMap("myobj", (Object)innerStructure);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testListField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]");

    List<Object> innerList = Collections.singletonList((Object)InitValueConfig.create());
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object)innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testListFieldBadIndex() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[1]");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testListFieldIndexGap() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]", "mylist[2]");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testListFieldMismatchedListThenField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield[0]", "myfield.subfield");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test(expected=IllegalArgumentException.class)
  public void testListFieldMismatchedFieldThenList() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield.subfield", "myfield[0]");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test
  public void testListEmbeddedField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0].myfield");

    Map<String, Object> innerStructure = Collections.singletonMap("myfield", (Object)InitValueConfig.create());
    List<Object> innerList = Collections.singletonList((Object)innerStructure);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object)innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testMultipleFields() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield", "secondfield");

    Map<String, Object> expectedStructure = new HashMap<>();
    expectedStructure.put("myfield", InitValueConfig.create());
    expectedStructure.put("secondfield", InitValueConfig.create());

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testMultipleListEntries() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]", "mylist[1]");

    List<Object> innerList = Arrays.asList((Object)InitValueConfig.create(), InitValueConfig.create());
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object)innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testListEmbeddedMultipleFields() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0].myfield", "mylist[0].secondfield");

    Map<String, Object> innerStructure = new HashMap<>();
    innerStructure.put("myfield", InitValueConfig.create());
    innerStructure.put("secondfield", InitValueConfig.create());

    List<Object> innerList = Collections.singletonList((Object)innerStructure);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object)innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testCompoundingStructure() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myobj", "myobj.myfield");

    Map<String, Object> innerStructure = Collections.singletonMap("myfield", (Object)InitValueConfig.create());
    Map<String, Object> expectedStructure = Collections.singletonMap("myobj", (Object)innerStructure);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testCompoundingStructureList() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist", "mylist[0]", "mylist[0].subfield");

    Map<String, Object> innerStructure = Collections.singletonMap("subfield", (Object)InitValueConfig.create());
    List<Object> innerList = Collections.singletonList((Object)innerStructure);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object)innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }
}
