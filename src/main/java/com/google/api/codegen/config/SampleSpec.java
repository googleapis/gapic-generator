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
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Class SampleSpec stores the sample specification for a given method, and provides methods to
 * easily access them by calling form and sample type.
 */
public class SampleSpec {

  /** A reference to the Sample Configuration from which the other fields are derived. */
  private final SampleConfiguration sampleConfiguration;

  /** All the SampleValueSets defined for this method, indexed by their IDs. */
  private HashMap<String, SampleValueSet> valueSets;

  /** The various types of supported samples. */
  public enum SampleType {
    INCODE,
    STANDALONE,
    EXPLORER,
  }

  public SampleSpec(MethodConfigProto methodConfigProto) {
    this.sampleConfiguration = methodConfigProto.getSamples();
    storeValueSets(methodConfigProto.getSampleValueSetsList(), methodConfigProto.getName());
  }

  /**
   * Returns true if id is a match for the given expression. This is the function used to determine
   * whether calling forms and value sets match expressions referencing them by ID.
   *
   * <p>CAUTION: This is a stub at the moment, always returning true.
   */
  public static boolean expressionMatchesId(String expression, String id) {
    // TODO(vchudnov-g): Implement more sophisticated matching as per design.
    return id.matches(expression);
  }

  /** Returns the SampleValueSets that were specified for this methodForm and sampleType. */
  public Set<SampleValueSet> valueSetsMatching(ClientMethodType methodForm, SampleType sampleType) {
    Set<SampleValueSet> matchingValueSets = new HashSet<>();
    List<SampleTypeConfiguration> sampleConfigList = getConfigFor(sampleType);
    String methodFormString = methodForm.toString();

    for (SampleTypeConfiguration sampleConfig : sampleConfigList) {

      // Determine whether sampleConfig applies to methodForm.
      boolean configMatchesForm = false;
      for (String callingFormExpression : sampleConfig.getCallingFormsList()) {
        if (expressionMatchesId(callingFormExpression, methodFormString)) {
          configMatchesForm = true;
          break;
        }
      }
      if (!configMatchesForm) {
        continue;
      }

      // Add the value sets referenced in this sampleConfig.
      for (String valueSetExpression : sampleConfig.getValueSetsList()) {
        Iterable<String> valueSetNames = getValueSetNamesMatchingExpression(valueSetExpression);
        for (String name : valueSetNames) {
          matchingValueSets.add(valueSets.get(name));
        }
      }
    }

    return matchingValueSets;
  }

  /** Returns the IDs of the ValueSets that match valueSetExpression. */
  private Iterable<String> getValueSetNamesMatchingExpression(String valueSetExpression) {
    Predicate<String> expressionMatch =
        new Predicate<String>() {
          @Override
          public boolean apply(@Nullable String id) {
            return expressionMatchesId(valueSetExpression, id);
          }
        };

    return Iterables.filter(valueSets.keySet(), expressionMatch);
  }

  /** Returns the single SampleTypeConfiguration for the specified sampleType. */
  private List<SampleTypeConfiguration> getConfigFor(SampleType sampleType) {
    switch (sampleType) {
      case STANDALONE:
        return sampleConfiguration.getStandaloneList();
      case INCODE:
        return sampleConfiguration.getInCodeList();
      case EXPLORER:
        return sampleConfiguration.getApiExplorerList();
    }
    return null;
  }

  /** Populates the map valueSets so as to be able to access each SampleValueSet by its ID. */
  private void storeValueSets(List<SampleValueSet> sampleValueSets, String methodName) {
    valueSets = new HashMap<>();
    if (sampleConfiguration != null && sampleValueSets != null) {
      for (SampleValueSet set : sampleValueSets) {
        String id = set.getId();
        SampleValueSet previous = valueSets.put(id, set);
        if (previous != null) {
          throw new IllegalArgumentException(
              String.format(
                  "value set \"%s\" defined multiple times in method \"%s\"", id, methodName));
        }
      }
    }
  }
}
