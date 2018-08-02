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
package com.google.api.codegen.metacode;

import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** InitValueConfig configures the initial value of an initialized variable. */
@AutoValue
public abstract class InitValueConfig {

  public static InitValueConfig create() {
    return newBuilder().build();
  }

  public static InitValueConfig createWithValue(InitValue value) {
    return newBuilder().setInitialValue(value).build();
  }

  public static InitValueConfig create(
      String apiWrapperName, SingleResourceNameConfig singleResourceNameConfig) {
    return newBuilder()
        .setApiWrapperName(apiWrapperName)
        .setSingleResourceNameConfig(singleResourceNameConfig)
        .build();
  }

  public static InitValueConfig create(
      String apiWrapperName,
      SingleResourceNameConfig singleResourceNameConfig,
      Map<String, InitValue> resourceNameBindingValues) {
    return newBuilder()
        .setApiWrapperName(apiWrapperName)
        .setSingleResourceNameConfig(singleResourceNameConfig)
        .setResourceNameBindingValues(resourceNameBindingValues)
        .build();
  }

  @Nullable
  public abstract String getApiWrapperName();

  @Nullable
  public abstract SingleResourceNameConfig getSingleResourceNameConfig();

  @Nullable
  public abstract InitValue getInitialValue();

  public abstract Map<String, InitValue> getResourceNameBindingValues();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_InitValueConfig.Builder().setResourceNameBindingValues(ImmutableMap.of());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder setApiWrapperName(String val);

    public abstract Builder setSingleResourceNameConfig(SingleResourceNameConfig val);

    public abstract Builder setInitialValue(InitValue val);

    public abstract Builder setResourceNameBindingValues(Map<String, InitValue> val);

    public abstract InitValueConfig build();
  }

  /** Creates an updated InitValueConfig with the provided value. */
  public InitValueConfig withInitialCollectionValue(String entityName, InitValue value) {
    HashMap<String, InitValue> resourceNameBindingValues = new HashMap<>();
    resourceNameBindingValues.put(entityName, value);
    return withInitialCollectionValues(resourceNameBindingValues);
  }

  public InitValueConfig withInitialCollectionValues(
      Map<String, InitValue> resourceNameBindingValues) {
    return toBuilder().setResourceNameBindingValues(resourceNameBindingValues).build();
  }

  public boolean isEmpty() {
    return getSingleResourceNameConfig() == null;
  }

  public boolean hasFormattingConfig() {
    return getSingleResourceNameConfig() != null;
  }

  public boolean hasFormattingConfigInitialValues() {
    return !getResourceNameBindingValues().isEmpty();
  }

  public boolean hasSimpleInitialValue() {
    return getInitialValue() != null;
  }
}
