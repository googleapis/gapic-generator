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
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * Class {@code SampleSpec} stores the sample specification for a given method, and provides methods
 * to easily access them by calling form and sample type.
 */
public class SampleSpec {

  /** A reference to the {@code SampleConfiguration} from which the other fields are derived. */
  public final SampleConfiguration sampleConfiguration;

  /** All the {@code SampleValueSets} defined for this method. */
  public final List<SampleValueSet> valueSets;

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

    HashSet<String> ids = new HashSet<>();
    for (SampleValueSet valueSet : valueSets) {
      String id = valueSet.getId();
      if (!ids.add(id)) {
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
   * Returns all the `ids` that can match to one or more elements of `expressions` through regex.
   */
  private static <T> List<T> expressionsMatchIds(
      List<String> expressions, List<T> targets, Function<T, String> targetToId) {
    return targets
        .stream()
        .filter(
            t ->
                expressions.stream().anyMatch(exp -> expressionMatchesId(exp, targetToId.apply(t))))
        .collect(Collectors.toList());
  }

  /**
   * Returns all the valid combinations of calling forms of value sets.
   *
   * <p>We match calling forms specified by users with `allValidCallingForms`. If users did not
   * specify any calling forms, we match `defaultCallingForm` with `allValidCallingForms`. If we
   * found no matching calling forms, an empty list will be returned. Note this implies that
   * `defaultCallingForm` can match none of the calling forms in `allValidCallingForms`. For
   * example, the `defaultCallingForm` for a C# unary call is `Request`, but it's not a valid
   * calling form when generating asynchronous samples using the generated asynchonous public
   * method. In this case we will not generate samples for this method.
   *
   * <p>We match value sets specified by users with `this.values`. If we find no matching value
   * sets, we use the default one. For incode samples, we always use the default value set derived
   * from sample_code_init_fields for backward compatibility.
   *
   * <p>We should probably disable default value sets after incode samples stop using
   * `sample_code_init_fields`.
   */
  public List<SampleConfig> getSampleConfigs(
      List<CallingForm> allValidCallingForms,
      CallingForm defaultCallingForm,
      SampleValueSet defaultValueSet,
      SampleType type) {
    List<SampleConfig> sampleConfigs = new ArrayList<>();
    if (type == SampleType.EXPLORER) {
      throw new UnsupportedOperationException("API Explorer samples unimplemented yet.");
    } else if (type == SampleType.IN_CODE) {
      for (CallingForm form : allValidCallingForms) {
        sampleConfigs.add(SampleConfig.create("", form, defaultValueSet, type));
      }
    } else {
      for (SampleTypeConfiguration config : getConfigFor(type)) {
        List<CallingForm> matchingCallingForms = null;
        List<SampleValueSet> matchingValueSets = null;
        // match calling forms
        List<String> callingFormNames =
            config.getCallingFormsList().isEmpty()
                ? Collections.singletonList(
                    Name.anyCamel(defaultCallingForm.toString()).toLowerUnderscore())
                : config.getCallingFormsList();
        matchingCallingForms =
            expressionsMatchIds(
                callingFormNames, allValidCallingForms, CallingForm::toLowerUnderscore);

        // match value sets
        matchingValueSets =
            expressionsMatchIds(config.getValueSetsList(), valueSets, v -> v.getId());

        for (CallingForm form : matchingCallingForms) {
          for (SampleValueSet matchingValueSet : matchingValueSets) {
            sampleConfigs.add(
                SampleConfig.create(config.getRegionTag(), form, matchingValueSet, type));
          }
        }
      }
    }

    return sampleConfigs;
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
