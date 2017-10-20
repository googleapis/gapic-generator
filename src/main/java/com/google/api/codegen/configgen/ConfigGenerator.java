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
package com.google.api.codegen.configgen;

import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ListItemConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.api.tools.framework.util.VisitsBefore;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;

public class ConfigGenerator extends NodeVisitor {
  private static final int MAX_WIDTH = 78;
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
          .append(visitFieldValue(indent + 2, node.getChild()));
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
        configBuilder.append(CharMatcher.whitespace().trimTrailingFrom(line));
      } else if (line.trim().startsWith("# ")) {
        appendIndent().append(CharMatcher.whitespace().trimTrailingFrom(line));
      } else {
        configBuilder.append(Strings.repeat(" ", indent - 2)).append("- ").append(line.trim());
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

    int maxWidth = MAX_WIDTH - indent;
    for (String line : Splitter.on("\n").split(comment)) {
      while (line.length() > maxWidth) {
        int split = lineWrapIndex(line, maxWidth);
        appendIndent().append("# ").append(line.substring(0, split)).append(System.lineSeparator());
        line = line.substring(split + 1);
      }
      appendIndent().append("# ").append(line).append(System.lineSeparator());
    }
  }

  private int lineWrapIndex(String line, int maxWidth) {
    for (int i = maxWidth; i > 0; i--) {
      if (Character.isWhitespace(line.charAt(i))) {
        return i;
      }
    }
    for (int i = maxWidth + 1; i < line.length(); i++) {
      if (Character.isWhitespace(line.charAt(i))) {
        return i;
      }
    }
    return line.length();
  }

  private StringBuilder appendIndent() {
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
