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
package com.google.api.codegen.discovery;

import com.google.api.codegen.DiscoveryContext;
import com.google.common.truth.Truth;
import org.junit.Test;

public class LineWrapTest {
  @Test
  public void testLineWrap() {
    String s;

    // Check for off-by-one's.
    s = "aaa a";
    Truth.assertThat(DiscoveryContext.s_lineWrapDoc(s, s.length() + 2)).containsExactly("* " + s);
    Truth.assertThat(DiscoveryContext.s_lineWrapDoc(s, s.length() + 2 - 1))
        .containsExactly("* aaa", "  a")
        .inOrder();

    // Honor existing newlines.
    s = "abc\nxyz";
    Truth.assertThat(DiscoveryContext.s_lineWrapDoc(s, 10))
        .containsExactly("* abc", "  xyz")
        .inOrder();

    // Break long lines
    s = "aaa 123456789 bbb";
    Truth.assertThat(DiscoveryContext.s_lineWrapDoc(s, 5))
        .containsExactly("* aaa", "  123456789", "  bbb")
        .inOrder();
  }
}
