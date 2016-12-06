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

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import javax.annotation.Nullable;

/** Configuration of the resource name types for all message field. */
@AutoValue
public abstract class ResourceNameMessageConfigs {

  abstract ImmutableMap<String, ResourceNameMessageConfig> getResourceTypeConfigMap();

  @Nullable
  public static ResourceNameMessageConfigs createMessageResourceTypesConfig(
      DiagCollector diagCollector, ConfigProto configProto) {
    ImmutableMap.Builder<String, ResourceNameMessageConfig> messageResourceTypeConfigMap =
        ImmutableMap.<String, ResourceNameMessageConfig>builder();
    for (ResourceNameMessageConfigProto messageResourceTypesProto :
        configProto.getResourceNameGenerationList()) {
      ResourceNameMessageConfig messageResourceTypeConfig =
          ResourceNameMessageConfig.createResourceNameMessageConfig(
              diagCollector, messageResourceTypesProto);
      messageResourceTypeConfigMap.put(
          messageResourceTypeConfig.messageName(), messageResourceTypeConfig);
    }
    return new AutoValue_ResourceNameMessageConfigs(messageResourceTypeConfigMap.build());
  }

  public boolean isEmpty() {
    return getResourceTypeConfigMap().isEmpty();
  }

  public boolean fieldHasResourceName(Field field) {
    return fieldHasResourceName(field.getParent().getSimpleName(), field.getSimpleName());
  }

  public boolean fieldHasResourceName(String messageSimpleName, String fieldSimpleName) {
    return getResourceNameOrNullForField(messageSimpleName, fieldSimpleName) != null;
  }

  public String getFieldResourceName(Field field) {
    return getFieldResourceName(field.getParent().getSimpleName(), field.getSimpleName());
  }

  public String getFieldResourceName(String messageSimpleName, String fieldSimpleName) {
    if (!fieldHasResourceName(messageSimpleName, fieldSimpleName)) {
      throw new IllegalArgumentException(
          "Field "
              + fieldSimpleName
              + " of message "
              + messageSimpleName
              + " does not have a resource name.");
    }
    return getResourceNameOrNullForField(messageSimpleName, fieldSimpleName);
  }

  private String getResourceNameOrNullForField(String messageSimpleName, String fieldSimpleName) {
    ResourceNameMessageConfig messageResourceTypeConfig =
        getResourceTypeConfigMap().get(messageSimpleName);
    if (messageResourceTypeConfig == null) {
      return null;
    }
    return messageResourceTypeConfig.getEntityNameForField(fieldSimpleName);
  }
}
