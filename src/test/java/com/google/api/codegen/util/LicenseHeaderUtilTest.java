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

import static com.google.api.codegen.util.LicenseHeaderUtil.DEFAULT_COPYRIGHT_FILE;
import static com.google.api.codegen.util.LicenseHeaderUtil.DEFAULT_LICENSE_FILE;
import static com.google.common.truth.Truth.assertThat;
import static org.junit.Assert.fail;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.LicenseHeaderProto;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class LicenseHeaderUtilTest {

  // A dummy value for package name override.
  private static final String IMAGINARY_FILE = "imaginary_file.txt";
  // Some target language; the value isn't important.
  private static final String LANGUAGE = "python";

  private static LicenseHeaderUtil defaultHeaderUtil;
  private static LicenseHeaderUtil explicitHeaderUtil;
  private static LicenseHeaderUtil langOverrideHeaderUtil;

  @BeforeClass
  public static void startUp() {
    defaultHeaderUtil = LicenseHeaderUtil.create(null, null, new BoundedDiagCollector());

    ConfigProto configProto =
        ConfigProto.newBuilder()
            .setLicenseHeader(
                LicenseHeaderProto.newBuilder()
                    .setCopyrightFile(DEFAULT_COPYRIGHT_FILE)
                    .setLicenseFile(DEFAULT_LICENSE_FILE))
            .build();
    explicitHeaderUtil =
        LicenseHeaderUtil.create(
            configProto, LanguageSettingsProto.getDefaultInstance(), new BoundedDiagCollector());

    ConfigProto langOverrideConfigProto =
        configProto
            .toBuilder()
            .putLanguageSettings(
                LANGUAGE,
                LanguageSettingsProto.newBuilder()
                    .setLicenseHeaderOverride(
                        LicenseHeaderProto.newBuilder().setLicenseFile(IMAGINARY_FILE).build())
                    .build())
            .build();
    langOverrideHeaderUtil =
        LicenseHeaderUtil.create(
            langOverrideConfigProto,
            langOverrideConfigProto.getLanguageSettingsMap().get(LANGUAGE),
            new BoundedDiagCollector());
  }

  @Test
  public void loadDefaultLicenseLines() throws IOException {
    String actualLicenseFilePath =
        String.format("src/main/resources/com/google/api/codegen/%s", DEFAULT_LICENSE_FILE);
    BufferedReader actualLicenseReader = new BufferedReader(new FileReader(actualLicenseFilePath));
    String firstLicenseLine = actualLicenseReader.readLine();

    List<String> defaultLicenseLines = defaultHeaderUtil.loadLicenseLines();
    List<String> explicitLicenseLines = explicitHeaderUtil.loadLicenseLines();

    assertThat(explicitLicenseLines).isEqualTo(defaultLicenseLines);
    assertThat(firstLicenseLine).isEqualTo(defaultLicenseLines.get(0));
    assertThat(defaultHeaderUtil.getDiagCollector().getErrorCount()).isEqualTo(0);
    assertThat(explicitHeaderUtil.getDiagCollector().getErrorCount()).isEqualTo(0);
  }

  @Test
  public void loadDefaultCopyrightLines() throws IOException {
    String actualCopyrightFilePath =
        String.format("src/main/resources/com/google/api/codegen/%s", DEFAULT_COPYRIGHT_FILE);
    BufferedReader actualCopyrightReader =
        new BufferedReader(new FileReader(actualCopyrightFilePath));
    String firstCopyrightLine = actualCopyrightReader.readLine();

    List<String> defaultCopyrightLines = defaultHeaderUtil.loadCopyrightLines();
    List<String> explicitCopyrightLines = explicitHeaderUtil.loadCopyrightLines();

    assertThat(explicitCopyrightLines).isEqualTo(defaultCopyrightLines);
    assertThat(firstCopyrightLine).isEqualTo(defaultCopyrightLines.get(0));
    assertThat(defaultHeaderUtil.getDiagCollector().getErrorCount()).isEqualTo(0);
    assertThat(explicitHeaderUtil.getDiagCollector().getErrorCount()).isEqualTo(0);
  }

  @Test
  public void testLanguageSettingsOverride() {
    try {
      langOverrideHeaderUtil.loadLicenseLines();
    } catch (RuntimeException e) {
      // This is supposed to happen because IMAGINARY_FILE doesn't exist.
      assertThat(langOverrideHeaderUtil.getDiagCollector().getErrorCount()).isGreaterThan(0);
      return;
    }

    fail();
  }
}
