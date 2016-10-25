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
package com.google.api.codegen.config;

import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

@AutoValue
public abstract class ResourceNameMessageConfig {

  public abstract String messageName();

  abstract ImmutableMap<String, String> entityNameMap();

  @Nullable
  public static ResourceNameMessageConfig createInterfaceConfig(
      DiagCollector diagCollector, ResourceNameMessageConfigProto messageResourceTypesProto) {
    String messageName = messageResourceTypesProto.getMessageName();
    ImmutableMap<String, String> entityNameMap =
        ImmutableMap.copyOf(messageResourceTypesProto.getFieldResourceTypes());

    return new AutoValue_ResourceNameMessageConfig(messageName, entityNameMap);
  }

  public String getEntityNameForField(String fieldSimpleName) {
    return entityNameMap().get(fieldSimpleName);
  }
}
