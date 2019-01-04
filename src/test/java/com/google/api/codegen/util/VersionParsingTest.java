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
package com.google.api.codegen.util;

import static com.google.common.truth.Truth.assertThat;

import com.google.api.codegen.transformer.go.GoGapicSurfaceTransformer;
import org.junit.Test;

public class VersionParsingTest {
  private static GoGapicSurfaceTransformer transformer = new GoGapicSurfaceTransformer(null);

  @Test
  public void testInferredBetaParsing() {
    assertThat(transformer.isInferredBetaVersion("cloud.google.com/go/vision/apiv1p1beta1"))
        .isTrue();
    assertThat(
            transformer.isInferredBetaVersion(
                "cloud.google.com/go/cloud/websecurityscanner/apiv1alpha"))
        .isFalse();
    assertThat(transformer.isInferredBetaVersion("cloud.google.com/go/datastore/apiv1")).isFalse();
    assertThat(transformer.isInferredBetaVersion("cloud.google.com/go/devtools/build/apiv1"))
        .isFalse();
    assertThat(transformer.isInferredBetaVersion("cloud.google.com/go/vision/apiv1p3beta1"))
        .isTrue();
    assertThat(transformer.isInferredBetaVersion("cloud.google.com/go/cloudtasks/apiv2beta3"))
        .isTrue();
  }
}
