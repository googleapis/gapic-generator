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

import com.google.api.ResourceDescriptor;
import com.google.api.codegen.configgen.CollectionPattern;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import java.util.Set;

/* Utility class for ResourceDescriptorConfig. */
public class ResourceDescriptorConfigs {

  private static final Set<String> IAM_METHOD_NAMES =
      ImmutableSet.of("GetIamPolicy", "SetIamPolicy", "TestIamPermissions");
  private static final String DEFAULT_MULTI_PATTERN_IAM_RESOURCE_TYPE = "IamResource";

  private ResourceDescriptorConfigs() {
    // utility class
  }

  /**
   * Creates Resource name configs from the google.api.http annotations on IAM methods.
   *
   * <p>`GetIamPolicy`, `SetIamPolicy` and `TestIamPermisions` are the three IAM methods that a lot
   * of APIs implement. However, since the request messages for these methods are defined in
   * `google.iam.v1`, each individual API is unable to add annotation to the `resource` field to
   * specify accepted resource name patterns of this field.
   *
   * <p>As a mitigation, we derive the resource name configs these methods need from the
   * `google.api.http` annotations.
   */
  public static List<ResourceDescriptorConfig> createResourceNameDescriptorsFromIamMethods(
      ProtoFile protoFile) {
    ImmutableList.Builder<ResourceDescriptorConfig> configs = ImmutableList.builder();
    for (Interface apiInterface : protoFile.getInterfaces()) {
      for (Method method : apiInterface.getMethods()) {
        if (!IAM_METHOD_NAMES.contains(method.getSimpleName())) {
          continue;
        }

        List<CollectionPattern> collectionPatterns =
            CollectionPattern.getCollectionPatternsFromMethod(method);
        if (collectionPatterns.isEmpty()) {
          continue;
        }
        ResourceDescriptorConfig config = createResourceNameDescriptorFromPatterns(collectionPatterns, protoFile);
        configs.add(config);
        System.out.println(config);
      }
    }
    return configs.build();
  }

  private static ResourceDescriptorConfig createResourceNameDescriptorFromPatterns(
      List<CollectionPattern> patterns, ProtoFile assignedProtoFile) {

    String unifiedTypeName =
        patterns.size() == 1
            ? patterns.get(0).getSimpleName()
            : DEFAULT_MULTI_PATTERN_IAM_RESOURCE_TYPE;

    return new AutoValue_ResourceDescriptorConfig(
        "*/" + unifiedTypeName,
        patterns
            .stream()
            .map(CollectionPattern::getFieldPath)
            .collect(ImmutableList.toImmutableList()),
        "resource",
        ResourceDescriptor.History.FUTURE_MULTI_PATTERN,
        assignedProtoFile);
  }
}
