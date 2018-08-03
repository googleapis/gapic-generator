/* Copyright 2018 Google LLC
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

import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.SampleConfiguration;
import com.google.api.codegen.SampleConfiguration.SampleTypeConfiguration;
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Class {@code SampleSpec} stores the sample specification for a given method, and provides methods
 * to easily access them by calling form and sample type.
 */
public class SampleSpec {

  /** A reference to the {@code SampleConfiguration} from which the other fields are derived. */
  private final SampleConfiguration sampleConfiguration;

  /** All the {@code SampleValueSets} defined for this method. */
  private final List<SampleValueSet> valueSets;

  /** Whether samples have been specified (ie. need to be emitted) for this method. */
  private final boolean specified;

  /** The various types of supported samples. */
  public enum SampleType {
    IN_CODE,
    STANDALONE,
    EXPLORER,
  }

  /**
   * A class used by {@code getMatchingValueSets} to return a value set needed to generate a sample
   * for a given method, sample type, and calling form, together with region tag that should be used
   * in that sample.
   */
  @AutoValue
  public abstract static class ValueSetAndTags {

    public abstract SampleValueSet values();

    /** Returns the region tag prefix to be used for this sample. */
    @Nullable
    public abstract String regionTag();

    public static Builder newBuilder() {
      return new AutoValue_SampleSpec_ValueSetAndTags.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {

      public abstract Builder values(SampleValueSet sets);

      public abstract Builder regionTag(String tag);

      public abstract ValueSetAndTags build();
    }
  }

  public SampleSpec(MethodConfigProto methodConfigProto) {
    specified = methodConfigProto.hasSamples();
    sampleConfiguration = methodConfigProto.getSamples();
    valueSets = methodConfigProto.getSampleValueSetsList();

    HashMap<String, SampleValueSet> setMap = new HashMap<>();
    for (SampleValueSet valueSet : valueSets) {
      String id = valueSet.getId();
      SampleValueSet oldSet = setMap.put(id, valueSet);
      if (oldSet != null) {
        throw new IllegalArgumentException(
            String.format(
                "in method \"%s\": duplicate value set id: \"%s\"",
                methodConfigProto.getName(), id));
      }
    }
  }

  public boolean isConfigured() {
    return specified;
  }

  /**
   * Returns true if id is a regexp match for the given expression. This is the function used to
   * determine whether calling forms and value sets match expressions referencing them by ID.
   *
   * @param expression The regex to use for matching
   * @param id The ID to be matched against the regex
   * @return True iff id matches expression
   */
  private static boolean expressionMatchesId(String expression, String id) {
    return id.matches(expression);
  }

  /**
   * Returns the SampleValueSets that were specified for this methodForm and sampleType.
   *
   * @param methodForm The calling form for which value sets are requested
   * @param sampleType The sample type for which value sets are requested
   * @return A set of SampleValueSets for methodForm andSampleType
   */
  public List<ValueSetAndTags> getMatchingValueSets(CallingForm methodForm, SampleType sampleType) {
    String methodFormString = Name.anyCamel(methodForm.toString()).toLowerUnderscore();

    // Get the `SampleTypeConfigs` configured for this `methodForm`.
    List<SampleTypeConfiguration> matchingSamples =
        getConfigFor(sampleType)
            .stream()
            .filter(
                sampleConfig ->
                    sampleConfig
                        .getCallingFormsList()
                        .stream()
                        .anyMatch(expression -> expressionMatchesId(expression, methodFormString)))
            .collect(Collectors.toList());

    // Construct a `ValueSetAndTags` for each sample specified in each element of `matchingSamples`.
    List<ValueSetAndTags> result = new ArrayList<>();
    Set<String> generated = new HashSet<>();
    for (SampleValueSet vset : valueSets) {
      for (SampleTypeConfiguration sample : matchingSamples) {
        for (String valueSetExpression : sample.getValueSetsList()) {
          if (expressionMatchesId(valueSetExpression, vset.getId())) {
            result.add(
                ValueSetAndTags.newBuilder().values(vset).regionTag(sample.getRegionTag()).build());
            generated.add(vset.getId());
          }
        }
      }
    }
    return result;
  }

  /** Returns the single {@code SampleTypeConfiguration} for the specified {@code sampleType}. */
  private List<SampleTypeConfiguration> getConfigFor(SampleType sampleType) {
    switch (sampleType) {
      case STANDALONE:
        return sampleConfiguration.getStandaloneList();
      case IN_CODE:
        return sampleConfiguration.getInCodeList();
      case EXPLORER:
        return sampleConfiguration.getApiExplorerList();
      default:
        throw new IllegalArgumentException("unhandled SampleType: " + sampleType.toString());
    }
  }
}
