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
package com.google.api.codegen;

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.php.PhpGapicCodePathMapper;
import com.google.api.gax.grpc.ApiCallable;
import com.google.common.collect.ImmutableMap;
import com.google.common.truth.Truth;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * Tests for {@link ApiCallable}.
 */
@RunWith(JUnit4.class)
public class PhpGapicCodePathMapperTest {

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Test
  public void getOutputPathTest() {
    PhpGapicCodePathMapper pathMapper =
        PhpGapicCodePathMapper.newBuilder().setPrefix("prefix").setSuffix("suffix").build();

    ApiConfig configWithGoogleCloud =
        ApiConfig.createDummyApiConfig(
            ImmutableMap.<String, InterfaceConfig>builder().build(),
            "Google\\Cloud\\Sample\\Package\\V1");
    Truth.assertThat(pathMapper.getOutputPath(null, configWithGoogleCloud))
        .isEqualTo("prefix/Sample/Package/V1/suffix");

    ApiConfig configWithGoogleNonCloud =
        ApiConfig.createDummyApiConfig(
            ImmutableMap.<String, InterfaceConfig>builder().build(),
            "Google\\NonCloud\\Sample\\Package\\V1");
    Truth.assertThat(pathMapper.getOutputPath(null, configWithGoogleNonCloud))
        .isEqualTo("prefix/NonCloud/Sample/Package/V1/suffix");

    ApiConfig configWithAlphabet =
        ApiConfig.createDummyApiConfig(
            ImmutableMap.<String, InterfaceConfig>builder().build(),
            "Alphabet\\Google\\Cloud\\Sample\\Package\\V1");
    Truth.assertThat(pathMapper.getOutputPath(null, configWithAlphabet))
        .isEqualTo("prefix/Alphabet/Google/Cloud/Sample/Package/V1/suffix");
  }
}
