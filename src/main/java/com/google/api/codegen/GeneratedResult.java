/* Copyright 2016 Google LLC
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
package com.google.api.codegen;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.Map;
import java.util.Objects;

/** Represents a generated document plus the filename for the document. */
@AutoValue
public abstract class GeneratedResult<T> {

  public static <T> GeneratedResult<T> create(T body, boolean executable) {
    return new AutoValue_GeneratedResult<>(body, executable);
  }

  public static <T> Map<String, T> extractBodies(Map<String, GeneratedResult<T>> results) {
    ImmutableMap.Builder<String, T> extractedResults = ImmutableMap.builder();
    for (Map.Entry<String, GeneratedResult<T>> entry : results.entrySet()) {
      extractedResults.put(entry.getKey(), Objects.requireNonNull(entry.getValue().getBody()));
    }
    return extractedResults.build();
  }

  public abstract T getBody();

  public abstract boolean isExecutable();
}
