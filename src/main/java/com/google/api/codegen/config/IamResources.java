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
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/* Utility class for ResourceDescriptorConfig. */
public class IamResources {

  private static final Set<String> IAM_METHOD_NAMES =
      ImmutableSet.of("GetIamPolicy", "SetIamPolicy", "TestIamPermissions");
  private static final Set<String> IAM_REQUEST_MESSAGE_NAMES =
      ImmutableSet.of(
          "google.iam.v1.GetIamPolicyRequest",
          "google.iam.v1.SetIamPolicyRequest",
          "google.iam.v1.TestIamPermissionsRequest");
  private static final String DEFAULT_MULTI_PATTERN_IAM_RESOURCE_TYPE = "IamResource";

  private IamResources() {
    // utility class
  }

  /**
   * Creates an ResourceDescriptorConfig from the google.api.http annotations on IAM methods.
   * Returns null if the API does not hava IAM methods.
   *
   * <p>`GetIamPolicy`, `SetIamPolicy` and `TestIamPermisions` are the three IAM methods that a lot
   * of APIs implement. However, since the request messages for these methods are defined in
   * `google.iam.v1`, each individual API is unable to add annotation to the `resource` field to
   * specify accepted resource name patterns of this field.
   *
   * <p>As a mitigation, we derive the resource name configs these methods need from the
   * `google.api.http` annotations.
   */
  @Nullable
  public static ResourceDescriptorConfig createIamResourceDescriptor(List<ProtoFile> protoFiles) {
    HashMap<String, ResourceDescriptorConfig> configs = new HashMap<>();
    for (ProtoFile protoFile : protoFiles) {
      for (Interface service : protoFile.getInterfaces()) {
        for (Method method : service.getMethods()) {
          // We only care about IAM methods
          if (!IAM_METHOD_NAMES.contains(method.getSimpleName())) {
            continue;
          }

          List<CollectionPattern> collectionPatterns =
              CollectionPattern.getAllCollectionPatternsFromMethod(method);
          if (collectionPatterns.isEmpty()) {
            continue;
          }
          ResourceDescriptorConfig config =
              createResourceNameDescriptorFromPatterns(
                  collectionPatterns, protoFile, new ProtoParser(true).getServiceAddress(service));
          configs.put(config.getUnifiedResourceType(), config);

          // The three IAM methods should always have the same http mapping, so we can break
          // if we have found one already
          break;
        }
      }
    }

    // We are unable to handle the case where services of a multi-service API have different http
    // bindings for IAM methods, because they share the same set of request messages, and we
    // wouldn't be able
    // to introduce multiple sets of resource_reference annotations on a single request message.
    // We don't yet have any API that has this need. But by failing preemptively in such a case it
    // might make troubleshooting easier should we need to handle this case in the future.

    Preconditions.checkState(
        configs.size() <= 1,
        "multiple services have IAM methods, and they have different HTTP mappings: %s",
        configs
            .values()
            .stream()
            .map(ResourceDescriptorConfig::getAssignedProtoFile)
            .map(ProtoFile::getSimpleName)
            .collect(Collectors.joining(", ")));

    return configs.size() == 0 ? null : configs.values().iterator().next();
  }

  public static Map<String, ResourceNameMessageConfig> createIamResourceNameMessageConfigs(
      List<ProtoFile> protoFiles) {

    ResourceDescriptorConfig config = createIamResourceDescriptor(protoFiles);

    if (config == null) {
      // There are no IAM methods.
      return Collections.emptyMap();
    } else {
      // Trim the service name and convert to snake_case to conform to gapic config v1
      String unifiedTypeName = config.getUnifiedResourceType();
      int indexOfSlash = unifiedTypeName.indexOf("/");
      String entityName = config.getUnifiedResourceType().substring(indexOfSlash + 1);
      if (config.getPatterns().size() > 1) {
        entityName += "Oneof";
      }

      // The resource field in IAM request messages is always called resource
      ImmutableMap<String, String> fieldEntityMap = ImmutableMap.of("resource", entityName);
      return IAM_REQUEST_MESSAGE_NAMES
          .stream()
          .map(name -> new AutoValue_ResourceNameMessageConfig(name, fieldEntityMap))
          .collect(ImmutableMap.toImmutableMap(c -> c.messageName(), c -> c));
    }
  }

  private static ResourceDescriptorConfig createResourceNameDescriptorFromPatterns(
      List<CollectionPattern> patterns, ProtoFile assignedProtoFile, String serviceName) {

    String unifiedTypeName =
        patterns.size() == 1
            ? Name.anyLower(patterns.get(0).getSimpleName()).toUpperCamel()
            : DEFAULT_MULTI_PATTERN_IAM_RESOURCE_TYPE;

    return new AutoValue_ResourceDescriptorConfig(
        serviceName + "/" + unifiedTypeName,
        patterns
            .stream()
            .map(CollectionPattern::getTemplatizedResourcePath)
            .collect(ImmutableList.toImmutableList()),
        "resource",
        ResourceDescriptor.History.HISTORY_UNSPECIFIED,
        assignedProtoFile);
  }
}
