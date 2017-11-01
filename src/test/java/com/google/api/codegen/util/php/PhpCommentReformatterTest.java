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
package com.google.api.codegen.util.php;

import com.google.common.collect.ImmutableList;
import com.google.common.truth.Truth;
import java.util.List;
import org.junit.Test;

public class PhpCommentReformatterTest {

  @Test
  public void testPhpAtSymbolRegex() {
    List<String> matches =
        ImmutableList.<String>builder()
            .add("some text wi@th at symbol")
            .add("@starting with at symbol")
            .add("ending with at symbol@")
            .add("@")
            .add("surrounded by @ whitespace")
            .add("almost a valid tag @seeb but not quite")
            .build();
    List<String> noMatches =
        ImmutableList.<String>builder().add("no at symbol").add("with @see tag").build();

    for (String m : matches) {
      Truth.assertWithMessage(m)
          .that(PhpCommentReformatter.AT_SYMBOL_PATTERN.matcher(m).find())
          .isTrue();
    }

    for (String m : noMatches) {
      Truth.assertWithMessage(m)
          .that(PhpCommentReformatter.AT_SYMBOL_PATTERN.matcher(m).find())
          .isFalse();
    }
  }
}
