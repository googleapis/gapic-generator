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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimaps;
import java.util.List;
import java.util.Optional;

/** Utility class for FlatteningConfig. */
public class FlatteningConfigs {

  private FlatteningConfigs() {}

  /**
   * Returns a list of flattening configs to generate unit tests and samples for. Pick one from all
   * flattening configs that have the same flattening fields. When there are multiple such
   * flattening configs, choose one with resource name types if possible.
   */
  public static List<FlatteningConfig> getRepresentativeFlatteningConfigs(
      List<FlatteningConfig> flatteningConfigs) {
    ImmutableList.Builder<FlatteningConfig> builder = ImmutableList.builder();
    for (List<FlatteningConfig> flatteningsByMethodSignature :
        groupByMethodSignature(flatteningConfigs)) {
      Optional<FlatteningConfig> flattening =
          flatteningsByMethodSignature
              .stream()
              .filter(FlatteningConfig::hasAnyResourceNameParameter)
              .findFirst();
      builder.add(flattening.isPresent() ? flattening.get() : flatteningConfigs.get(0));
    }

    return builder.build();
  }

  /**
   * Return a list of list of flattening configs, where flattening configs in each inner list are
   * generated from the same google.api.method_signature annotation.
   */
  private static List<List<FlatteningConfig>> groupByMethodSignature(
      List<FlatteningConfig> flatteningConfigs) {
    ImmutableListMultimap<String, FlatteningConfig> configs =
        flatteningConfigs
            .stream()
            .collect(
                ImmutableListMultimap.toImmutableListMultimap(
                    FlatteningConfig::getMethodSignature, f -> f));
    return Multimaps.asMap(configs).values().stream().collect(ImmutableList.toImmutableList());
  }
}
