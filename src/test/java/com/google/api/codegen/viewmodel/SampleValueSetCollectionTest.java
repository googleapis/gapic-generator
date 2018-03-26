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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.SampleConfiguration;
import com.google.api.codegen.SampleConfiguration.SampleTypeConfiguration;
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.config.SampleSpec.SampleType;
import java.util.Set;
import org.junit.Test;

public class SampleValueSetCollectionTest {
  @Test
  public void forSampleType() {
    SampleValueSet valueSetAlice =
        SampleValueSet.newBuilder().setId("alice").addParameters("apple").build();
    SampleValueSet valueSetBob =
        SampleValueSet.newBuilder().setId("bob").addParameters("banana").build();
    SampleValueSet valueSetAlison =
        SampleValueSet.newBuilder().setId("alison").addParameters("apricot").build();

    final MethodConfigProto methodConfigProto =
        MethodConfigProto.newBuilder()
            .addSampleValueSets(valueSetAlice)
            .addSampleValueSets(valueSetBob)
            .addSampleValueSets(valueSetAlison)
            .setSamples(
                SampleConfiguration.newBuilder()
                    .addStandalone(
                        SampleTypeConfiguration.newBuilder()
                            .addValueSets("ali.*")
                            .addCallingForms(".*"))
                    .addStandalone(
                        SampleTypeConfiguration.newBuilder()
                            .addValueSets("be.*")
                            .addCallingForms(".*")))
            .build();
    SampleSpec sampleSpec = new SampleSpec(methodConfigProto);
    final SampleValueSetCollection model =
        new SampleValueSetCollection(sampleSpec, ClientMethodType.CallableMethod);
    final Set<SampleValueSetView> matchingValueSets = model.forSampleType(SampleType.STANDALONE);
    assertEquals(2, matchingValueSets.size());

    assertTrue(matchingValueSets.contains(SampleValueSetView.New(valueSetAlice)));
    assertTrue(matchingValueSets.contains(SampleValueSetView.New(valueSetAlison)));
    assertFalse(matchingValueSets.contains(SampleValueSetView.New(valueSetBob)));
  }
}
