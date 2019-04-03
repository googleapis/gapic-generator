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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;

public class LicenseHeaderUtilTest {

  private static LicenseHeaderUtil defaultHeaderUtil;

  @BeforeClass
  public static void startUp() {
    defaultHeaderUtil = new LicenseHeaderUtil();
  }

  @Test
  public void loadDefaultLicenseLines() throws IOException {
    String actualLicenseFilePath =
        String.format("src/main/resources/com/google/api/codegen/%s", DEFAULT_LICENSE_FILE);
    BufferedReader actualLicenseReader = new BufferedReader(new FileReader(actualLicenseFilePath));
    String firstLicenseLine = actualLicenseReader.readLine();

    List<String> defaultLicenseLines = defaultHeaderUtil.loadLicenseLines();

    assertThat(firstLicenseLine).isEqualTo(defaultLicenseLines.get(0));
  }

  @Test
  public void loadDefaultCopyrightLines() throws IOException {
    String actualCopyrightFilePath =
        String.format("src/main/resources/com/google/api/codegen/%s", DEFAULT_COPYRIGHT_FILE);
    BufferedReader actualCopyrightReader =
        new BufferedReader(new FileReader(actualCopyrightFilePath));
    String firstCopyrightLine = actualCopyrightReader.readLine();

    List<String> defaultCopyrightLines = defaultHeaderUtil.loadCopyrightLines();

    assertThat(firstCopyrightLine).isEqualTo(defaultCopyrightLines.get(0));
  }
}
