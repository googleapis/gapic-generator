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
package com.google.api.codegen.viewmodel;

import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.config.SampleSpec.SampleType;
import java.util.HashMap;
import java.util.Set;

/**
 * Implements a view model for the value sets specified in the configuration as relevant to a
 * specific calling form.
 */
public class SampleValueSetsModel {

  private HashMap<SampleType, Set<SampleValueSet>> valueSetsBySampleType;

  /**
   * Initializes SampleValueSetsModel from the given sampleSpec (obtained via the config) and
   * callingForm (determined when constructing the method's view model).
   *
   * @param sampleSpec the sample specification from the user-provided config
   * @param callingForm the calling form for which this SampleValueSetsModel will apply. This
   *     selects which parts of sampleSpec get used.
   */
  public SampleValueSetsModel(SampleSpec sampleSpec, ClientMethodType callingForm) {
    valueSetsBySampleType = new HashMap<>();
    for (SampleType sampleType : SampleType.values()) {
      valueSetsBySampleType.put(sampleType, sampleSpec.valueSetsMatching(callingForm, sampleType));
    }
  }

  /**
   * Returns all the SampleValueSets that apply to the given sampleType.
   *
   * @param sampleType the type of sample for which SampleValueSets are requested
   * @return all the ValueSets for which separate samples of the given type should be generated
   */
  public Set<SampleValueSet> forSampleType(SampleType sampleType) {
    return valueSetsBySampleType.get(sampleType);
  }
}
