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
package com.google.api.codegen.configgen.mergers;

import com.google.api.codegen.configgen.ListTransformer;
import com.google.api.codegen.configgen.MissingFieldTransformer;
import com.google.api.codegen.configgen.NodeFinder;
import com.google.api.codegen.configgen.nodes.ConfigNode;
import com.google.api.codegen.configgen.nodes.FieldConfigNode;
import com.google.api.codegen.configgen.nodes.ScalarConfigNode;
import com.google.api.codegen.configgen.nodes.metadata.DefaultComment;
import com.google.api.codegen.util.VersionMatcher;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.LinkedList;
import java.util.List;

/** Merges the language_settings property from a package into a ConfigNode. */
public class LanguageSettingsMerger {
  private static final String DEFAULT_PACKAGE_SEPARATOR = ".";

  private static final ImmutableList<RewriteRule> JAVA_REWRITE_RULES =
      ImmutableList.of(new RewriteRule("^google(\\.cloud)?", "com.google.cloud"));

  private static final ImmutableList<RewriteRule> COMMON_REWRITE_RULES =
      ImmutableList.of(new RewriteRule("^google(?!\\.cloud)", "google.cloud"));

  private static final ImmutableMap<String, LanguageFormatter> LANGUAGE_FORMATTERS =
      ImmutableMap.<String, LanguageFormatter>builder()
          .put("java", new SimpleLanguageFormatter(".", JAVA_REWRITE_RULES, false))
          .put("python", new PythonLanguageFormatter())
          .put("go", new GoLanguageFormatter())
          .put("csharp", new SimpleLanguageFormatter(".", ImmutableList.of(), true))
          .put("ruby", new SimpleLanguageFormatter("::", COMMON_REWRITE_RULES, true))
          .put("php", new SimpleLanguageFormatter("\\", COMMON_REWRITE_RULES, true))
          .put("nodejs", new NodeJSLanguageFormatter())
          .build();

  public ConfigNode mergeLanguageSettings(
      final String packageName, ConfigNode configNode, ConfigNode prevNode) {
    FieldConfigNode languageSettingsNode =
        MissingFieldTransformer.insert("language_settings", configNode, prevNode).generate();
    if (NodeFinder.hasContent(languageSettingsNode.getChild())) {
      return languageSettingsNode;
    }

    ConfigNode languageSettingsValueNode =
        ListTransformer.generateList(
            LANGUAGE_FORMATTERS.entrySet(),
            languageSettingsNode,
            (startLine, entry) -> {
              ConfigNode languageNode = new FieldConfigNode(startLine, entry.getKey());
              mergeLanguageSetting(languageNode, entry.getValue(), packageName);
              return languageNode;
            });
    return languageSettingsNode
        .setChild(languageSettingsValueNode)
        .setComment(new DefaultComment("The settings of generated code in a specific language."));
  }

  private ConfigNode mergeLanguageSetting(
      ConfigNode languageNode, LanguageFormatter languageFormatter, String packageName) {
    ConfigNode packageNameNode =
        new FieldConfigNode(NodeFinder.getNextLine(languageNode), "package_name");
    languageNode.setChild(packageNameNode);
    mergePackageNameValue(packageNameNode, languageFormatter, packageName);
    return packageNameNode;
  }

  private ConfigNode mergePackageNameValue(
      ConfigNode packageNameNode, LanguageFormatter languageFormatter, String packageName) {
    ConfigNode packageNameValueNode =
        new ScalarConfigNode(
            packageNameNode.getStartLine(), languageFormatter.getFormattedPackageName(packageName));
    packageNameNode.setChild(packageNameValueNode);
    return packageNameValueNode;
  }

  private interface LanguageFormatter {
    String getFormattedPackageName(String packageName);
  }

  private static class SimpleLanguageFormatter implements LanguageFormatter {

    private final String separator;
    private final List<RewriteRule> rewriteRules;
    private final boolean shouldCapitalize;

    public SimpleLanguageFormatter(
        String separator, List<RewriteRule> rewriteRules, boolean shouldCapitalize) {
      this.separator = separator;
      this.rewriteRules = rewriteRules;
      this.shouldCapitalize = shouldCapitalize;
    }

    @Override
    public String getFormattedPackageName(String packageName) {
      for (RewriteRule rewriteRule : rewriteRules) {
        packageName = rewriteRule.rewrite(packageName);
      }
      Iterable<String> elements = Splitter.on(DEFAULT_PACKAGE_SEPARATOR).split(packageName);
      if (shouldCapitalize) {
        elements = Iterables.transform(elements, SimpleLanguageFormatter::capitalize);
      }
      return Joiner.on(separator).join(elements);
    }

    private static String capitalize(String string) {
      return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
  }

  private static class GoLanguageFormatter implements LanguageFormatter {
    public String getFormattedPackageName(String packageName) {
      List<String> nameComponents = Splitter.on(DEFAULT_PACKAGE_SEPARATOR).splitToList(packageName);

      // If the name follows the pattern google.foo.bar.v1234,
      // we reformat it into cloud.google.com.
      // google.logging.v2 => cloud.google.com/go/logging/apiv2
      // Otherwise, fall back to backup
      if (!isApiGoogleCloud(nameComponents)) {
        return "google.golang.org/" + Joiner.on("/").join(nameComponents);
      }
      int startIndex = nameComponents.get(1).equals("cloud") ? 2 : 1;
      int lastIndex = nameComponents.size() - 1;
      return "cloud.google.com/go/"
          + Joiner.on("/").join(nameComponents.subList(startIndex, lastIndex))
          + "/api"
          + nameComponents.get(lastIndex);
    }

    /** Returns true if it is a Google Cloud API. */
    private boolean isApiGoogleCloud(List<String> nameComponents) {
      int size = nameComponents.size();
      return size >= 3
          && nameComponents.get(0).equals("google")
          && nameComponents.get(size - 1).startsWith("v");
    }
  }

  private static class NodeJSLanguageFormatter implements LanguageFormatter {
    @Override
    public String getFormattedPackageName(String packageName) {
      List<String> nameComponents = Splitter.on(DEFAULT_PACKAGE_SEPARATOR).splitToList(packageName);
      return nameComponents.get(nameComponents.size() - 2)
          + "."
          + nameComponents.get(nameComponents.size() - 1);
    }
  }

  private static class RewriteRule {
    private final String pattern;
    private final String replacement;

    public RewriteRule(String pattern, String replacement) {
      this.pattern = pattern;
      this.replacement = replacement;
    }

    public String rewrite(String input) {
      if (pattern == null) {
        return input;
      }
      return input.replaceAll(pattern, replacement);
    }
  }

  private static class PythonLanguageFormatter implements LanguageFormatter {
    @Override
    public String getFormattedPackageName(String packageName) {
      for (RewriteRule rule : COMMON_REWRITE_RULES) {
        packageName = rule.rewrite(packageName);
      }
      List<String> names = Splitter.on(DEFAULT_PACKAGE_SEPARATOR).splitToList(packageName);
      LinkedList<String> collector = new LinkedList<>();
      boolean found = false;
      for (String n : names) {
        if (VersionMatcher.isVersion(n)) {
          collector.add(String.format("%s_%s", collector.removeLast(), n));
          collector.add("rules_gapic");
          found = true;
        } else {
          collector.add(n);
        }
      }
      if (!found) {
        collector.add("rules_gapic");
      }
      return Joiner.on('.').join(collector);
    }
  }
}
