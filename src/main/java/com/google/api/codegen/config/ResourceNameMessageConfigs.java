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
package com.google.api.codegen.config;

import com.google.api.Resource;
import com.google.api.ResourceSet;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ListMultimap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Configuration of the resource name types for all message field. */
@AutoValue
public abstract class ResourceNameMessageConfigs {

  abstract ImmutableMap<String, ResourceNameMessageConfig> getResourceTypeConfigMap();

  /**
   * Get a map from fully qualified message names to Fields, where each field has a resource name
   * defined.
   */
  public abstract ListMultimap<String, FieldModel> getFieldsWithResourceNamesByMessage();

  @VisibleForTesting
  static ResourceNameMessageConfigs createMessageResourceTypesConfig(
      List<ProtoFile> protoFiles,
      ConfigProto configProto,
      String defaultPackage,
      Map<Resource, ProtoFile> resourceDefs,
      Map<ResourceSet, ProtoFile> resourceSetDefs,
      ProtoParser protoParser) {
    HashMap<String, ResourceNameMessageConfig> mutableMap = new HashMap<>();
    insertMessageResourceTypesConfigFromAnnotations(
        protoFiles, resourceDefs, resourceSetDefs, protoParser, mutableMap);
    insertMessageResourceTypesConfigFromGapicConfig(configProto, defaultPackage, mutableMap);
    return new AutoValue_ResourceNameMessageConfigs(
        ImmutableSortedMap.copyOf(mutableMap), createFieldsByMessage(protoFiles, mutableMap));
  }

  @VisibleForTesting
  static ResourceNameMessageConfigs createMessageResourceTypesConfig(
      List<ProtoFile> protoFiles, ConfigProto configProto, String defaultPackage) {
    HashMap<String, ResourceNameMessageConfig> mutableMap = new HashMap<>();
    insertMessageResourceTypesConfigFromGapicConfig(configProto, defaultPackage, mutableMap);
    return new AutoValue_ResourceNameMessageConfigs(
        ImmutableSortedMap.copyOf(mutableMap), createFieldsByMessage(protoFiles, mutableMap));
  }

  private static ListMultimap<String, FieldModel> createFieldsByMessage(
      List<ProtoFile> protoFiles,
      Map<String, ResourceNameMessageConfig> messageResourceTypeConfigMap) {
    ListMultimap<String, FieldModel> fieldsByMessage = ArrayListMultimap.create();
    Set<String> seenProtoFiles = new HashSet<>();
    for (ProtoFile protoFile : protoFiles) {
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
              fieldsByMessage.put(msg.getFullName(), new ProtoField(field));
            }
          }
        }
      }
    }
    return fieldsByMessage;
  }

  private static void insertMessageResourceTypesConfigFromGapicConfig(
      ConfigProto configProto,
      String defaultPackage,
      HashMap<String, ResourceNameMessageConfig> mutableMap) {
    // Add more ResourceNameMessageConfigs from configProto. Overwrite the configs from
    // configProto if any clash.
    for (ResourceNameMessageConfigProto messageResourceTypesProto :
        configProto.getResourceNameGenerationList()) {
      ResourceNameMessageConfig messageResourceTypeConfig =
          ResourceNameMessageConfig.createResourceNameMessageConfig(
              messageResourceTypesProto, defaultPackage);
      mutableMap.put(messageResourceTypeConfig.messageName(), messageResourceTypeConfig);
    }
  }

  private static void insertMessageResourceTypesConfigFromAnnotations(
      List<ProtoFile> protoFiles,
      Map<Resource, ProtoFile> resourceDefs,
      Map<ResourceSet, ProtoFile> resourceSetDefs,
      ProtoParser protoParser,
      HashMap<String, ResourceNameMessageConfig> mutableMap) {
    for (ProtoFile protoFile : protoFiles) {
      for (MessageType message : protoFile.getMessages()) {
        ResourceNameMessageConfig resourceNameMessageConfig =
            ResourceNameMessageConfig.createResourceNameMessageConfig(
                message, resourceDefs, resourceSetDefs, protoParser);
        if (resourceNameMessageConfig != null) {
          mutableMap.put(message.getFullName(), resourceNameMessageConfig);
        }
      }
    }
  }

  static ResourceNameMessageConfigs createMessageResourceTypesConfig(
      DiscoApiModel model, ConfigProto configProto, String defaultPackage) {
    ImmutableMap.Builder<String, ResourceNameMessageConfig> builder = ImmutableMap.builder();
    for (ResourceNameMessageConfigProto messageResourceTypesProto :
        configProto.getResourceNameGenerationList()) {
      ResourceNameMessageConfig messageResourceTypeConfig =
          ResourceNameMessageConfig.createResourceNameMessageConfig(
              messageResourceTypesProto, defaultPackage);
      builder.put(messageResourceTypeConfig.messageName(), messageResourceTypeConfig);
    }
    ImmutableMap<String, ResourceNameMessageConfig> messageResourceTypeConfigMap = builder.build();

    ListMultimap<String, FieldModel> fieldsByMessage = ArrayListMultimap.create();
    DiscoGapicNamer discoGapicNamer = new DiscoGapicNamer();

    for (Method method : model.getDocument().methods()) {
      String fullName = discoGapicNamer.getRequestMessageFullName(method, defaultPackage);
      ResourceNameMessageConfig messageConfig = messageResourceTypeConfigMap.get(fullName);
      if (messageConfig == null) {
        continue;
      }
      for (Schema property : method.parameters().values()) {
        if (messageConfig.getEntityNameForField(property.getIdentifier()) != null) {
          fieldsByMessage.put(fullName, DiscoveryField.create(property, model));
        }
      }
    }
    return new AutoValue_ResourceNameMessageConfigs(messageResourceTypeConfigMap, fieldsByMessage);
  }

  public boolean isEmpty() {
    return getResourceTypeConfigMap().isEmpty();
  }

  boolean fieldHasResourceName(FieldModel field) {
    return fieldHasResourceName(field.getParentFullName(), field.getSimpleName());
  }

  private boolean fieldHasResourceName(String messageFullName, String fieldSimpleName) {
    return getResourceNameOrNullForField(messageFullName, fieldSimpleName) != null;
  }

  String getFieldResourceName(FieldModel field) {
    return getFieldResourceName(field.getParentFullName(), field.getSimpleName());
  }

  private String getFieldResourceName(String messageSimpleName, String fieldSimpleName) {
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
