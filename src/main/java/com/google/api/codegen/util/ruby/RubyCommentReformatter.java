/* Copyright 2017 Google LLC
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

import com.google.api.codegen.util.CommentReformatter;
import com.google.api.codegen.util.ErrorMarkdownVisitor;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.commonmark.node.BulletList;
import org.commonmark.node.Code;
import org.commonmark.node.Document;
import org.commonmark.node.Emphasis;
import org.commonmark.node.HardLineBreak;
import org.commonmark.node.Heading;
import org.commonmark.node.HtmlBlock;
import org.commonmark.node.HtmlInline;
import org.commonmark.node.IndentedCodeBlock;
import org.commonmark.node.Link;
import org.commonmark.node.ListBlock;
import org.commonmark.node.ListItem;
import org.commonmark.node.Node;
import org.commonmark.node.Paragraph;
import org.commonmark.node.SoftLineBreak;
import org.commonmark.node.Text;
import org.commonmark.parser.Parser;

public class RubyCommentReformatter implements CommentReformatter {

  private static final Logger LOGGER = Logger.getLogger(RubyCommentReformatter.class.getName());

  // Might as well create only one. Parser is thread-safe.
  private static final Parser PARSER = Parser.builder().build();

  private static String CLOUD_URL_PREFIX = "https://cloud.google.com";

  private static final Pattern LINE_SEPARATOR = Pattern.compile("\\v");

  private static final Pattern RELATIVE_LINK_DEST_PATTERN = Pattern.compile("(?!\\p{Alpha}+:).+");

  private static final Pattern PROTO_LINK_PATTERN =
      Pattern.compile("\\[(?<title>[^\\]]+)\\]\\[(?<element>[A-Za-z_][A-Za-z_.0-9]*)?\\]");

  @Override
  public String reformat(String comment) {
    Node root = PARSER.parse(comment);
    RubyVisitor visitor = new RubyVisitor();
    try {
      root.accept(visitor);
      return visitor.toString();
    } catch (ErrorMarkdownVisitor.UnimplementedRenderException e) {
      LOGGER.log(
          Level.WARNING, "markdown contains elements we don't handle; copying doc verbatim", e);
      return comment;
    }
  }

  private static class RubyVisitor extends ErrorMarkdownVisitor {
    StringBuffer sb = new StringBuffer();
    int indentLevel = 0;

    @Override
    public String toString() {
      return sb.toString().trim();
    }

    @Override
    public void visit(BulletList bulletList) {
      ++indentLevel;
      visitChildren(bulletList);
      --indentLevel;
    }

    @Override
    public void visit(Code code) {
      sb.append("`").append(code.getLiteral()).append("`");
      visitChildren(code);
    }

    @Override
    public void visit(Document document) {
      visitChildren(document);
    }

    @Override
    public void visit(Emphasis emphasis) {
      sb.append('*');
      visitChildren(emphasis);
      sb.append('*');
    }

    @Override
    public void visit(HtmlInline htmlInline) {
      sb.append(htmlInline.getLiteral());
      visitChildren(htmlInline);
    }

    @Override
    public void visit(HtmlBlock htmlBlock) {
      sb.append(htmlBlock.getLiteral());
      visitChildren(htmlBlock);
    }

    @Override
    public void visit(Link link) {
      sb.append('[');

      // The text is in the child.
      visitChildren(link);

      sb.append("](");

      if (RELATIVE_LINK_DEST_PATTERN.matcher(link.getDestination()).matches()) {
        sb.append(CLOUD_URL_PREFIX);
      }

      sb.append(link.getDestination()).append(')');
    }

    @Override
    public void visit(ListItem listItem) {
      sb.append('\n');

      // Adjust loose list.
      ListBlock parent = (ListBlock) listItem.getParent();
      if (!parent.isTight()) {
        sb.append('\n');
      }

      // Reduce indent for bullet.
      --indentLevel;
      printIndent();
      sb.append("* ");
      ++indentLevel;

      Node child = listItem.getFirstChild();
      if (child != null) {
        // Skip formatting first Paragraph child.
        visitChildren(child);
        child = child.getNext();
      }

      while (child != null) {
        child.accept(this);
        child = child.getNext();
      }
    }

    @Override
    public void visit(Paragraph paragraph) {
      sb.append("\n\n");
      printIndent();
      visitChildren(paragraph);
    }

    @Override
    public void visit(SoftLineBreak softLineBreak) {
      sb.append('\n');
      printIndent();
      visitChildren(softLineBreak);
    }

    @Override
    public void visit(Text text) {
      Matcher matcher = PROTO_LINK_PATTERN.matcher(text.getLiteral());
      while (matcher.find()) {
        String protoLink =
            getProtoLink(matcher.group("title"), matcher.group("element")).replace("$", "\\$");
        matcher.appendReplacement(sb, protoLink);
      }

      matcher.appendTail(sb);

      visitChildren(text);
    }

    @Override
    public void visit(HardLineBreak hardLineBreak) {
      sb.append('\n');
      printIndent();
      visitChildren(hardLineBreak);
    }

    @Override
    public void visit(Heading heading) {
      sb.append("\n\n");
      IntStream.range(0, heading.getLevel()).forEach(i -> sb.append('='));
      sb.append(' ');
      visitChildren(heading);
    }

    @Override
    public void visit(IndentedCodeBlock indentedCodeBlock) {
      sb.append('\n');
      indentLevel += 2;
      LINE_SEPARATOR
          .splitAsStream(indentedCodeBlock.getLiteral())
          .peek(line -> sb.append('\n'))
          .peek(line -> printIndent())
          .forEach(sb::append);
      visitChildren(indentedCodeBlock);
      indentLevel -= 2;
    }

    private void printIndent() {
      IntStream.range(0, indentLevel).forEach(i -> sb.append("  "));
    }

    private String getProtoLink(String title, String element) {
      if (Strings.isNullOrEmpty(element) || title.equals(element)) {
        return String.format("{%s}", protoToRubyDoc(title));
      }

      return String.format("{%s %s}", protoToRubyDoc(element), protoToRubyDoc(title, false));
    }

    private static String protoToRubyDoc(String protoElement) {
      return protoToRubyDoc(protoElement, true);
    }

    private static String protoToRubyDoc(String protoElement, boolean changeCase) {
      List<String> nameSegments = Splitter.on('.').splitToList(protoElement);
      if (nameSegments.isEmpty()) {
        return protoElement;
      }

      String message =
          nameSegments
              .stream()
              .filter(n -> !n.isEmpty())
              .limit(nameSegments.size() - 1)
              .map(n -> changeCase ? capitalize(n) : n)
              .collect(Collectors.joining("::"));
      String field = nameSegments.get(nameSegments.size() - 1);

      if (field.isEmpty() || field.equals(protoElement)) {
        return protoElement;
      }

      if (Character.isUpperCase(field.charAt(0))) {
        return String.format("%s::%s", message, field);
      }

      return String.format("%s#%s", message, field);
    }

    private static String capitalize(String str) {
      return Character.toUpperCase(str.charAt(0)) + str.substring(1, str.length());
    }
  }
}
