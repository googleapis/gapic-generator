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

import com.google.common.truth.Truth;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FieldStructureTest {

  @Test
  public void testRegex() throws Exception {
    Pattern fieldPattern = FieldStructureParser.getFieldStructurePattern();
    Pattern listPattern = FieldStructureParser.getFieldListPattern();
    Pattern mapPattern = FieldStructureParser.getFieldMapPattern();

    Matcher matcher = listPattern.matcher("mylist[0][0]");
    Truth.assertThat(matcher.matches()).isTrue();
    Truth.assertThat(matcher.group(1)).isEqualTo("mylist[0]");
    Truth.assertThat(matcher.group(2)).isEqualTo("0");

    String dualMatch = "mymap[0]{key}";
    matcher = listPattern.matcher(dualMatch);
    Truth.assertThat(matcher.matches()).isFalse();
    matcher = mapPattern.matcher(dualMatch);
    Truth.assertThat(matcher.matches()).isTrue();
    Truth.assertThat(matcher.group(1)).isEqualTo("mymap[0]");
    Truth.assertThat(matcher.group(2)).isEqualTo("key");

    Matcher fieldMatcher = fieldPattern.matcher("myfield.mynextfield");
    Truth.assertThat(fieldMatcher.matches()).isTrue();
    Truth.assertThat(fieldMatcher.group(1)).isEqualTo("myfield");
    Truth.assertThat(fieldMatcher.group(2)).isEqualTo("mynextfield");

    Truth.assertThat(fieldPattern.matcher("singlefield").matches()).isFalse();
    Truth.assertThat(fieldPattern.matcher("myfield.mylist[0]").matches()).isFalse();
    Truth.assertThat(fieldPattern.matcher("myfield.mymap{key}").matches()).isFalse();
  }

  @Test
  public void testSimpleField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield");

    Map<String, Object> expectedStructure =
        Collections.singletonMap("myfield", (Object) InitValueConfig.create());

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testEmbeddedField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myobj.myfield");

    Map<String, Object> innerStructure =
        Collections.singletonMap("myfield", (Object) InitValueConfig.create());
    Map<String, Object> expectedStructure =
        Collections.singletonMap("myobj", (Object) innerStructure);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testListField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]");

    List<Object> innerList = Collections.singletonList((Object) InitValueConfig.create());
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testMapField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist{key}");

    Map<String, Object> innerMap =
        Collections.singletonMap("key", (Object) InitValueConfig.create());
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) innerMap);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testNestedListField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0][0]");

    List<Object> innerList = Collections.singletonList((Object) InitValueConfig.create());
    List<Object> outerList = Collections.singletonList((Object) innerList);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) outerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testNestedMixedField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]{key}");

    Map<String, Object> innerMap =
        Collections.singletonMap("key", (Object) InitValueConfig.create());
    List<Object> innerList = Collections.singletonList((Object) innerMap);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testAssignment() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield=\"default\"");

    Map<String, Object> expectedStructure =
        Collections.singletonMap(
            "myfield", (Object) InitValueConfig.createWithValue("\"default\""));

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldBadIndex() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[1]");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldIndexGap() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0]", "mylist[2]");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldMismatchedListThenField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield[0]", "myfield.subfield");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testListFieldMismatchedFieldThenList() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield.subfield", "myfield[0]");
    FieldStructureParser.parseFields(fieldSpecs);
  }

  @Test
  public void testListEmbeddedField() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0].myfield");

    Map<String, Object> innerStructure =
        Collections.singletonMap("myfield", (Object) InitValueConfig.create());
    List<Object> innerList = Collections.singletonList((Object) innerStructure);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testEmbeddedFieldList() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myfield.mylist[0]");

    List<Object> innerList = Collections.singletonList((Object) InitValueConfig.create());
    Map<String, Object> innerStructure = Collections.singletonMap("mylist", (Object) innerList);

    Map<String, Object> expectedStructure =
        Collections.singletonMap("myfield", (Object) innerStructure);

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

    List<Object> innerList =
        Arrays.asList((Object) InitValueConfig.create(), InitValueConfig.create());
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testListEmbeddedMultipleFields() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist[0].myfield", "mylist[0].secondfield");

    Map<String, Object> innerStructure = new HashMap<>();
    innerStructure.put("myfield", InitValueConfig.create());
    innerStructure.put("secondfield", InitValueConfig.create());

    List<Object> innerList = Collections.singletonList((Object) innerStructure);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testCompoundingStructure() throws Exception {
    List<String> fieldSpecs = Arrays.asList("myobj", "myobj.myfield");

    Map<String, Object> innerStructure =
        Collections.singletonMap("myfield", (Object) InitValueConfig.create());
    Map<String, Object> expectedStructure =
        Collections.singletonMap("myobj", (Object) innerStructure);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }

  @Test
  public void testCompoundingStructureList() throws Exception {
    List<String> fieldSpecs = Arrays.asList("mylist", "mylist[0]", "mylist[0].subfield");

    Map<String, Object> innerStructure =
        Collections.singletonMap("subfield", (Object) InitValueConfig.create());
    List<Object> innerList = Collections.singletonList((Object) innerStructure);
    Map<String, Object> expectedStructure = Collections.singletonMap("mylist", (Object) innerList);

    Map<String, Object> actualStructure = FieldStructureParser.parseFields(fieldSpecs);
    Truth.assertThat(actualStructure).isEqualTo(expectedStructure);
  }
}
