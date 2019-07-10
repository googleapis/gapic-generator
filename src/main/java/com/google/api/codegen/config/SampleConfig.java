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

import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.samplegen.v1.RequestFieldProto;
import com.google.api.codegen.samplegen.v1.ResponseStatementProto;
import com.google.api.codegen.samplegen.v1.SampleConfigProto;
import com.google.api.codegen.samplegen.v1.SampleSpecProto;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.auto.value.AutoValue;
import com.google.common.base.Preconditions;
import com.google.common.collect.HashBasedTable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/** SampleConfig represents configurations of a sample. */
// Note: This class was used as an intermediate data structure to hold some information
// about samples. Now that we are flattening out sample configuration, it's very likely
// that we will no longer need a class to serve that purpose. However, to conform to
// the model/config/context style used by gapic-generator, we will reuse this class to
// hold all the information derived from sample yaml configs.
//
// TODO(hzyi): In order to have a smooth transition, all new fields added are marked as Nullable to
// not break existing code. Once we finish plumbing the pipeline to take the new configuration for
// samples, we can remove them.
@AutoValue
public abstract class SampleConfig {

  @Nullable
  public abstract String id();

  @Nullable
  public abstract String title();

  @Nullable
  public abstract String description();

  @Nullable
  public abstract InterfaceConfig interfaceConfig();

  @Nullable
  public abstract MethodConfig methodConfig();

  @Nullable
  public abstract ImmutableList<RequestFieldProto> requestConfigs();

  @Nullable
  public abstract ImmutableList<ResponseStatementProto> responseConfigs();

  @Nullable
  public abstract String regionTag();

  @Nullable
  public abstract String callingPattern();

  public boolean usesDefaultCallingForm() {
    return callingPattern() == null
        || callingPattern().isEmpty()
        || callingPattern().equals("default");
  }

  @Nullable
  public abstract CallingForm callingForm();

  @Nullable
  public abstract SampleValueSet valueSet();

  public abstract SampleSpec.SampleType type();

  public static SampleConfig create(
      String regionTag,
      CallingForm callingForm,
      SampleValueSet valueSet,
      SampleSpec.SampleType type) {
    return newBuilder()
        .regionTag(regionTag)
        .callingForm(callingForm)
        .valueSet(valueSet)
        .type(type)
        .build();
  }

  public static Builder newBuilder() {
    return new AutoValue_SampleConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder id(String val);

    public abstract Builder title(String val);

    public abstract Builder description(String val);

    public abstract Builder interfaceConfig(InterfaceConfig val);

    public abstract Builder methodConfig(MethodConfig val);

    public abstract Builder requestConfigs(ImmutableList<RequestFieldProto> val);

    public abstract Builder responseConfigs(ImmutableList<ResponseStatementProto> val);

    public abstract Builder regionTag(String val);

    public abstract Builder callingForm(CallingForm val);

    public abstract Builder callingPattern(String val);

    public abstract Builder valueSet(SampleValueSet val);

    public abstract Builder type(SampleSpec.SampleType val);

    public abstract SampleConfig build();
  }

  public static ImmutableTable<String, String, ImmutableList<SampleConfig>> createSampleConfigTable(
      SampleConfigProto sampleConfigProto, final Map<String, InterfaceConfig> interfaceConfigMap) {
    // First, apply region tag as IDs if IDs are not given
    List<SampleSpecProto> sampleSpecs = new ArrayList<>();
    for (SampleSpecProto spec : sampleSpecs) {
      if (spec.getId().isEmpty()) {
        spec = spec.toBuilder().setId(spec.getRegionTag()).build();
      }
      sampleSpecs.add(spec);
    }

    // Then, check user specified sample IDs do not clash
    Set<String> distinctIds = new HashSet<>();
    Set<String> duplicateIds =
        sampleSpecs
            .stream()
            .map(s -> s.getId())
            .filter(id -> !id.isEmpty())
            .filter(s -> !distinctIds.add(s))
            .collect(Collectors.toSet());
    Preconditions.checkArgument(
        duplicateIds.isEmpty(),
        "Found duplicate IDs: %s",
        duplicateIds.stream().collect(Collectors.joining(", ")));

    // Next, flatten the calling pattern list so we have one per sample
    List<SampleSpecProto> flattenedSampleSpecs = new ArrayList<>();
    for (SampleSpecProto spec : sampleSpecs) {
      if (spec.getCallingPatternsList().isEmpty()) {
        sampleSpecs.add(spec.toBuilder().addCallingPatterns("").build());
      }
      for (String pattern : spec.getCallingPatternsList()) {
        sampleSpecs.add(spec.toBuilder().addCallingPatterns(pattern).build());
      }
    }

    // These are not the final calling pattern values, because the
    // regexes specified in the config need to be matched against
    // language-specific calling pattern definitions.

    // Construct the table.
    HashBasedTable<String, String, ArrayList<SampleConfig>> table = HashBasedTable.create();
    for (SampleSpecProto sampleSpec : flattenedSampleSpecs) {
      SampleConfig config = createOneSampleConfig(sampleSpec, interfaceConfigMap);
      if (!table.contains(sampleSpec.getService(), sampleSpec.getRpc())) {
        table.put(sampleSpec.getService(), sampleSpec.getRpc(), new ArrayList<>());
      }
      table.get(sampleSpec.getService(), sampleSpec.getRpc()).add(config);
    }

    // Make an immutable copy.
    return table
        .cellSet()
        .stream()
        .collect(
            ImmutableTable.toImmutableTable(
                Table.Cell::getRowKey,
                Table.Cell::getColumnKey,
                v -> ImmutableList.copyOf(v.getValue())));
  }

  private static SampleConfig createOneSampleConfig(
      SampleSpecProto sampleSpec, Map<String, InterfaceConfig> interfaceConfigMap) {
    InterfaceConfig interfaceConfig = interfaceConfigMap.get(sampleSpec.getService());
    Preconditions.checkNotNull(
        interfaceConfig, "can't find interface named %s", sampleSpec.getService());
    Preconditions.checkState(
        interfaceConfig instanceof GapicInterfaceConfig,
        "can't generate samples for non-gapic libraries");

    GapicInterfaceConfig gapicInterfaceConfig = (GapicInterfaceConfig) interfaceConfig;
    MethodConfig methodConfig = gapicInterfaceConfig.getMethodConfigMap().get(sampleSpec.getRpc());
    Preconditions.checkNotNull(methodConfig, "can't find method named %s", sampleSpec.getRpc());

    return SampleConfig.newBuilder()
        .id(sampleSpec.getId())
        .title(sampleSpec.getTitle())
        .description(sampleSpec.getDescription())
        .interfaceConfig(gapicInterfaceConfig)
        .methodConfig(methodConfig)
        .requestConfigs(
            ImmutableList.<RequestFieldProto>builder().addAll(sampleSpec.getRequestList()).build())
        .responseConfigs(
            ImmutableList.<ResponseStatementProto>builder()
                .addAll(sampleSpec.getResponseList())
                .build())
        .regionTag(sampleSpec.getRegionTag())
        .callingPattern(
            sampleSpec.getCallingPatternsCount() > 0
                ? sampleSpec.getCallingPatternsList().get(0)
                : "")
        .type(SampleSpec.SampleType.STANDALONE)
        .build();
  }
}
