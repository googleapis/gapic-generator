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
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nullable;

/** Configuration of the resource name types for all message field. */
@AutoValue
public abstract class ResourceNameMessageConfigs {

  abstract ImmutableMap<String, ResourceNameMessageConfig> getResourceTypeConfigMap();

  public abstract ListMultimap<String, Field> getFieldsWithResourceNamesByMessage();

  @Nullable
  public static ResourceNameMessageConfigs createMessageResourceTypesConfig(
      Model model, ConfigProto configProto, String defaultPackage) {
    ImmutableMap.Builder<String, ResourceNameMessageConfig> builder =
        ImmutableMap.<String, ResourceNameMessageConfig>builder();
    for (ResourceNameMessageConfigProto messageResourceTypesProto :
        configProto.getResourceNameGenerationList()) {
      ResourceNameMessageConfig messageResourceTypeConfig =
          ResourceNameMessageConfig.createResourceNameMessageConfig(
              model.getDiagCollector(), messageResourceTypesProto, defaultPackage);
      builder.put(messageResourceTypeConfig.messageName(), messageResourceTypeConfig);
    }
    ImmutableMap<String, ResourceNameMessageConfig> messageResourceTypeConfigMap = builder.build();

    ListMultimap<String, Field> fieldsByMessage = ArrayListMultimap.create();
    Set<String> seenProtoFiles = new HashSet<>();
    for (ProtoFile protoFile : model.getFiles()) {
      if (!seenProtoFiles.contains(protoFile.getSimpleName())) {
        seenProtoFiles.add(protoFile.getSimpleName());
        for (MessageType msg : protoFile.getMessages()) {
          ResourceNameMessageConfig messageConfig =
              messageResourceTypeConfigMap.get(msg.getFullName());
          if (messageConfig == null) {
            continue;
          }
          for (Field field : msg.getFields()) {
            if (messageConfig.getEntityNameForField(field.getSimpleName()) != null) {
              fieldsByMessage.put(msg.getFullName(), field);
            }
          }
        }
      }
    }

    return new AutoValue_ResourceNameMessageConfigs(messageResourceTypeConfigMap, fieldsByMessage);
  }

  public boolean isEmpty() {
    return getResourceTypeConfigMap().isEmpty();
  }

  public boolean fieldHasResourceName(Field field) {
    return fieldHasResourceName(field.getParent().getFullName(), field.getSimpleName());
  }

  public boolean fieldHasResourceName(String messageFullName, String fieldSimpleName) {
    return getResourceNameOrNullForField(messageFullName, fieldSimpleName) != null;
  }

  public String getFieldResourceName(Field field) {
    return getFieldResourceName(field.getParent().getFullName(), field.getSimpleName());
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
