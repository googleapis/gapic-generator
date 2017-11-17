/* Copyright 2016 Google LLC
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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.transformer.ModelTypeNameConverterTestUtil;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.tools.framework.model.EnumValue;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.truth.Truth;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class GoModelTypeNameConverterTest {

  @ClassRule public static TemporaryFolder tempDir = new TemporaryFolder();

  private static final GoModelTypeNameConverter converter = new GoModelTypeNameConverter();

  @Test
  public void testGetTypeName() {
    boolean isPointerTrue = true;
    boolean isPointerFalse = false;

    // Specified import path, use the path.
    Truth.assertThat(
            converter
                .getTypeName("google.golang.org/genproto/zip/zap", "Baz", isPointerFalse)
                .getNickname())
        .isEqualTo("zappb.Baz");

    // Also specified the import name, use it.
    Truth.assertThat(
            converter
                .getTypeName("google.golang.org/genproto/zip/zap;smack", "Baz", isPointerFalse)
                .getNickname())
        .isEqualTo("smackpb.Baz");
  }

  @Test
  public void testGetEnumValue() {
    TypeRef type = ModelTypeNameConverterTestUtil.getTestEnumType(tempDir);
    EnumValue value = type.getEnumType().getValues().get(0);
    Truth.assertThat(
            converter.getEnumValue(type, value).getValueAndSaveTypeNicknameIn(new GoTypeTable()))
        .isEqualTo("librarypb.Book_GOOD");

    type = ModelTypeNameConverterTestUtil.getTestType(tempDir, "TopLevelEnum");
    value = type.getEnumType().getValues().get(0);
    Truth.assertThat(
            converter.getEnumValue(type, value).getValueAndSaveTypeNicknameIn(new GoTypeTable()))
        .isEqualTo("librarypb.TopLevelEnum_FOO");
  }
}
