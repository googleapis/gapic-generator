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

import static com.google.common.truth.Truth.assertThat;

import org.junit.Test;

public class RubyPackageMetadataNamerTest {

  private static String testPackageName = "Google::Cloud::UrlLengthenerApi::V1";

  @Test
  public void getMetadataIdentifier() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    assertThat(namer.getMetadataIdentifier()).isEqualTo("google-cloud-url_lengthener_api");
  }

  @Test
  public void getOutputFileName() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    assertThat(namer.getOutputFileName()).isEqualTo("google-cloud-url_lengthener_api.gemspec");
  }

  @Test
  public void getProjectVariable() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    assertThat(namer.getProjectVariable(true)).isEqualTo("URL_LENGTHENER_API_TEST_PROJECT");
    assertThat(namer.getProjectVariable(false)).isEqualTo("URL_LENGTHENER_API_PROJECT");
  }

  @Test
  public void getKeyfileVariable() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    assertThat(namer.getKeyfileVariable(true)).isEqualTo("URL_LENGTHENER_API_TEST_KEYFILE");
    assertThat(namer.getKeyfileVariable(false)).isEqualTo("URL_LENGTHENER_API_KEYFILE");
  }

  @Test
  public void getJsonKeyVariable() {
    RubyPackageMetadataNamer namer = new RubyPackageMetadataNamer(testPackageName);
    assertThat(namer.getJsonKeyVariable(true)).isEqualTo("URL_LENGTHENER_API_TEST_KEYFILE_JSON");
    assertThat(namer.getJsonKeyVariable(false)).isEqualTo("URL_LENGTHENER_API_KEYFILE_JSON");
  }
}
