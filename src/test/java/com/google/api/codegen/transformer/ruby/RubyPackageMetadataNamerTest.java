/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.ruby;

import com.google.common.truth.Truth;
import org.junit.Test;

public class RubyPackageMetadataNamerTest {

  private static String testPackageName = "Google::Cloud::UrlLengthenerApi::V1";

  @Test
  public void getMetadataIdentifier() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    Truth.assertThat(namer.getMetadataIdentifier()).isEqualTo("google-cloud-url_lengthener_api");
  }

  @Test
  public void getOutputFileName() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    Truth.assertThat(namer.getOutputFileName())
        .isEqualTo("google-cloud-url_lengthener_api.gemspec");
  }

  @Test
  public void getSmokeTestProjectVariable() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    Truth.assertThat(namer.getSmokeTestProjectVariable())
        .isEqualTo("URL_LENGTHENER_API_TEST_PROJECT");
  }
}
