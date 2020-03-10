/* Copyright 2020 Google LLC
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
package com.google.api.codegen.config;

import com.google.api.codegen.util.Name;
import com.google.api.pathtemplate.PathTemplate;
import com.google.common.collect.ImmutableSet;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** ResourceNamePatternConfig represents the configuration a resource name pattern. */
public class ResourceNamePatternConfig {

  private final String pattern;
  @Nullable private final PathTemplate template;

  public ResourceNamePatternConfig(String pattern) {
    this.pattern = pattern;
    this.template = PathTemplate.create(pattern);
  }

  /** The binding variables of the pattern, usually in curly braces. */
  public ImmutableSet<String> getBindingVariables() {
    if (isFixedPattern()) {
      return ImmutableSet.of();
    }
    return ImmutableSet.copyOf(template.vars());
  }

  /** Returns true if the pattern does not have binding variables. */
  public boolean isFixedPattern() {
    return pattern.indexOf('{') == -1 && pattern.indexOf('}') == -1;
  }

  /**
   * Returns the name of the static method that creates an instance of the resource name class
   * representing this pattern, such as ofProjectBookName.
   */
  public String getCreateMethodName() {
    return Name.anyLower("of", getPatternNameLowerUnderscore(), "name").toLowerCamel();
  }

  /**
   * Returns the name of the static method that returns a formatted string using this pattern, such
   * as formatProjectBookName.
   */
  public String getFormatMethodName() {
    return Name.anyLower("format", getPatternNameLowerUnderscore(), "name").toLowerCamel();
  }

  private String getPatternNameLowerUnderscore() {
    if (isFixedPattern()) {
      String name = pattern.replaceAll("^[^a-zA-Z]+", "");
      name = name.replaceAll("[^a-zA-Z]$", "");
      name = name.replaceAll("[^a-zA-Z]+", "_");
      return name;
    }

    // PathTemplate uses an ImmutableMap to keep track of bindings, so we
    // can count on it to give us the correct order of binding variables
    return getBindingVariables().stream().collect(Collectors.joining("_"));
  }
}
