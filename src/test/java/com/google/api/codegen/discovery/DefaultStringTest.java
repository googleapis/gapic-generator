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
import java.util.Map;
import org.junit.Test;

public class DefaultStringTest {
  @Test
  public void testOf() {
    String def =
        DefaultString.getNonTrivialPlaceholder("[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?", "my-%s");
    String sample = DefaultString.getSample("compute", "zone", "[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?");
    Truth.assertThat(def).isEqualTo("");
    Truth.assertThat(sample).isEqualTo("us-central1-f");

    def = DefaultString.getNonTrivialPlaceholder("^projects/[^/]*$", "my-%s");
    sample = DefaultString.getSample("pubsub", "project", "^projects/[^/]*$");
    Truth.assertThat(def).isEqualTo("projects/my-project");
    Truth.assertThat(sample).isEqualTo("");

    def = DefaultString.getNonTrivialPlaceholder("bar", "my-%s");
    sample = DefaultString.getSample("foo", "bar", null);
    Truth.assertThat(def).isEqualTo("");
    Truth.assertThat(sample).isEqualTo("");
  }

  @Test
  public void testInvalidPattern() {
    String[] invalid =
        new String[] {
          null,
          "abc",
          "(?:(?:[-a-z0-9]{1,63}\\.)*(?:[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?):)?(?:[0-9]{1,19}|(?:[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?))",
          "[a-z](?:[-a-z0-9]{0,61}[a-z0-9])?",
          "^projects/[^/]*/topics$"
        };

    for (String s : invalid) {
      Truth.assertThat(DefaultString.forPattern(s, "my-%s")).isNull();
    }
  }

  @Test
  public void testDefault() {
    ImmutableMap<String, String> tests =
        ImmutableMap.<String, String>builder()
            .put("^billingAccounts/[^/]*$", "billingAccounts/my-billing-account")
            .put("^projects/[^/]*/topics/[^/]*$", "projects/my-project/topics/my-topic")
            .put(
                "^projects/[^/]*/regions/[^/]*/operations/[^/]*$",
                "projects/my-project/regions/my-region/operations/my-operation")
            .build();

    for (Map.Entry<String, String> entry : tests.entrySet()) {
      Truth.assertThat(DefaultString.forPattern(entry.getKey(), "my-%s"))
          .isEqualTo(entry.getValue());
    }
  }
}
