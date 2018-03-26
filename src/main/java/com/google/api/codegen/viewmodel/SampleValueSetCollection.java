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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Contains all the value sets specified in the configuration as relevant to a specific calling
 * form, for inclusion in a view model.
 */
public class SampleValueSetCollection {

  private Map<SampleType, Set<SampleValueSetView>> valueSetsBySampleType;

  /**
   * Initializes SampleValueSetCollection from the given sampleSpec (obtained via the config) and
   * callingForm (determined when constructing the method's view model).
   *
   * @param sampleSpec the sample specification from the user-provided config
   * @param callingForm the calling form for which this SampleValueSetCollection will apply. This
   *     selects which parts of sampleSpec get used.
   */
  public SampleValueSetCollection(SampleSpec sampleSpec, ClientMethodType callingForm) {
    valueSetsBySampleType = new HashMap<>();
    for (SampleType sampleType : SampleType.values()) {
      Set<SampleValueSet> configSets = sampleSpec.getMatchingValueSets(callingForm, sampleType);
      Set<SampleValueSetView> viewSets =
          configSets
              .stream()
              .map((SampleValueSet config) -> SampleValueSetView.New(config))
              .collect(Collectors.toSet());
      valueSetsBySampleType.put(sampleType, viewSets);
    }
  }

  /**
   * Returns all the SampleValuesViews that apply to the given sampleType.
   *
   * @param sampleType the type of sample for which sample value set view are requested
   * @return all the SampleValueSetViews for which separate samples of the given type should be
   *     generated
   */
  public Set<SampleValueSetView> forSampleType(SampleType sampleType) {
    return valueSetsBySampleType.get(sampleType);
  }
}
