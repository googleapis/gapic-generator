/* Copyright 2018 Google LLC
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
package com.google.api.codegen.transformer;

import com.google.auto.value.AutoValue;
import java.util.HashMap;
import java.util.Map;

/**
 * {@code SampleFileRegistry} is used to verify that the samples we generate with different
 * parameters have different paths, so they don't clobber each other.
 */
public class SampleFileRegistry {

  private final Map<String, SampleInfo> files = new HashMap<>();

  /**
   * Adds a file with the given parameters to the registry. If a file with the given {@code path}
   * previously existed in the registry and any of the parameters don't match, throws an exception
   * describing the conflict.
   */
  public void addFile(
      String path, String method, String callingForm, String valueSet, String regionTag) {
    SampleInfo current =
        SampleInfo.newBuilder()
            .path(path)
            .method(method)
            .callingForm(callingForm)
            .valueSet(valueSet)
            .regionTag(regionTag)
            .build();
    SampleInfo previous = files.get(path);
    if (previous == null) {
      files.put(path, current);
    } else if (!current.equals(previous)) {
      throw new IllegalArgumentException(
          String.format(
              "conflicting configurations for sample \"%s\":\n  %s\n  %s",
              path, previous.toString(), current.toString()));
    }
  }

  @AutoValue
  public abstract static class SampleInfo {
    public abstract String path();

    public abstract String method();

    public abstract String callingForm();

    public abstract String valueSet();

    public abstract String regionTag();

    public static SampleInfo.Builder newBuilder() {
      return new AutoValue_SampleFileRegistry_SampleInfo.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract SampleInfo.Builder path(String value);

      public abstract SampleInfo.Builder method(String value);

      public abstract SampleInfo.Builder callingForm(String value);

      public abstract SampleInfo.Builder valueSet(String value);

      public abstract SampleInfo.Builder regionTag(String value);

      public abstract SampleInfo build();
    }
  }
}
