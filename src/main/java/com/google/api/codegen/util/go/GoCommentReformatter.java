/* Copyright 2017 Google Inc
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
package com.google.api.codegen.util.go;

import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.ErrorMarkdownVisitor;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

public class GoCommentReformatter implements CommentReformatter {

  private static final Logger LOGGER = Logger.getLogger(GoCommentReformatter.class.getName());

  // Might as well create only one. Parser is thread-safe.
  private static final Parser PARSER = Parser.builder().build();

  @Override
  public String reformat(String comment) {
    Node root = PARSER.parse(comment);
    GoVisitor visitor = new GoVisitor();
    try {
      root.accept(visitor);
      return visitor.toString();
    } catch (ErrorMarkdownVisitor.UnimplementedRenderException e) {
      LOGGER.log(
          Level.WARNING, "markdown contains elements we don't handle; copying doc verbatim", e);
      return comment;
    }
  }

  private static class GoVisitor extends ErrorMarkdownVisitor {
    StringBuilder stringBuilder = new StringBuilder();
    int indentLevel = 0;

    @Override
    public String toString() {
      return stringBuilder.toString().trim();
    }

    @Override
    public void visit(BulletList bulletList) {
      indentLevel++;
      printIndent();
      visitChildren(bulletList);
      indentLevel--;
    }

    @Override
    public void visit(Code code) {
      stringBuilder.append(code.getLiteral());
      visitChildren(code);
    }

    @Override
    public void visit(Document document) {
      visitChildren(document);
    }

    @Override
    public void visit(Emphasis emphasis) {
      visitChildren(emphasis);
    }

    @Override
    public void visit(HtmlInline htmlInline) {
      stringBuilder.append(htmlInline.getLiteral());
      visitChildren(htmlInline);
    }

    @Override
    public void visit(HtmlBlock htmlBlock) {
      stringBuilder.append(htmlBlock.getLiteral());
      visitChildren(htmlBlock);
    }

    @Override
    public void visit(Link link) {
      // The text is in the child.
      visitChildren(link);
      stringBuilder.append(" (at ");
      stringBuilder.append(link.getDestination());
      stringBuilder.append(")");
    }

    @Override
    public void visit(ListItem listItem) {
      visitChildren(listItem);
    }

    @Override
    public void visit(Paragraph paragraph) {
      stringBuilder.append("\n\n");
      printIndent();
      visitChildren(paragraph);
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
      stringBuilder.append("\n");
      printIndent();
      visitChildren(softLineBreak);
    }

    @Override
    public void visit(Text text) {
      stringBuilder.append(text.getLiteral());
      visitChildren(text);
    }

    private void printIndent() {
      for (int i = 0; i < indentLevel; i++) {
        stringBuilder.append("  ");
      }
    }
  }
}
