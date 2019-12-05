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

import com.google.api.ResourceDescriptor;
import com.google.api.ResourceReference;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.ResourceNameMessageConfigProto;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.discovery.Schema;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ListMultimap;
import java.util.*;

/** Configuration of the resource name types for all message field. */
@AutoValue
public abstract class ResourceNameMessageConfigs {

  /** Get a map from fully qualified message names to resource name configs of its fields. */
  abstract ImmutableMap<String, ResourceNameMessageConfig> getResourceTypeConfigMap();

  /**
   * Get a map from fully qualified message names to Fields, where each field has a resource name
   * defined.
   */
  public abstract ListMultimap<String, FieldModel> getFieldsWithResourceNamesByMessage();

  @VisibleForTesting
  static ResourceNameMessageConfigs createFromAnnotations(
      DiagCollector diagCollector,
      List<ProtoFile> protoFiles,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      ProtoParser parser,
      Map<String, ResourceDescriptorConfig> descriptorConfigMap,
      Map<String, String> childParentResourceMap) {
    ImmutableMap.Builder<String, ResourceNameMessageConfig> builder = ImmutableMap.builder();

    for (ProtoFile protoFile : protoFiles) {
      for (MessageType message : protoFile.getMessages()) {
        ImmutableMap.Builder<String, String> fieldEntityMapBuilder = ImmutableMap.builder();

        // Handle resource definitions.
        ResourceDescriptor resourceDescriptor = parser.getResourceDescriptor(message);
        if (resourceDescriptor != null) {
          collectFieldEntityMapForResourceDefinition(
              fieldEntityMapBuilder, resourceDescriptor, message);
        }

        // Handle resource references.
        collectFieldEntityMapForResourceReference(
            fieldEntityMapBuilder, parser, message, resourceNameConfigs, childParentResourceMap);

        ImmutableMap<String, String> fieldEntityMap = fieldEntityMapBuilder.build();
        if (fieldEntityMap.size() > 0) {
          ResourceNameMessageConfig messageConfig =
              new AutoValue_ResourceNameMessageConfig(message.getFullName(), fieldEntityMap);
          builder.put(messageConfig.messageName(), messageConfig);
        }
      }
    }

    ImmutableMap<String, ResourceNameMessageConfig> map = builder.build();
    map.entrySet().forEach(System.out::println);
    return new AutoValue_ResourceNameMessageConfigs(map, createFieldsByMessage(protoFiles, map));
  }

  private static void collectFieldEntityMapForResourceDefinition(
      ImmutableMap.Builder<String, String> fieldEntityMap,
      ResourceDescriptor resourceDescriptor,
      MessageType message) {
    String resourceFieldName = resourceDescriptor.getNameField();
    if (resourceFieldName.isEmpty()) {
      resourceFieldName = "name";
    }

    String entityName =
        ResourceDescriptorConfig.getUnqualifiedTypeName(resourceDescriptor.getType());
    if (resourceDescriptor.getPatternList().size() > 1) {
      entityName = entityName + "Oneof";
    }
    fieldEntityMap.put(resourceFieldName, entityName);
  }

  private static void collectFieldEntityMapForResourceReference(
      ImmutableMap.Builder<String, String> fieldEntityMap,
      ProtoParser parser,
      MessageType message,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      Map<String, String> childParentResourceMap) {
    for (Field field : message.getFields()) {
      ResourceReference reference = parser.getResourceReference(field);
      if (reference == null) {
        continue;
      }
      String childType = reference.getChildType();
      String type = reference.getType();

      Preconditions.checkArgument(
          childType.isEmpty() || type.isEmpty(),
          "At least one of child_type and type should be set: %s",
          field);
      Preconditions.checkArgument(
          !childType.isEmpty() || !type.isEmpty(),
          "Only one of child_type and type should be set: %s",
          field);

      if (!childType.isEmpty()) {
        ResourceNameConfig parentResource =
            resourceNameConfigs.get(
                ResourceDescriptorConfig.getUnqualifiedTypeName(
                    childParentResourceMap.get(childType)));
        Preconditions.checkArgument(
            parentResource != null, "Referencing non-existing parent resource: %s", childType);
        fieldEntityMap.put(field.getSimpleName(), parentResource.getEntityId());
        continue;
      }

      if (type.equals("*")) {
        fieldEntityMap.put(field.getSimpleName(), "*");
        continue;
      }

      String unqualifiedResourceType = ResourceDescriptorConfig.getUnqualifiedTypeName(type);
      ResourceNameConfig resourceNameConfig =
          resourceNameConfigs.get(unqualifiedResourceType + "Oneof");
      if (resourceNameConfig != null
          && resourceNameConfig.getResourceNameType() == ResourceNameType.ONEOF) {
        fieldEntityMap.put(field.getSimpleName(), unqualifiedResourceType + "Oneof");
        continue;
      }
      resourceNameConfig = resourceNameConfigs.get(unqualifiedResourceType);
      Preconditions.checkArgument(
          resourceNameConfig != null, "Referencing non-existing resource: %s", type);
      fieldEntityMap.put(field.getSimpleName(), unqualifiedResourceType);
    }
  }

  @VisibleForTesting
  static ResourceNameMessageConfigs createFromGapicConfigOnly(
      List<ProtoFile> protoFiles, ConfigProto configProto, String defaultPackage) {
    ImmutableMap<String, ResourceNameMessageConfig> map =
        createMapFromGapicConfig(configProto, defaultPackage);
    return new AutoValue_ResourceNameMessageConfigs(map, createFieldsByMessage(protoFiles, map));
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

  private static ImmutableMap<String, ResourceNameMessageConfig> createMapFromGapicConfig(
      ConfigProto configProto, String defaultPackage) {
    ImmutableMap.Builder<String, ResourceNameMessageConfig> builder = ImmutableMap.builder();
    for (ResourceNameMessageConfigProto messageResourceTypesProto :
        configProto.getResourceNameGenerationList()) {
      ResourceNameMessageConfig messageResourceTypeConfig =
          ResourceNameMessageConfig.createResourceNameMessageConfig(
              messageResourceTypesProto, defaultPackage);
      builder.put(messageResourceTypeConfig.messageName(), messageResourceTypeConfig);
    }
    return builder.build();
  }

  static ResourceNameMessageConfigs createMessageResourceTypesConfig(
      DiscoApiModel model, ConfigProto configProto, String defaultPackage) {
    ImmutableMap<String, ResourceNameMessageConfig> messageResourceTypeConfigMap =
        createMapFromGapicConfig(configProto, defaultPackage);

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

  public boolean fieldHasResourceName(String messageFullName, String fieldSimpleName) {
    return getResourceNameOrNullForField(messageFullName, fieldSimpleName) != null;
  }

  String getFieldResourceName(FieldModel field) {
    return getFieldResourceName(field.getParentFullName(), field.getSimpleName());
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
