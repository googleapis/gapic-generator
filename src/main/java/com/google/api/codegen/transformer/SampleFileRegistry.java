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

import com.google.api.codegen.config.SampleConfig;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code SampleFileRegistry} is used to verify that the samples we generate with different
 * parameters have different paths, so they don't clobber each other.
 *
 * <p>If a sample has a unique region tag, the file name of the sample will be its region tag in the
 * language-idiomatic case, followed by an appropriate extension.
 *
 * <p>If the region tag of a sample is not unique, the file name of the sample will be constructed
 * by concatinating `method_name`, `calling_form` and `value_set_id`. The file name will be in the
 * language-idiomatic case and followed by an appropriate extension as well.
 */
public class SampleFileRegistry {

  private final Map<String, SampleInfo> files = new HashMap<>();
  private final Map<String, Integer> userProvidedIdCount = new HashMap<>();
  private final Map<String, Integer> usedSuffixes = new HashMap<>();
  private final SurfaceNamer namer;

  public SampleFileRegistry(SurfaceNamer namer, List<MethodSampleView> allSamples) {
    this.namer = namer;
    for (MethodSampleView sample : allSamples) {
      userProvidedIdCount.put(
          sample.regionTag(), userProvidedIdCount.getOrDefault(sample.regionTag(), 0) + 1);
    }
  }

  public SampleFileRegistry(SurfaceNamer namer, Collection<SampleConfig> sampleConfigs) {
    this.namer = namer;
    for (SampleConfig config : sampleConfigs) {
      userProvidedIdCount.put(config.id(), userProvidedIdCount.getOrDefault(config.id(), 0) + 1);
    }
  }

  public String getUniqueSampleId(String userProvidedId, CallingForm callingForm) {
    Integer count = userProvidedIdCount.get(userProvidedId);
    Preconditions.checkState(count != null && count > 0, "Sample not registered.");

    if (count == 1) {
      return userProvidedId;
    }

    String idWithCallingPattern = userProvidedId + "_" + callingForm.toLowerUnderscore();
    if (!userProvidedIdCount.containsKey(idWithCallingPattern)) {
      userProvidedIdCount.put(idWithCallingPattern, 1);
      return idWithCallingPattern;
    }

    int suffix = usedSuffixes.getOrDefault(idWithCallingPattern, 0) + 1;
    usedSuffixes.put(idWithCallingPattern, suffix);
    return idWithCallingPattern + suffix;
  }

  @Deprecated
  public String getSampleClassName(MethodSampleView sample, String method) {
    String regionTag = sample.regionTag();
    Preconditions.checkState(
        userProvidedIdCount.get(regionTag) != null && userProvidedIdCount.get(regionTag) > 0,
        "Sample not registered.");
    if (userProvidedIdCount.get(regionTag) == 1) {
      return namer.getApiSampleClassName(regionTag);
    }
    return namer.getApiSampleClassName(method, sample.callingForm().toLowerCamel(), sample.id());
  }

  @Deprecated
  public String getSampleFileName(MethodSampleView sample, String method) {
    String regionTag = sample.regionTag();
    String callingForm = sample.callingForm().toLowerCamel();
    String id = sample.id();
    Preconditions.checkState(
        userProvidedIdCount.get(regionTag) != null && userProvidedIdCount.get(regionTag) > 0,
        "Sample not registered.");
    String fileName;
    if (userProvidedIdCount.get(regionTag) == 1) {
      fileName = namer.getApiSampleFileName(regionTag);
    } else {
      fileName = namer.getApiSampleFileName(method, callingForm, id);
    }
    addFile(fileName, method, callingForm, id, regionTag);
    return fileName;
  }

  /**
   * Adds a file with the given parameters to the registry. If a file with the given {@code path}
   * previously existed in the registry and any of the parameters don't match, throws an exception
   * describing the conflict.
   */
  private void addFile(
      String path, String method, String callingForm, String id, String regionTag) {
    SampleInfo current =
        SampleInfo.newBuilder()
            .path(path)
            .method(method)
            .callingForm(callingForm)
            .id(id)
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

    public abstract String id();

    public abstract String regionTag();

    public static SampleInfo.Builder newBuilder() {
      return new AutoValue_SampleFileRegistry_SampleInfo.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
      public abstract SampleInfo.Builder path(String value);

      public abstract SampleInfo.Builder method(String value);

      public abstract SampleInfo.Builder callingForm(String value);

      public abstract SampleInfo.Builder id(String value);

      public abstract SampleInfo.Builder regionTag(String value);

      public abstract SampleInfo build();
    }
  }
}
