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
package com.google.api.codegen.util.java;

import static org.junit.Assert.assertEquals;

import java.util.List;
import org.junit.Test;

public class JavaRenderingUtilTest {

  @Test
  public void testGetDocLines() {
    String text =
        "Sample code:\n"
            + "  @Override\n"
            + "  public boolean evaluate() {\n"
            + "    return a < b*c && d > e;\n"
            + "  }";

    List<String> lines = JavaRenderingUtil.getDocLines(text);

    assertEquals(5, lines.size());
    assertEquals("Sample code:", lines.get(0));
    assertEquals("  {@literal @}Override", lines.get(1));
    assertEquals("  public boolean evaluate() {", lines.get(2));
    assertEquals("    return a &lt; b&#42;c &amp;&amp; d &gt; e;", lines.get(3));
    assertEquals("  }", lines.get(4));
  }
}
