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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.config.FieldModel;
import com.google.api.tools.framework.model.Oneof;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;
import org.mockito.Mockito;

public class TestCaseTransformerTest {
  @Test
  public void testResponseOneof() {
    Oneof oneof1 = Mockito.mock(Oneof.class);
    Oneof oneof2 = Mockito.mock(Oneof.class);

    FieldModel field1 = Mockito.mock(FieldModel.class);
    Mockito.when(field1.isPrimitive()).thenReturn(true);
    Mockito.when(field1.isRepeated()).thenReturn(false);
    Mockito.when(field1.getOneof()).thenReturn(oneof1);

    FieldModel field2 = Mockito.mock(FieldModel.class);
    Mockito.when(field2.isPrimitive()).thenReturn(true);
    Mockito.when(field2.isRepeated()).thenReturn(false);
    Mockito.when(field2.getOneof()).thenReturn(oneof1);

    // not equal, even if they have the same properties; otherwise "containsExactly" below doesn't
    // work.
    assertThat(field1).isNotEqualTo(field2);

    FieldModel field3 = Mockito.mock(FieldModel.class);
    Mockito.when(field3.isPrimitive()).thenReturn(true);
    Mockito.when(field3.isRepeated()).thenReturn(false);
    Mockito.when(field3.getOneof()).thenReturn(oneof2);

    List<FieldModel> fields;

    fields = Arrays.asList(field1);
    assertThat(TestCaseTransformer.responseInitFields(fields)).containsExactly(field1);

    // field1 and field2 have the same oneof, we can only choose one. First one wins.
    fields = Arrays.asList(field1, field2);
    assertThat(TestCaseTransformer.responseInitFields(fields)).containsExactly(field1);
    fields = Arrays.asList(field2, field1);
    assertThat(TestCaseTransformer.responseInitFields(fields)).containsExactly(field2);

    // field3 has a different oneof.
    fields = Arrays.asList(field1, field3);
    assertThat(TestCaseTransformer.responseInitFields(fields)).containsExactly(field1, field3);
    fields = Arrays.asList(field1, field2, field3);
    assertThat(TestCaseTransformer.responseInitFields(fields)).containsExactly(field1, field3);
  }
}
