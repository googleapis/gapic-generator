/* Copyright 2016 Google Inc
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

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Generator for language settings section. Currently the only language setting is the package name.
 */
public class LanguageGenerator {

  private static final String DEFAULT_PACKAGE_SEPARATOR = ".";

  private static final String CONFIG_KEY_PACKAGE_NAME = "package_name";

  private static final Map<String, LanguageFormatter> LANGUAGE_FORMATTERS;

  static {
    List<RewriteRule> javaRewriteRules =
        Arrays.asList(
            new RewriteRule("^google", "com.google.cloud"),
            new RewriteRule("(.v[^.]+)$", ".spi$1"));
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

  public static Map<String, Object> generate(String packageName) {

    Map<String, Object> languages = new LinkedHashMap<>();
    for (String language : LANGUAGE_FORMATTERS.keySet()) {
      LanguageFormatter formatter = LANGUAGE_FORMATTERS.get(language);
      String formattedPackageName = formatter.getFormattedPackageName(packageName);

      Map<String, Object> packageNameMap = new LinkedHashMap<>();
      packageNameMap.put(CONFIG_KEY_PACKAGE_NAME, formattedPackageName);
      languages.put(language, packageNameMap);
    }
    return languages;
  }

  private static String firstCharToUpperCase(String string) {
    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }

  /** Returns true if it is a Google Cloud API. */
  private static boolean isApiGoogleCloud(List<String> nameComponents) {
    int size = nameComponents.size();
    return size >= 3
        && nameComponents.get(0).equals("google")
        && nameComponents.get(size - 1).startsWith("v");
  }

  private interface LanguageFormatter {
    String getFormattedPackageName(String packageName);
  }

  private static class SimpleLanguageFormatter implements LanguageFormatter {

    private final String separator;
    private final List<RewriteRule> rewriteRules;
    private final boolean capitalize;

    public SimpleLanguageFormatter(
        String separator, List<RewriteRule> rewriteRules, boolean capitalize) {
      this.separator = separator;
      if (rewriteRules != null) {
        this.rewriteRules = rewriteRules;
      } else {
        this.rewriteRules = new ArrayList<>();
      }
      this.capitalize = capitalize;
    }

    public String getFormattedPackageName(String packageName) {
      for (RewriteRule rewriteRule : rewriteRules) {
        packageName = rewriteRule.rewrite(packageName);
      }
      List<String> elements = new LinkedList<>();
      for (String component : Splitter.on(DEFAULT_PACKAGE_SEPARATOR).split(packageName)) {
        if (capitalize) {
          elements.add(firstCharToUpperCase(component));
        } else {
          elements.add(component);
        }
      }
      return Joiner.on(separator).join(elements);
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
        nameComponents.add(0, "google.golang.org");
        return Joiner.on("/").join(nameComponents);
      }
      int size = nameComponents.size();
      return "cloud.google.com/go/"
          + Joiner.on("/").join(nameComponents.subList(1, size - 1))
          + "/api"
          + nameComponents.get(size - 1);
    }
  }

  private static class NodeJSLanguageFormatter implements LanguageFormatter {
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
