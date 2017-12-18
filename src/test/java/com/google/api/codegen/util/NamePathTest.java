/* Copyright 2016 Google LLC
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
package com.google.api.codegen.util;

import com.google.common.truth.Truth;
import org.junit.Test;

public class NamePathTest {

  @Test
  public void testSingleWord() {
    NamePath path = NamePath.dotted("Foo");
    Truth.assertThat(path.toDotted()).isEqualTo("Foo");
    Truth.assertThat(path.toBackslashed()).isEqualTo("Foo");
    Truth.assertThat(path.getHead()).isEqualTo("Foo");
    Truth.assertThat(path.withHead("Bar").toDotted()).isEqualTo("Bar");
  }

  @Test
  public void testDottedPath() {
    NamePath path = NamePath.dotted("com.google.Foo");
    Truth.assertThat(path.toDotted()).isEqualTo("com.google.Foo");
    Truth.assertThat(path.toBackslashed()).isEqualTo("com\\google\\Foo");
    Truth.assertThat(path.getHead()).isEqualTo("Foo");
    Truth.assertThat(path.withHead("Bar").toDotted()).isEqualTo("com.google.Bar");
  }
}
