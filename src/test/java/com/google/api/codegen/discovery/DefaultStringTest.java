/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.discovery;

import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Test;

import java.util.Map;

public class DefaultStringTest {
  @Test
  public void testInvalidString() {
    String[] invalid =
        new String[] {
          "abc",
          "(?:(?:[-a-z0-9]{1,63}\\.)*(?:[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?):)?(?:[0-9]{1,19}|(?:[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?))",
          "[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?",
          "^projects/[^/]*/topics$"
        };

    for (String s : invalid) {
      Truth.assertThat(DefaultString.of(s)).isNull();
    }
  }

  @Test
  public void testDefault() {
    ImmutableMap<String, String> tests =
        ImmutableMap.<String, String>builder()
            .put("^projects/[^/]*$", "projects/MY-PROJECT")
            .put("^projects/[^/]*/topics/[^/]*$", "projects/MY-PROJECT/topics/MY-TOPIC")
            .put(
                "^projects/[^/]*/regions/[^/]*/operations/[^/]*$",
                "projects/MY-PROJECT/regions/MY-REGION/operations/MY-OPERATION")
            .build();

    for (Map.Entry<String, String> entry : tests.entrySet()) {
      Truth.assertThat(DefaultString.of(entry.getKey())).isEqualTo(entry.getValue());
    }
  }
}
