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
package com.google.api.codegen.config;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Generator for language settings section. Currently the only language setting is the package
 * name.
 */
public class LanguageGenerator {

  private static final String DEFAULT_PACKAGE_SEPARATOR = "\\.";

  private static final String CONFIG_KEY_PACKAGE_NAME = "package_name";

  private static final Map<String, LanguageFormatter> LANGUAGE_FORMATTERS =
      ImmutableMap.<String, LanguageFormatter>builder()
          .put("java", new SimpleLanguageFormatter(".", "com", false))
          .put("python", new SimpleLanguageFormatter(".", null, false))
          .put("go", new GoLanguageFormatter())
          .put("csharp", new SimpleLanguageFormatter(".", null, true))
          .put("ruby", new SimpleLanguageFormatter("::", null, true))
          .put("php", new SimpleLanguageFormatter("\\", null, true))
          .build();

  public static Map<String, Object> generate(String packageName) {
    List<String> packageNameComponents =
        Arrays.asList(packageName.split(DEFAULT_PACKAGE_SEPARATOR));

    Map<String, Object> languages = new LinkedHashMap<>();
    for (String language : LANGUAGE_FORMATTERS.keySet()) {
      LanguageFormatter formatter = LANGUAGE_FORMATTERS.get(language);
      String formattedPackageName = formatter.getFormattedPackageName(packageNameComponents);

      Map<String, Object> packageNameMap = new LinkedHashMap<>();
      packageNameMap.put(CONFIG_KEY_PACKAGE_NAME, formattedPackageName);
      languages.put(language, packageNameMap);
    }
    return languages;
  }

  private static String firstCharToUpperCase(String string) {
    return Character.toUpperCase(string.charAt(0)) + string.substring(1);
  }

  private interface LanguageFormatter {
    String getFormattedPackageName(List<String> nameComponents);
  }

  private static class SimpleLanguageFormatter implements LanguageFormatter {

    private final String separator;
    private final String prefix;
    private final boolean capitalize;

    public SimpleLanguageFormatter(String separator, String prefix, boolean capitalize) {
      this.separator = separator;
      this.prefix = prefix;
      this.capitalize = capitalize;
    }

    public String getFormattedPackageName(List<String> nameComponents) {
      List<String> elements = new LinkedList<>();
      if (prefix != null) {
        elements.add(prefix);
      }
      for (String component : nameComponents) {
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

    private static LanguageFormatter backup =
        new SimpleLanguageFormatter("/", "google.golang.org", false);

    public String getFormattedPackageName(List<String> nameComponents) {
      // If the name follows the pattern google.foo.bar.v1234,
      // we reformat it into cloud.google.com.
      // google.logging.v2 => cloud.google.com/go/logging/apiv2
      // Otherwise, fall back to backup
      int size = nameComponents.size();
      if (size < 3
          || !nameComponents.get(0).equals("google")
          || !nameComponents.get(size - 1).startsWith("v")) {
        return backup.getFormattedPackageName(nameComponents);
      }
      return "cloud.google.com/go/"
          + Joiner.on("/").join(nameComponents.subList(1, size - 1))
          + "/api"
          + nameComponents.get(size - 1);
    }
  }
}
