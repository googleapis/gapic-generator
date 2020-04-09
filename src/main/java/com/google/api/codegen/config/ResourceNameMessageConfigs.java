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

import static com.google.common.base.Preconditions.checkNotNull;

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
import com.google.common.collect.ImmutableListMultimap;
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
      Map<String, List<ResourceDescriptorConfig>> childParentResourceMap) {
    ImmutableMap.Builder<String, ResourceNameMessageConfig> builder = ImmutableMap.builder();

    for (ProtoFile protoFile : protoFiles) {
      for (MessageType message : protoFile.getMessages()) {
        ImmutableListMultimap.Builder<String, String> fieldEntityMapBuilder =
            ImmutableListMultimap.builder();

        // Handle resource definitions.
        ResourceDescriptor resourceDescriptor = parser.getResourceDescriptor(message);
        if (resourceDescriptor != null) {
          loadFieldEntityPairFromResourceAnnotation(
              fieldEntityMapBuilder, resourceDescriptor, message);
        }

        // Handle resource references.
        loadFieldEntityPairFromResourceReferenceAnnotation(
            fieldEntityMapBuilder,
            parser,
            message,
            resourceNameConfigs,
            descriptorConfigMap,
            childParentResourceMap);

        ImmutableListMultimap<String, String> fieldEntityMap = fieldEntityMapBuilder.build();
        if (fieldEntityMap.size() > 0) {
          ResourceNameMessageConfig messageConfig =
              new AutoValue_ResourceNameMessageConfig(message.getFullName(), fieldEntityMap);
          builder.put(messageConfig.messageName(), messageConfig);
        }
      }
    }
    ImmutableMap<String, ResourceNameMessageConfig> map = builder.build();
    return new AutoValue_ResourceNameMessageConfigs(map, createFieldsByMessage(protoFiles, map));
  }

  /**
   * Create a field name to resource name type mapping from (google.api).resource annotation put it
   * to fieldEntityMap.
   */
  private static void loadFieldEntityPairFromResourceAnnotation(
      ImmutableListMultimap.Builder<String, String> fieldEntityMap,
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

  /**
   * Create field name to resource name type mappings from (google.api).resource_reference
   * annotations and put them to fieldEntityMap.
   */
  private static void loadFieldEntityPairFromResourceReferenceAnnotation(
      ImmutableListMultimap.Builder<String, String> fieldEntityMap,
      ProtoParser parser,
      MessageType message,
      Map<String, ResourceNameConfig> resourceNameConfigs,
      Map<String, ResourceDescriptorConfig> descriptorConfigMap,
      Map<String, List<ResourceDescriptorConfig>> childParentResourceMap) {
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

      if (type.equals("*") || childType.equals("*")) {
        fieldEntityMap.put(field.getSimpleName(), "*");
        continue;
      }

      List<ResourceDescriptorConfig> referencedResources;
      if (!childType.isEmpty()) {
        referencedResources = childParentResourceMap.get(childType);
        checkNotNull(referencedResources, "unable to find parent resources of %s", childType);
      } else {
        ResourceDescriptorConfig referencedResource = descriptorConfigMap.get(type);
        checkNotNull(referencedResource, "unable to find referenced resource %s", type);
        referencedResources = Collections.singletonList(referencedResource);
      }

      for (ResourceDescriptorConfig descriptor : referencedResources) {
        String unqualifiedName = descriptor.getDerivedEntityName();
        ResourceNameConfig resource = resourceNameConfigs.get(unqualifiedName);

        switch (resource.getResourceNameType()) {
          case ANY:
            fieldEntityMap.put(field.getSimpleName(), "*");
            break;
          case SINGLE:
            fieldEntityMap.put(field.getSimpleName(), unqualifiedName);
            break;
          case ONEOF:
            fieldEntityMap.put(field.getSimpleName(), unqualifiedName);
            if (((ResourceNameOneofConfig) resource).hasAnyResourceNamePattern()) {
              fieldEntityMap.put(field.getSimpleName(), "*");
            }
            break;
          default:
            throw new IllegalArgumentException(
                "unknown resource name type: " + resource.getResourceNameType());
        }
      }
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
            if (!messageConfig.getEntityNamesForField(field.getSimpleName()).isEmpty()) {
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
        if (!messageConfig.getEntityNamesForField(property.getIdentifier()).isEmpty()) {
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
    return !getResourceNamesForField(messageFullName, fieldSimpleName).isEmpty();
  }

  List<String> getFieldResourceNames(FieldModel field) {
    return getFieldResourceNames(field.getParentFullName(), field.getSimpleName());
  }

  public List<String> getFieldResourceNames(String messageSimpleName, String fieldSimpleName) {
    List<String> resourceNames = getResourceNamesForField(messageSimpleName, fieldSimpleName);
    Preconditions.checkArgument(
        !resourceNames.isEmpty(),
        "Field %s of message %s does not have a resource name.",
        fieldSimpleName,
        messageSimpleName);
    return resourceNames;
  }

  private List<String> getResourceNamesForField(String messageSimpleName, String fieldSimpleName) {
    ResourceNameMessageConfig messageResourceTypeConfig =
        getResourceTypeConfigMap().get(messageSimpleName);
    if (messageResourceTypeConfig == null) {
      return Collections.emptyList();
    }
    return messageResourceTypeConfig.getEntityNamesForField(fieldSimpleName);
  }
}
