/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen;

import static com.google.common.base.CharMatcher.whitespace;

import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.api.tools.framework.util.VisitsBefore;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import java.util.ArrayList;
import java.util.List;

/** Generates the text of the gapic yaml file from a ConfigNode representation. */
public class ConfigGenerator extends NodeVisitor {
  private static final int MAX_LINE_WIDTH = 78;

  private static final int TAB_WIDTH = 2;

  private final int indent;

  private final StringBuilder configBuilder = new StringBuilder();

  public ConfigGenerator(int indent) {
    this.indent = indent;
  }

  @VisitsBefore
  void generate(FieldConfigNode node) {
    appendComment(node.getComment().generate());
    appendIndent();

    if (node.getText().isEmpty()) {
      configBuilder.append(visitFieldValue(indent, node.getChild()).trim());
    } else {
      configBuilder
          .append(node.getText())
          .append(":")
          .append(visitFieldValue(indent + TAB_WIDTH, node.getChild()));
    }
  }

  @VisitsBefore
  void generate(ListItemConfigNode node) {
    appendComment(node.getComment().generate());
    ConfigGenerator childGenerator = new ConfigGenerator(indent);
    childGenerator.visit(node.getChild());
    String child = childGenerator.toString().trim();
    boolean isFirst = true;
    for (String line : Splitter.on(System.lineSeparator()).split(child)) {
      if (!isFirst) {
        configBuilder.append(whitespace().trimTrailingFrom(line));
      } else if (line.trim().startsWith("#")) {
        appendIndent().append(whitespace().trimTrailingFrom(line));
      } else {
        configBuilder
            .append(Strings.repeat(" ", indent - TAB_WIDTH))
            .append("- ")
            .append(line.trim());
        isFirst = false;
      }
      configBuilder.append(System.lineSeparator());
    }
  }

  @VisitsBefore
  void generate(ScalarConfigNode node) {
    appendIndent().append(node.getText()).append(System.lineSeparator());
  }

  private void appendComment(String comment) {
    if (comment.isEmpty()) {
      return;
    }

    for (String commentLine : Splitter.on("\n").split(comment)) {
      int startIndex = whitespace().negate().indexIn(commentLine);
      if (startIndex < 0) {
        appendIndent().append("#").append(System.lineSeparator());
        continue;
      }

      for (String line : breakLine(commentLine.trim(), MAX_LINE_WIDTH - indent - startIndex)) {
        appendIndent().append("# ");
        appendIndent(startIndex).append(line).append(System.lineSeparator());
      }
    }
  }

  private List<String> breakLine(String line, int maxWidth) {
    List<String> lines = new ArrayList<>();
    while (line.length() > maxWidth) {
      int split = lineWrapIndex(line, maxWidth);
      lines.add(line.substring(0, split).trim());
      line = line.substring(split).trim();
    }
    lines.add(line.trim());
    return lines;
  }

  private int lineWrapIndex(String line, int maxWidth) {
    int index = whitespace().lastIndexIn(line.substring(0, maxWidth + 1));
    if (index >= 0) {
      return index;
    }

    index = whitespace().indexIn(line, maxWidth);
    if (index >= 0) {
      return index;
    }

    return line.length();
  }

  private StringBuilder appendIndent() {
    return appendIndent(indent);
  }

  private StringBuilder appendIndent(int indent) {
    return configBuilder.append(Strings.repeat(" ", indent));
  }

  private String visitFieldValue(int indent, ConfigNode childNode) {
    if (!childNode.isPresent()) {
      return String.format(" []%n");
    }

    ConfigGenerator childGenerator = new ConfigGenerator(indent);
    childGenerator.visit(childNode);
    String child = childGenerator.toString();
    if (childNode instanceof ScalarConfigNode) {
      return String.format(" %s%n", child.trim());
    }

    return System.lineSeparator() + child;
  }

  @Override
  public String toString() {
    return configBuilder.toString();
  }
}
