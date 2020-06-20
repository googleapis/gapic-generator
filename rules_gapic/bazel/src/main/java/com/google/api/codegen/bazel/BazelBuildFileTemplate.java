package com.google.api.codegen.bazel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

  String expand(BazelBuildFileView bpv) throws IOException {
    String expandedTemplate = this.expand(bpv.getTokens());

    // Apply overrides
    Map<String, Map<String, String>> overriddenStringAttributes =
        bpv.getOverriddenStringAttributes();
    Map<String, Map<String, List<String>>> overriddenListAttributes =
        bpv.getOverriddenListAttributes();
    Map<String, String> assemblyPkgRulesNames = bpv.getAssemblyPkgRulesNames();
    if (overriddenStringAttributes.size() == 0
        && overriddenListAttributes.size() == 0
        && assemblyPkgRulesNames.size() == 0) {
      // nothing to override
      return expandedTemplate;
    }

    // write the content of the build file to a temporary directory and fix it with Buildozer
    File tempdir = Files.createTempDirectory("build_file_generator_").toFile();
    File buildBazel = new File(tempdir, "BUILD.bazel");
    Path buildBazelPath = buildBazel.toPath();
    Files.write(buildBazelPath, expandedTemplate.getBytes(StandardCharsets.UTF_8));

    Buildozer buildozer = Buildozer.getInstance();

    // First of all, rename the rules
    for (Map.Entry<String, String> entry : assemblyPkgRulesNames.entrySet()) {
      String kind = entry.getKey();
      String newName = entry.getValue();
      String currentName = buildozer.getAttribute(buildBazelPath, "%" + kind, "name");
      if (!currentName.equals(newName)) {
        buildozer.batchSetAttribute(buildBazelPath, currentName, "name", newName);
      }
    }
    buildozer.commit();

    // Apply preserved string attribute values
    for (Map.Entry<String, Map<String, String>> entry : overriddenStringAttributes.entrySet()) {
      String ruleName = entry.getKey();
      for (Map.Entry<String, String> subentry : entry.getValue().entrySet()) {
        String attr = subentry.getKey();
        String value = subentry.getValue();
        buildozer.batchSetAttribute(buildBazelPath, ruleName, attr, value);
      }
    }
    // Apply preserved list attribute values
    for (Map.Entry<String, Map<String, List<String>>> entry : overriddenListAttributes.entrySet()) {
      String ruleName = entry.getKey();
      for (Map.Entry<String, List<String>> subentry : entry.getValue().entrySet()) {
        String attr = subentry.getKey();
        List<String> values = subentry.getValue();
        buildozer.batchRemoveAttribute(buildBazelPath, ruleName, attr);
        for (String value : values) {
          buildozer.batchAddAttribute(buildBazelPath, ruleName, attr, value);
        }
      }
    }
    buildozer.commit();

    String updatedContent = new String(Files.readAllBytes(buildBazelPath), StandardCharsets.UTF_8);

    buildBazel.delete();
    tempdir.delete();

    return updatedContent;
  }
}
