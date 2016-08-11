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
package com.google.api.codegen.go;

import com.google.common.truth.Truth;
import com.google.common.base.Joiner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

/**
 * Client config code generator baseline tests.
 */
public class GoContextCommonTest {

  private static GoContextCommon ctx = new GoContextCommon();

  @Test
  public void testLineWrapShort() {
    Truth.assertThat(ctx.getWrappedCommentLines("aaa")).containsExactly("// aaa");
  }

  @Test
  public void testLineWrapLong() {
    List<String> words = Arrays.asList(stringRep("a", 50), stringRep("b", 20), stringRep("c", 3));
    String in = Joiner.on(" ").join(words);
    Truth.assertThat(ctx.getWrappedCommentLines(in))
        .containsExactly("// " + words.get(0) + " " + words.get(1), "// " + words.get(2))
        .inOrder();
  }

  /**
   * Repeats string `s` `n` times.
   */
  private String stringRep(String s, int n) {
    StringBuilder sb = new StringBuilder(s.length() * n);
    for (int i = 0; i < n; i++) {
      sb.append(s);
    }
    return sb.toString();
  }
}
