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
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.CharStreams;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class LicenseHeaderUtil {
  @VisibleForTesting static final String DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  static final String DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";

  public LicenseHeaderUtil() {}

  public ImmutableList<String> loadLicenseLines() throws IOException {
    return getResourceLines(DEFAULT_LICENSE_FILE);
  }

  public ImmutableList<String> loadCopyrightLines() throws IOException {
    return getResourceLines(DEFAULT_COPYRIGHT_FILE);
  }

  private ImmutableList<String> getResourceLines(String resourceFileName) throws IOException {
    InputStream fileStream = ConfigProto.class.getResourceAsStream(resourceFileName);
    if (fileStream == null) {
      throw new FileNotFoundException(resourceFileName);
    }
    InputStreamReader fileReader = new InputStreamReader(fileStream, Charsets.UTF_8);
    return ImmutableList.copyOf(CharStreams.readLines(fileReader));
  }
}
