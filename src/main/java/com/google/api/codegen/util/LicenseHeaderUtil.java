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

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.LicenseHeaderProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.annotation.Nullable;

public class LicenseHeaderUtil {
  public static final String DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  public static final String DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";

  private final LicenseHeaderProto licenseHeader;
  private final DiagCollector diagCollector;

  private LicenseHeaderUtil(
      DiagCollector diagCollector, @Nullable LicenseHeaderProto licenseHeader) {
    this.diagCollector = diagCollector;
    this.licenseHeader = licenseHeader;
  }

  public static LicenseHeaderUtil create(
      @Nullable ConfigProto configProto,
      @Nullable LanguageSettingsProto settings,
      @Nullable DiagCollector diagCollector) {
    Preconditions.checkNotNull(diagCollector);
    if (configProto != null) {
      Preconditions.checkNotNull(
          settings, "If configProto is non-null, then settings must also be non-null");
    }

    LicenseHeaderProto licenseHeader = null;
    if (configProto != null) {
      licenseHeader =
          configProto
              .getLicenseHeader()
              .toBuilder()
              .mergeFrom(settings.getLicenseHeaderOverride())
              .build();
    }
    return new LicenseHeaderUtil(diagCollector, licenseHeader);
  }

  public ImmutableList<String> loadLicenseLines() {
    String licenseFile;
    if (licenseHeader == null || Strings.isNullOrEmpty(licenseHeader.getLicenseFile())) {
      licenseFile = DEFAULT_LICENSE_FILE;
    } else {
      licenseFile = licenseHeader.getLicenseFile();
    }
    return getResourceLines(licenseFile);
  }

  public ImmutableList<String> loadCopyrightLines() {
    return getResourceLines(DEFAULT_COPYRIGHT_FILE);
  }

  private ImmutableList<String> getResourceLines(String resourceFileName) {
    try {
      InputStream fileStream = ConfigProto.class.getResourceAsStream(resourceFileName);
      if (fileStream == null) {
        throw new FileNotFoundException(resourceFileName);
      }
      InputStreamReader fileReader = new InputStreamReader(fileStream, Charsets.UTF_8);
      return ImmutableList.copyOf(CharStreams.readLines(fileReader));
    } catch (IOException e) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      throw new RuntimeException(e);
    }
  }

  @VisibleForTesting
  public DiagCollector getDiagCollector() {
    return diagCollector;
  }
}
