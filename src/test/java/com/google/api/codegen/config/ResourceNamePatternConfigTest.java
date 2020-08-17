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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class ResourceNamePatternConfigTest {

  @Test
  public void testIsFixedPattern() {
    assertThat(new ResourceNamePatternConfig("_deleted-topic_").isFixedPattern()).isEqualTo(true);
    assertThat(new ResourceNamePatternConfig("states/{state}/cities/{city}").isFixedPattern())
        .isEqualTo(false);
  }

  @Test
  public void testGetCreateMethodName() {
    assertThat(new ResourceNamePatternConfig("_deleted-topic_").getCreateMethodName())
        .isEqualTo("ofDeletedTopicName");
    assertThat(new ResourceNamePatternConfig("states/{state}/cities/{city}").getCreateMethodName())
        .isEqualTo("ofStateCityName");
  }

  @Test
  public void testGetBindingVariables() {
    assertThat(new ResourceNamePatternConfig("_deleted-topic_").getBindingVariables()).isEmpty();
    assertThat(new ResourceNamePatternConfig("states/{state}/cities/{city}").getBindingVariables())
        .containsExactly("state", "city");
  }

  @Test
  public void testGetBindingVariablesWithComplexResourceIds() {
    assertThat(
            new ResourceNamePatternConfig("states/{state}/animals/{animal_1}~{animal_2}")
                .getBindingVariables())
        .containsExactly("state", "animal_1", "animal_2");
    assertThat(
            new ResourceNamePatternConfig("states/{state}/animals/{foo}.{bar}~{car}-{cdr}_{cadr}")
                .getBindingVariables())
        .containsExactly("state", "foo", "bar", "car", "cdr", "cadr");
    assertThat(
            new ResourceNamePatternConfig("states/{state}/animals/{foo}.{bar}/prizes/{prize}")
                .getBindingVariables())
        .containsExactly("state", "foo", "bar", "prize");
  }

  @Test
  public void testGetPatternId() {
    ResourceNamePatternConfig pattern;
    pattern = new ResourceNamePatternConfig("_deleted-topic_");
    assertThat(pattern.getPatternId()).isEqualTo("deleted_topic");
    pattern = new ResourceNamePatternConfig("states/{state}/cities/{city}");
    assertThat(pattern.getPatternId()).isEqualTo("state_city");
    pattern = new ResourceNamePatternConfig("states/{state}/cities/{city}/mayor");
    assertThat(pattern.getPatternId()).isEqualTo("state_city_mayor");
    pattern = new ResourceNamePatternConfig("states/{state}/cities/{city}/mascotAnimal");
    assertThat(pattern.getPatternId()).isEqualTo("state_city_mascot_animal");
    pattern = new ResourceNamePatternConfig("states/{state}/mascotAnimals/{mascot_animal}");
    assertThat(pattern.getPatternId()).isEqualTo("state_mascot_animal");

    pattern = new ResourceNamePatternConfig("states/{state}/animals/{animal_id=**}");
    assertThat(pattern.getPatternId()).isEqualTo("state_animal_id");
  }

  @Test
  public void testGetPatternIdWithComplexResourceIds() {
    ResourceNamePatternConfig pattern =
        new ResourceNamePatternConfig("states/{state}/animals/{animal_1}~{animal_2}");
    assertThat(pattern.getPatternId()).isEqualTo("state_animal_1_animal_2");
    pattern =
        new ResourceNamePatternConfig("states/{state}/animals/{foo}.{bar}~{car}-{cdr}_{cadr}");
    assertThat(pattern.getPatternId()).isEqualTo("state_foo_bar_car_cdr_cadr");
  }
}
