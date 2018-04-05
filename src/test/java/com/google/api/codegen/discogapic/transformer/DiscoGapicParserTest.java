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
package com.google.api.codegen.discogapic.transformer;

import com.google.common.truth.Truth;
import org.junit.Test;

public class DiscoGapicParserTest {

  @Test
  public void testCanonicalPath() {
    // The test inputs to the getCanonicalPath() are examples from the Compute Discovery Doc API.

    Truth.assertThat(DiscoGapicParser.getCanonicalPath("{project}/backendBuckets/{backendBucket}"))
        .isEqualTo("projects/{project}/backendBuckets/{backendBucket}");
    Truth.assertThat(
            DiscoGapicParser.getCanonicalPath("{project}/global/backendBuckets/{backendBucket}"))
        .isEqualTo("projects/{project}/global/backendBuckets/{backendBucket}");
    Truth.assertThat(
            DiscoGapicParser.getCanonicalPath("{project}/zones/{zone}/disks/{resource}/setLabels"))
        .isEqualTo("projects/{project}/zones/{zone}/disks/{resource}");
    Truth.assertThat(
            DiscoGapicParser.getCanonicalPath("{project}/global/images/{resource}/setLabels"))
        .isEqualTo("projects/{project}/global/images/{resource}");
  }

  @Test
  public void testGetQualifiedResourceIdentifier() {
    Truth.assertThat(
            DiscoGapicParser.getQualifiedResourceIdentifier(
                    "{project}/backendBuckets/{backendBucket}")
                .toUpperCamel())
        .isEqualTo("ProjectBackendBucket");
    Truth.assertThat(
            DiscoGapicParser.getQualifiedResourceIdentifier(
                    "{project}/global/backendBuckets/{backendBucket}")
                .toUpperCamel())
        .isEqualTo("ProjectGlobalBackendBucket");
    Truth.assertThat(
            DiscoGapicParser.getQualifiedResourceIdentifier("{project}/zones/{zone}/disks/{disk}")
                .toUpperCamel())
        .isEqualTo("ProjectZoneDisk");
    Truth.assertThat(
            DiscoGapicParser.getQualifiedResourceIdentifier(
                    "{project}/zones/{zone}/disks/{resource}")
                .toUpperCamel())
        .isEqualTo("ProjectZoneDiskResource");
    Truth.assertThat(
            DiscoGapicParser.getQualifiedResourceIdentifier("{project}/global/images/{resource}")
                .toUpperCamel())
        .isEqualTo("ProjectGlobalImageResource");
  }
}
