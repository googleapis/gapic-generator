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
import com.google.api.codegen.util.Name;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.pathtemplate.ValidationException;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.stream.Collectors;

@AutoValue
public abstract class ResourceDescriptorConfig {

  public abstract String getUnifiedResourceType();

  public abstract ImmutableList<String> getPatterns();

  public abstract String getNameField();

  public abstract ResourceDescriptor.History getHistory();

  public abstract boolean getRequiresOneofConfig();

  public abstract String getSinglePattern();

  /**
   * Returns the proto file to which the resource name config has been assigned. This is required to
   * ensure that a consistent namespace can be calculated for the resource name.
   */
  public abstract ProtoFile getAssignedProtoFile();

  public static ResourceDescriptorConfig from(
      ResourceDescriptor descriptor, ProtoFile assignedProtoFile) {
    // The logic for requiresOneofConfig and requiresSinglePattern is finicky, so let's lay out
    // the desired result for all possible combinations of History and number of patterns:
    // (history, patterns) -> (requiresOneofConfig, requiresSinglePattern)
    //
    // (HISTORY_UNSPECIFIED,       1)  -> (false, true)
    // (HISTORY_UNSPECIFIED,       2+) -> (true, false)
    // (ORIGINALLY_SINGLE_PATTERN, 1)  -> (false, true) !!! WARNING, very odd
    // (ORIGINALLY_SINGLE_PATTERN, 2+) -> (true, true)
    // (FUTURE_MULTI_PATTERN,      1)  -> (true, false)
    // (FUTURE_MULTI_PATTERN,      2+) -> (true, false) !!! WARNING, very odd

    boolean requiresOneofConfig =
        descriptor.getHistory() == ResourceDescriptor.History.FUTURE_MULTI_PATTERN
            || descriptor.getPatternList().size() > 1;
    boolean requiresSinglePattern =
        descriptor.getHistory() == ResourceDescriptor.History.ORIGINALLY_SINGLE_PATTERN
            || (descriptor.getHistory() == ResourceDescriptor.History.HISTORY_UNSPECIFIED
                && descriptor.getPatternList().size() == 1);

    return new AutoValue_ResourceDescriptorConfig(
        descriptor.getType(),
        ImmutableList.copyOf(descriptor.getPatternList()),
        descriptor.getNameField(),
        descriptor.getHistory(),
        requiresOneofConfig,
        requiresSinglePattern ? descriptor.getPattern(0) : "",
        assignedProtoFile);
  }

  private String buildEntityName() {
    return getUnifiedResourceType().substring(getUnifiedResourceType().lastIndexOf("/") + 1)
        + "Name";
  }

  private String buildEntityName(String pattern) {
    if (pattern.equals(getSinglePattern())) {
      return buildEntityName();
    } else {
      return "Project" + buildEntityName();
    }
  }

  private List<ResourceNameConfig> buildSingleResourceNameConfigs(DiagCollector diagCollector) {
    try {
      return getPatterns()
          .stream()
          .map(
              p ->
                  SingleResourceNameConfig.newBuilder()
                      .setNamePattern(p)
                      .setNameTemplate(PathTemplate.create(p))
                      .setAssignedProtoFile(getAssignedProtoFile())
                      .setEntityId(buildEntityName(p))
                      .setEntityName(Name.anyCamel(buildEntityName(p)))
                      .build())
          .collect(Collectors.toList());
    } catch (ValidationException e) {
      // Catch exception that may be thrown by PathTemplate.create
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
      return ImmutableList.of();
    }
  }

  public ResourceNameOneofConfig buildResourceNameOneofConfig(DiagCollector diagCollector) {
    String oneOfName = buildEntityName();
    return new AutoValue_ResourceNameOneofConfig(
        oneOfName,
        Name.anyCamel(oneOfName),
        buildSingleResourceNameConfigs(diagCollector),
        getAssignedProtoFile());
  }
}
