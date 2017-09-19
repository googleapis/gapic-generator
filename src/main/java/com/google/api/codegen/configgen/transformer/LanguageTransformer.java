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
package com.google.api.codegen.configgen.transformer;

import com.google.api.codegen.configgen.viewmodel.LanguageSettingView;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/** Generates language setting view objects using a package name. */
public class LanguageTransformer {
  private static final String DEFAULT_PACKAGE_SEPARATOR = ".";

  public static final Map<String, LanguageFormatter> LANGUAGE_FORMATTERS;

  static {
    List<RewriteRule> javaRewriteRules =
        Arrays.asList(new RewriteRule("^google(\\.cloud)?", "com.google.cloud"));
    List<RewriteRule> pythonRewriteRules =
        Arrays.asList(new RewriteRule("^google(?!\\.cloud)", "google.cloud.gapic"));
    List<RewriteRule> commonRewriteRules =
        Arrays.asList(new RewriteRule("^google(?!\\.cloud)", "google.cloud"));
    LANGUAGE_FORMATTERS =
        ImmutableMap.<String, LanguageFormatter>builder()
            .put("java", new SimpleLanguageFormatter(".", javaRewriteRules, false))
            .put("python", new SimpleLanguageFormatter(".", pythonRewriteRules, false))
            .put("go", new GoLanguageFormatter())
            .put("csharp", new SimpleLanguageFormatter(".", null, true))
            .put("ruby", new SimpleLanguageFormatter("::", commonRewriteRules, true))
            .put("php", new SimpleLanguageFormatter("\\", commonRewriteRules, true))
            .put("nodejs", new NodeJSLanguageFormatter())
            .build();
  }

  public List<LanguageSettingView> generateLanguageSettings(String packageName) {
    ImmutableList.Builder<LanguageSettingView> languageSettings = ImmutableList.builder();
    for (Map.Entry<String, LanguageFormatter> entry : LANGUAGE_FORMATTERS.entrySet()) {
      LanguageFormatter languageFormatter = entry.getValue();
      languageSettings.add(
          LanguageSettingView.newBuilder()
              .language(entry.getKey())
              .packageName(languageFormatter.getFormattedPackageName(packageName))
              .build());
    }
    return languageSettings.build();
  }

  public interface LanguageFormatter {
    String getFormattedPackageName(String packageName);
  }

  private static class SimpleLanguageFormatter implements LanguageFormatter {

    private final String separator;
    private final List<RewriteRule> rewriteRules;
    private final boolean shouldCapitalize;

    public SimpleLanguageFormatter(
        String separator, List<RewriteRule> rewriteRules, boolean shouldCapitalize) {
      this.separator = separator;
      if (rewriteRules != null) {
        this.rewriteRules = rewriteRules;
      } else {
        this.rewriteRules = new ArrayList<>();
      }
      this.shouldCapitalize = shouldCapitalize;
    }

    @Override
    public String getFormattedPackageName(String packageName) {
      for (RewriteRule rewriteRule : rewriteRules) {
        packageName = rewriteRule.rewrite(packageName);
      }
      List<String> elements = new LinkedList<>();
      for (String component : Splitter.on(DEFAULT_PACKAGE_SEPARATOR).split(packageName)) {
        if (shouldCapitalize) {
          elements.add(capitalize(component));
        } else {
          elements.add(component);
        }
      }
      return Joiner.on(separator).join(elements);
    }

    private String capitalize(String string) {
      return Character.toUpperCase(string.charAt(0)) + string.substring(1);
    }
  }

  private static class GoLanguageFormatter implements LanguageFormatter {
    public String getFormattedPackageName(String packageName) {
      List<String> nameComponents =
          Lists.newArrayList(Splitter.on(DEFAULT_PACKAGE_SEPARATOR).splitToList(packageName));

      // If the name follows the pattern google.foo.bar.v1234,
      // we reformat it into cloud.google.com.
      // google.logging.v2 => cloud.google.com/go/logging/apiv2
      // Otherwise, fall back to backup
      if (!isApiGoogleCloud(nameComponents)) {
        nameComponents.add(0, "google.golang.org");
        return Joiner.on("/").join(nameComponents);
      }
      int size = nameComponents.size();
      return "cloud.google.com/go/"
          + Joiner.on("/").join(nameComponents.subList(1, size - 1))
          + "/api"
          + nameComponents.get(size - 1);
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
}
