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
package com.google.api.codegen.util.ruby;

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RubyCommentReformatterTest {

  @Test
  public void testBulletList_whenTight() throws Exception {
    String comment =
        new StringBuilder()
            .append("A list of items:")
            .append(System.lineSeparator())
            .append("* List item 1")
            .append(System.lineSeparator())
            .append("* List item 2")
            .append(System.lineSeparator())
            .append("  * which has a nested item")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End list.")
            .toString();

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment =
        new StringBuilder()
            .append("A list of items:")
            .append(System.lineSeparator())
            .append("* List item 1")
            .append(System.lineSeparator())
            .append("* List item 2")
            .append(System.lineSeparator())
            .append("  * which has a nested item")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End list.")
            .toString();
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testBulletList_whenLoose() throws Exception {
    String comment =
        new StringBuilder()
            .append("A list of items:")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 1")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 2")
            .append(System.lineSeparator())
            .append("  * which has a nested item")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 3")
            .append(System.lineSeparator())
            .append("  which continues on a second line")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 4")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("      which has a nested code block")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End list.")
            .toString();

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment =
        new StringBuilder()
            .append("A list of items:")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 1")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 2")
            .append(System.lineSeparator())
            .append("  * which has a nested item")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 3")
            .append(System.lineSeparator())
            .append("  which continues on a second line")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("* List item 4")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("      which has a nested code block")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End list.")
            .toString();
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testCode() throws Exception {
    String comment = "Some `inline code` here.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "Some `inline code` here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testEmphasis() throws Exception {
    String comment = "An *emphasized* statement.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "An *emphasized* statement.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testHtmlInline() throws Exception {
    String comment = "Some <span>inline html</span> here.";

    String formatted = new RubyCommentReformatter().reformat(comment);

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

    String formatted = new RubyCommentReformatter().reformat(comment);

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
  public void testParagraph() throws Exception {
    String comment =
        new StringBuilder()
            .append("A paragraph.")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("A second paragraph.")
            .toString();

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment =
        new StringBuilder()
            .append("A paragraph.")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("A second paragraph.")
            .toString();
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testHeading() throws Exception {
    String comment =
        new StringBuilder()
            .append("# A page title")
            .append(System.lineSeparator())
            .append("Some intro text")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("## A section heading")
            .append(System.lineSeparator())
            .append("Some section text")
            .toString();

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment =
        new StringBuilder()
            .append("= A page title")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("Some intro text")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("== A section heading")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("Some section text")
            .toString();
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testIndentedCodeBlock() throws Exception {
    String comment =
        new StringBuilder()
            .append("A block of code:")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("    line 1")
            .append(System.lineSeparator())
            .append("    line 2")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End code block.")
            .toString();

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment =
        new StringBuilder()
            .append("A block of code:")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("    line 1")
            .append(System.lineSeparator())
            .append("    line 2")
            .append(System.lineSeparator())
            .append(System.lineSeparator())
            .append("End code block.")
            .toString();
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenProtoLinkWithEmptyDestination() throws Exception {
    String comment = "A [Shelf][] message.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "A {Shelf} message.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenProtoLinkWithDestinationAsTitle() throws Exception {
    String comment =
        "A [google.example.library.v1.Shelf][google.example.library.v1.Shelf] message.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "A {Google::Example::Library::V1::Shelf} message.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenProtoLinkWithMessageDestination() throws Exception {
    String comment = "A [Shelf][google.example.library.v1.Shelf] message.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "A {Google::Example::Library::V1::Shelf Shelf} message.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenProtoLinkWithNestedMessageDestination() throws Exception {
    String comment =
        "A [CreateShelfRequest.Shelf][google.example.library.v1.CreateShelfRequest.Shelf] message.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment =
        "A {Google::Example::Library::V1::CreateShelfRequest::Shelf CreateShelfRequest::Shelf} message.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenProtoLinkWithFieldDestination() throws Exception {
    String comment = "A [name][google.example.library.v1.Shelf.name] field.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "A {Google::Example::Library::V1::Shelf#name name} field.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenProtoLinkWithSpecialCharInTitle() throws Exception {
    String comment = "A [Shelf$][google.example.library.v1.Shelf] message.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "A {Google::Example::Library::V1::Shelf Shelf$} message.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenRelativeLink() throws Exception {
    String comment = "Some [cloud docs!](/example/link) here.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "Some [cloud docs!](https://cloud.google.com/example/link) here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenRelativeLinkWithSpecialCharInDestination() throws Exception {
    String comment = "Some [cloud docs!](/example/link$) here.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "Some [cloud docs!](https://cloud.google.com/example/link$) here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenAbsoluteLink() throws Exception {
    String comment = "Some [linked text](http://www.google.com) here.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "Some [linked text](http://www.google.com) here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }

  @Test
  public void testLink_whenAbsoluteLinkWithSpecialCharInDestination() throws Exception {
    String comment = "Some [linked text](http://www.google.com$) here.";

    String formatted = new RubyCommentReformatter().reformat(comment);

    String expectedComment = "Some [linked text](http://www.google.com$) here.";
    assertThat(formatted).isEqualTo(expectedComment);
  }
}
