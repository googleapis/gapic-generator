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
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;

/** InitValueConfig configures the initial value of an initialized variable. */
@AutoValue
public abstract class InitValueConfig {

  public static InitValueConfig create() {
    return new AutoValue_InitValueConfig(null, null, null, null);
  }

  public static InitValueConfig createWithValue(InitValue value) {
    return new AutoValue_InitValueConfig(null, null, value, null);
  }

  public static InitValueConfig create(
      String apiWrapperName, SingleResourceNameConfig singleResourceNameConfig) {
    return new AutoValue_InitValueConfig(apiWrapperName, singleResourceNameConfig, null, null);
  }

  public static InitValueConfig create(
      String apiWrapperName,
      SingleResourceNameConfig singleResourceNameConfig,
      Map<String, InitValue> resourceNameBindingValues) {
    return new AutoValue_InitValueConfig(
        apiWrapperName, singleResourceNameConfig, null, resourceNameBindingValues);
  }

  @Nullable
  public abstract String getApiWrapperName();

  @Nullable
  public abstract SingleResourceNameConfig getSingleResourceNameConfig();

  @Nullable
  public abstract InitValue getInitialValue();

  @Nullable
  public abstract Map<String, InitValue> getResourceNameBindingValues();

  /** Creates an updated InitValueConfig with the provided value. */
  public InitValueConfig withInitialCollectionValue(String entityName, InitValue value) {
    HashMap<String, InitValue> resourceNameBindingValues = new HashMap<>();
    resourceNameBindingValues.put(entityName, value);
    return withInitialCollectionValues(resourceNameBindingValues);
  }

  public InitValueConfig withInitialCollectionValues(
      Map<String, InitValue> resourceNameBindingValues) {
    return new AutoValue_InitValueConfig(
        getApiWrapperName(), getSingleResourceNameConfig(), null, resourceNameBindingValues);
  }

  public boolean isEmpty() {
    return getSingleResourceNameConfig() == null;
  }

  public boolean hasFormattingConfig() {
    return getSingleResourceNameConfig() != null;
  }

  public boolean hasFormattingConfigInitialValues() {
    return getResourceNameBindingValues() != null && !getResourceNameBindingValues().isEmpty();
  }

  public boolean hasSimpleInitialValue() {
    return getInitialValue() != null;
  }
}
