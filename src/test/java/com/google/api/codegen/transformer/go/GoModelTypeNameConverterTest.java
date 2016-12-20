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
package com.google.api.codegen.transformer.go;

import com.google.common.truth.Truth;
import org.junit.Test;

public class GoModelTypeNameConverterTest {

  private static final GoModelTypeNameConverter converter = new GoModelTypeNameConverter();

  @Test
  public void testGetTypeName() {
    boolean isPointerTrue = true;
    boolean isPointerFalse = false;

    // Not in curated proto. Don't guess anything.
    Truth.assertThat(
            converter
                .getTypeName("github.com/someone/repo", "foo.bar", "Baz", isPointerFalse)
                .getNickname())
        .isEqualTo("foo_bar.Baz");

    // Pointer.
    Truth.assertThat(
            converter
                .getTypeName("github.com/someone/repo", "foo.bar", "Baz", isPointerTrue)
                .getNickname())
        .isEqualTo("*foo_bar.Baz");

    // Curated but no specified import path, guess the import.
    Truth.assertThat(converter.getTypeName("", "foo.bar", "Baz", isPointerFalse).getNickname())
        .isEqualTo("barpb.Baz");

    // Pointer.
    Truth.assertThat(converter.getTypeName("", "foo.bar", "Baz", isPointerTrue).getNickname())
        .isEqualTo("*barpb.Baz");

    // Skip the version.
    Truth.assertThat(converter.getTypeName("", "foo.bar.v1", "Baz", isPointerFalse).getNickname())
        .isEqualTo("barpb.Baz");

    // Curated but specified import path, use the path, ignore proto package.
    Truth.assertThat(
            converter
                .getTypeName("google.golang.org/genproto/zip/zap", "foo.bar", "Baz", isPointerFalse)
                .getNickname())
        .isEqualTo("zappb.Baz");

    // Also specified the import name, use it.
    Truth.assertThat(
            converter
                .getTypeName(
                    "google.golang.org/genproto/zip/zap;smack", "foo.bar", "Baz", isPointerFalse)
                .getNickname())
        .isEqualTo("smackpb.Baz");
  }
}
