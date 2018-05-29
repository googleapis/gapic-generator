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
package com.google.api.codegen;

import com.google.api.codegen.util.Inflector;
import com.google.common.truth.Truth;
import org.junit.Test;

public class InflectorTest {

  @Test
  public void testSingularize() {
    Truth.assertThat(Inflector.singularize("blesses")).isEqualTo("bless");
    Truth.assertThat(Inflector.singularize("cares")).isEqualTo("care");
    Truth.assertThat(Inflector.singularize("cars")).isEqualTo("car");
    Truth.assertThat(Inflector.singularize("fishes")).isEqualTo("fish");
    Truth.assertThat(Inflector.singularize("fuzzes")).isEqualTo("fuzz");
    Truth.assertThat(Inflector.singularize("halves")).isEqualTo("half");
    Truth.assertThat(Inflector.singularize("licenses")).isEqualTo("license");
    Truth.assertThat(Inflector.singularize("scarves")).isEqualTo("scarf");
    Truth.assertThat(Inflector.singularize("sneezes")).isEqualTo("sneeze");
  }
}
