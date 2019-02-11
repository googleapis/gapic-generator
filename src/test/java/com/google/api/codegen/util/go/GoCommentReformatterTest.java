/* Copyright 2019 Google LLC
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
package com.google.api.codegen.util.go;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class GoCommentReformatterTest {

  @Test
  public void testBulletList() throws Exception {
    String comment =
        new StringBuilder()
            .append("A list of items:")
            .append(System.lineSeparator())
            .append("* List item 1")
            .append(System.lineSeparator())
            .append("* List item 2")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End list.")
            .toString();

    String formatted = new GoCommentReformatter().reformat(comment);

    String expectedComment =
        new StringBuilder()
            .append("A list of items:  ")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("  List item 1")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("  List item 2")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End list.")
            .toString();
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testCode() throws Exception {
    String comment = "Some `inline code` here.";

    String formatted = new GoCommentReformatter().reformat(comment);

    String expectedComment = "Some inline code here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testEmphasis() throws Exception {
    String comment = "An *emphasized* statement.";

    String formatted = new GoCommentReformatter().reformat(comment);

    String expectedComment = "An emphasized statement.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testHtmlInline() throws Exception {
    String comment = "Some <span>inline html</span> here.";

    String formatted = new GoCommentReformatter().reformat(comment);

    String expectedComment = "Some <span>inline html</span> here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testHtmlBlock() throws Exception {
    String comment =
        new StringBuilder()
            .append("A block of html:  ")
            .append(System.lineSeparator())
            .append("<p>")
            .append(System.lineSeparator())
            .append("A paragraph here.")
            .append(System.lineSeparator())
            .append("</p>")
            .append(System.lineSeparator())
            .append("End html block.")
            .toString();

    String formatted = new GoCommentReformatter().reformat(comment);

    String expectedComment =
        new StringBuilder()
            .append("A block of html:<p>")
            .append(System.lineSeparator())
            .append("A paragraph here.")
            .append(System.lineSeparator())
            .append("</p>")
            .append(System.lineSeparator())
            .append("End html block.")
            .toString();
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink() throws Exception {
    String comment = "Some [linked text](https://cloud.google.com) here.";

    String formatted = new GoCommentReformatter().reformat(comment);

    String expectedComment = "Some linked text (at https://cloud.google.com) here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }
}
