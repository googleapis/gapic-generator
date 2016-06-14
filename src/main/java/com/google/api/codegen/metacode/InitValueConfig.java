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

import com.google.api.codegen.CollectionConfig;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;

/**
 * InitValueConfig configures the initial value of an initialized variable.
 */
@AutoValue
public abstract class InitValueConfig {
  public static InitValueConfig create() {
    return new AutoValue_InitValueConfig(null, null);
  }

  public static InitValueConfig create(String apiWrapperName, CollectionConfig collectionConfig) {
    return new AutoValue_InitValueConfig(apiWrapperName, collectionConfig);
  }

  @Nullable
  public abstract String getApiWrapperName();

  @Nullable
  public abstract CollectionConfig getCollectionConfig();

  public boolean isEmpty() {
    return getCollectionConfig() == null;
  }

  public boolean hasFormattingConfig() {
    return getCollectionConfig() != null;
  }
}
