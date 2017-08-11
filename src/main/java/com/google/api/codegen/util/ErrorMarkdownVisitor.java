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

import org.commonmark.node.BlockQuote;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.CustomBlock;
import org.commonmark.node.CustomNode;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.FencedCodeBlock;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.Image;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.OrderedList;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.StrongEmphasis;
import org.commonmark.node.Text;
import org.commonmark.node.ThematicBreak;
import org.commonmark.node.Visitor;

/**
 * A dummy implementation of {@code Visitor} that always throws Exception.
 *
 * <p>The exception is always {@code UnimplementedRenderException}. We use this instead of {@link
 * UnsupportedOperationException} so we we don't confuse it with exceptions we get trying to add to
 * an unmodifiable list, etc.
 */
public class ErrorMarkdownVisitor implements Visitor {

  public static class UnimplementedRenderException extends RuntimeException {
    UnimplementedRenderException(String val) {
      super(val);
    }
  }

  @Override
  public void visit(BlockQuote blockQuote) {
    throw new UnimplementedRenderException("BlockQuote");
  }

  @Override
  public void visit(BulletList bulletList) {
    throw new UnimplementedRenderException("BulletList");
  }

  @Override
  public void visit(Code code) {
    throw new UnimplementedRenderException("Code");
  }

  @Override
  public void visit(Document document) {
    throw new UnimplementedRenderException("Document");
  }

  @Override
  public void visit(Emphasis emphasis) {
    throw new UnimplementedRenderException("Emphasis");
  }

  @Override
  public void visit(FencedCodeBlock fencedCodeBlock) {
    throw new UnimplementedRenderException("FencedCodeBlock");
  }

  @Override
  public void visit(HardLineBreak hardLineBreak) {
    throw new UnimplementedRenderException("HardLineBreak");
  }

  @Override
  public void visit(Heading heading) {
    throw new UnimplementedRenderException("Heading");
  }

  @Override
  public void visit(ThematicBreak thematicBreak) {
    throw new UnimplementedRenderException("ThematicBreak");
  }

  @Override
  public void visit(HtmlInline htmlInline) {
    throw new UnimplementedRenderException("HtmlInline");
  }

  @Override
  public void visit(HtmlBlock htmlBlock) {
    throw new UnimplementedRenderException("HtmlBlock");
  }

  @Override
  public void visit(Image image) {
    throw new UnimplementedRenderException("Image");
  }

  @Override
  public void visit(IndentedCodeBlock indentedCodeBlock) {
    throw new UnimplementedRenderException("IndentedCodeBlock");
  }

  @Override
  public void visit(Link link) {
    throw new UnimplementedRenderException("Link");
  }

  @Override
  public void visit(ListItem listItem) {
    throw new UnimplementedRenderException("ListItem");
  }

  @Override
  public void visit(OrderedList orderedList) {
    throw new UnimplementedRenderException("OrderedList");
  }

  @Override
  public void visit(Paragraph paragraph) {
    throw new UnimplementedRenderException("Paragraph");
  }

  @Override
  public void visit(SoftLineBreak softLineBreak) {
    throw new UnimplementedRenderException("SoftLineBreak");
  }

  @Override
  public void visit(StrongEmphasis strongEmphasis) {
    throw new UnimplementedRenderException("StrongEmphasis");
  }

  @Override
  public void visit(Text text) {
    throw new UnimplementedRenderException("Text");
  }

  @Override
  public void visit(CustomBlock customBlock) {
    throw new UnimplementedRenderException("CustomBlock");
  }

  @Override
  public void visit(CustomNode customNode) {
    throw new UnimplementedRenderException("CustomNode");
  }

  protected void visitChildren(Node node) {
    Node child = node.getFirstChild();
    while (child != null) {
      Node next = child.getNext();
      child.accept(this);
      child = next;
    }
  }
}
