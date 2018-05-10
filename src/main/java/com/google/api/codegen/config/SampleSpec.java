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
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.common.collect.ImmutableList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class SampleSpec stores the sample specification for a given method, and provides methods to
 * easily access them by calling form and sample type.
 */
public class SampleSpec {

  /** A reference to the Sample Configuration from which the other fields are derived. */
  private final SampleConfiguration sampleConfiguration;

  /** All the SampleValueSets defined for this method. */
  private final List<SampleValueSet> valueSets;

  private final boolean specified;

  /** The various types of supported samples. */
  public enum SampleType {
    IN_CODE,
    STANDALONE,
    EXPLORER,
  }

  public SampleSpec(MethodConfigProto methodConfigProto) {
    specified = methodConfigProto.hasSamples();
    sampleConfiguration = methodConfigProto.getSamples();
    valueSets = methodConfigProto.getSampleValueSetsList();

    HashSet<String> ids = new HashSet<>();
    for (SampleValueSet valueSet : valueSets) {
      String id = valueSet.getId();
      if (!ids.add(id)) {
        throw new IllegalArgumentException("SampleSpec: duplicate element: " + id);
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

  public List<SampleValueSet> getMatchingValueSets(
      ClientMethodType methodForm, SampleType sampleType) {
    return getMatchingValueSets(CallingForm.Generic, sampleType);
  }

  /**
   * Returns the SampleValueSets that were specified for this methodForm and sampleType.
   *
   * @param methodForm The calling form for which value sets are requestd
   * @param sampleType The sample type for which value sets are requested
   * @return A set of SampleValueSets for methodForm andSampleType
   */
  public List<SampleValueSet> getMatchingValueSets(CallingForm methodForm, SampleType sampleType) {
    String methodFormString = methodForm.toString();

    // Get the value set expressions configured for this calling form.
    List<String> valueSetExpressions =
        getConfigFor(sampleType)
            .stream()
            .filter(
                sampleConfig ->
                    sampleConfig
                        .getCallingFormsList()
                        .stream()
                        .anyMatch(expression -> expressionMatchesId(expression, methodFormString)))
            .flatMap(sampleConfig -> sampleConfig.getValueSetsList().stream())
            .collect(Collectors.toList());

    // Return the value sets corresponding to the selected value set expressions.
    return valueSets
        .stream()
        .filter(
            valueSet ->
                valueSetExpressions
                    .stream()
                    .anyMatch(expression -> expressionMatchesId(expression, valueSet.getId())))
        .collect(ImmutableList.toImmutableList());
  }

  /** Returns the single SampleTypeConfiguration for the specified sampleType. */
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
