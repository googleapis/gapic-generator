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

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.SampleConfiguration;
import com.google.api.codegen.SampleConfiguration.SampleTypeConfiguration;
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.viewmodel.CallingForm;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class SampleSpecTest {

  @Test(expected = IllegalArgumentException.class)
  public void storingDuplicateValueSets() {
    final MethodConfigProto methodConfigProto =
        MethodConfigProto.newBuilder()
            .addSampleValueSets(SampleValueSet.newBuilder().setId("alice"))
            .addSampleValueSets(SampleValueSet.newBuilder().setId("bob"))
            .addSampleValueSets(SampleValueSet.newBuilder().setId("alice"))
            .setSamples(
                SampleConfiguration.newBuilder()
                    .addStandalone(
                        SampleTypeConfiguration.newBuilder()
                            .addValueSets("alice")
                            .addCallingForms(".*"))
                    .addStandalone(
                        SampleTypeConfiguration.newBuilder()
                            .addValueSets("bob")
                            .addCallingForms(".*")))
            .build();
    SampleSpec sampleSpec = new SampleSpec(methodConfigProto);
  }

  @Test
  public void valueSetsMatching() {
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
    final List<SampleValueSet> matchingValues =
        sampleSpec
            .getMatchingValueSets(CallingForm.Request, SampleType.STANDALONE)
            .stream()
            .map(vsat -> vsat.values())
            .collect(Collectors.toList());
    assertThat(matchingValues).containsExactly(valueSetAlice, valueSetAlison).inOrder();
  }

  @Test
  public void valueSetsReferencedMultipleTimes() {
    final MethodConfigProto methodConfigProto =
        MethodConfigProto.newBuilder()
            .addSampleValueSets(SampleValueSet.newBuilder().setId("alice"))
            .addSampleValueSets(SampleValueSet.newBuilder().setId("bob"))
            .setSamples(
                SampleConfiguration.newBuilder()
                    .addStandalone(
                        SampleTypeConfiguration.newBuilder()
                            .addValueSets("alice")
                            .addCallingForms(".*"))
                    .addStandalone(
                        SampleTypeConfiguration.newBuilder()
                            .addValueSets("bob")
                            .addCallingForms(".*"))
                    .addStandalone(
                        SampleTypeConfiguration.newBuilder()
                            .addValueSets("alice")
                            .addCallingForms(".*")))
            .build();
    SampleSpec sampleSpec = new SampleSpec(methodConfigProto);
    assertThat(sampleSpec.getMatchingValueSets(CallingForm.Request, SampleType.STANDALONE))
        .hasSize(3);
  }
}
