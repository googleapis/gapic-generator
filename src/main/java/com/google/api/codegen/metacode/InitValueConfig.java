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
package com.google.api.codegen.metacode;

import com.google.api.codegen.config.CollectionConfig;
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

  public static InitValueConfig createWithValue(String value) {
    return new AutoValue_InitValueConfig(null, null, value, null);
  }

  public static InitValueConfig create(String apiWrapperName, CollectionConfig collectionConfig) {
    return new AutoValue_InitValueConfig(apiWrapperName, collectionConfig, null, null);
  }

  public static InitValueConfig create(
      String apiWrapperName,
      CollectionConfig collectionConfig,
      Map<String, String> collectionValues) {
    return new AutoValue_InitValueConfig(apiWrapperName, collectionConfig, null, collectionValues);
  }

  @Nullable
  public abstract String getApiWrapperName();

  @Nullable
  public abstract CollectionConfig getCollectionConfig();

  @Nullable
  public abstract String getInitialValue();

  @Nullable
  public abstract Map<String, String> getCollectionValues();

  /** Creates an updated InitValueConfig with the provided value. */
  public InitValueConfig withInitialCollectionValue(String entityName, String value) {
    HashMap<String, String> collectionValues = new HashMap<>();
    collectionValues.put(entityName, value);
    return withInitialCollectionValues(collectionValues);
  }

  public InitValueConfig withInitialCollectionValues(Map<String, String> collectionValues) {
    return new AutoValue_InitValueConfig(
        getApiWrapperName(), getCollectionConfig(), null, collectionValues);
  }

  public boolean isEmpty() {
    return getCollectionConfig() == null;
  }

  public boolean hasFormattingConfig() {
    return getCollectionConfig() != null;
  }

  public boolean hasFormattingConfigInitialValues() {
    return getCollectionValues() != null && !getCollectionValues().isEmpty();
  }

  public boolean hasSimpleInitialValue() {
    return getInitialValue() != null;
  }
}
