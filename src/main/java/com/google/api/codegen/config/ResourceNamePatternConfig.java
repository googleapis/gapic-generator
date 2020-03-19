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
import com.google.common.base.Preconditions;
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

  public String getPattern() {
    return this.pattern;
  }

  public PathTemplate getNameTemplate() {
    return this.template;
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
    return Name.anyLower("of", getPatternId(), "name").toLowerCamel();
  }

  /**
   * Returns the name of the static method that returns a formatted string using this pattern, such
   * as formatProjectBookName.
   */
  public String getFormatMethodName() {
    return Name.anyLower("format", getPatternId(), "name").toLowerCamel();
  }

  /**
   * Returns the entity ID of the resource name pattern, always in lower_underscore case. The entity
   * ID is used to generate formatting and parsing function names.
   *
   * <p>If the pattern is a fixed resource name pattern, the entity ID is derived by concatenating
   * all alphabetical substrings with underscores.
   *
   * <p>If the pattern is a normal formattable resource name pattern, the entity ID is derived by
   * concatenating all binding variables with underscores.
   *
   * <p>If the pattern is a singleton resource name pattern (see https://aip.dev/156), the entity ID
   * is derived by concatenating all binding variables and the last non-binding segment of the
   * pattern with underscores.
   */
  public String getPatternId() {
    // TODO(hzyi): support singleton resource name patterns
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

  /**
   * If this pattern represents a single formattable resource name, creates a SingleResourceNameConfig from it.
   */
  public SingleResourceNameConfig asSingleResourceNameConfig() {
    Preconditions.checkArgument(!isFixedPattern(), "pattern %s is a fixed pattern", pattern);
    return SingleResourceNameConfig.newBuilder()
        .setNamePattern(pattern)
        .setNameTemplate(template)
        .setEntityId(getPatternId())
        .setEntityName(Name.from(getPatternId()))
        .build();
  }

  /** If this pattern is a fixed resource name, creates a FixedResourceNameConfig from it. */
  public FixedResourceNameConfig asFixedResourceNameConfig() {
    Preconditions.checkArgument(isFixedPattern(), "pattern %s is not a fixed pattern", pattern);
    return new AutoValue_FixedResourceNameConfig(
        getPatternId(), Name.from(getPatternId()), pattern, null);
  }
}
