/* Copyright 2019 Google LLC
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

import com.google.api.codegen.util.Name;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Represents definition of 5 well-known resource name types: project, location, org, folder,
 * billingAccount.
 */
public class WellKnownResourceNames {

  private static final SingleResourceNameConfig.Builder PROJECT =
      SingleResourceNameConfig.newBuilder()
          .setNamePattern("project/{project}")
          .setNameTemplate(PathTemplate.create("project/{project}"))
          .setEntityId("project")
          .setEntityName(Name.anyLower("project"));

  private final Map<String, ResourceNameConfig> RESOURCE_NAMES;
  private final ProtoFile sourceProtoFile;
  private final Set<ResourceNameConfig> referencedResourceNames;
  private final Map<String, String> entityNameTypeNameMap;

  // Because we don't have resource definitions for these well-known resource name types,
  // we need to assign a source file to them so that we know how to calculate a consistent
  // for them.
  public WellKnownResourceNames(ProtoFile sourceProtoFile) {
    this.sourceProtoFile = sourceProtoFile;
    this.referencedResourceNames = new HashSet<>();
    RESOURCE_NAMES =
        ImmutableMap.<String, ResourceNameConfig>builder()
            .put(
                "cloudresourcemanager.googleapis.com/Project",
                PROJECT.setAssignedProtoFile(sourceProtoFile).build())
            .build();
    this.entityNameTypeNameMap =
        ImmutableMap.of("project", "cloudresourcemanager.googleapis.com/Project");
  }

  public boolean containsType(String type) {
    return RESOURCE_NAMES.containsKey(type);
  }

  public boolean containsTypeWithEntityName(String entityName) {
    return entityNameTypeNameMap.containsKey(entityName);
  }

  public List<ResourceNameConfig> getReferencedResourceNameConfigs() {
    return ImmutableList.<ResourceNameConfig>builder().addAll(referencedResourceNames).build();
  }

  /**
   * Gets a ResourceNameConfig based on the type, mark it as referenced and returns it. We need to
   * keep track of the all the referenced resource names because we need to generate resource name
   * classes or formatting helper methods for them.
   */
  public ResourceNameConfig reference(String type) {
    Preconditions.checkArgument(containsType(type), "not a well-known resource type: %s", type);
    ResourceNameConfig resource = RESOURCE_NAMES.get(type);
    referencedResourceNames.add(resource);
    return resource;
  }

  /**
   * Gets a ResourceNameConfig based on the type, mark it as referenced and returns it. We need to
   * keep track of the all the referenced resource names because we need to generate resource name
   * classes or formatting helper methods for them.
   */
  public ResourceNameConfig referenceByEntityName(String entityName) {
    Preconditions.checkArgument(
        containsTypeWithEntityName(entityName),
        "not a well-known resource type entity name: %s",
        entityName);
    ResourceNameConfig resource = RESOURCE_NAMES.get(entityNameTypeNameMap.get(entityName));
    referencedResourceNames.add(resource);
    return resource;
  }
}
