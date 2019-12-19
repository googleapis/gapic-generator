package com.google.api.codegen.bazel;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class BazelBuildFileTemplate {
  private static final Pattern TEMPLATE_TOKEN = Pattern.compile("\\{\\{(.+?)}}");

  private final String template;

  BazelBuildFileTemplate(String template) {
    this.template = template;
  }

  private String expand(Map<String, String> tokens) {
    Matcher m = TEMPLATE_TOKEN.matcher(template);

    StringBuilder builder = new StringBuilder();
    int index = 0;
    while (m.find()) {
      String replacement = tokens.get(m.group(1));
      builder.append(template, index, m.start());
      if (replacement == null) {
        builder.append(m.group(0));
      } else {
        builder.append(replacement);
      }
      index = m.end();
    }
    builder.append(template.substring(index));
    return builder.toString();
  }

  String expand(BazelBuildFileView bpv) {
    return this.expand(bpv.getTokens());
  }
}
