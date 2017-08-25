/* Copyright 2017 Google Inc
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
package com.google.api.codegen.advising;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.LicenseHeaderProto;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class LicenseHeaderRule implements AdviserRule {
  @Override
  public String getName() {
    return "license-header";
  }

  @Override
  public List<String> collectAdvice(Model model, ConfigProto configProto) {
    ImmutableList.Builder<String> messages = ImmutableList.builder();
    LicenseHeaderProto licenseHeaderProto = configProto.getLicenseHeader();

    if (licenseHeaderProto.equals(LicenseHeaderProto.getDefaultInstance())) {
      messages.add(
          String.format(
              "Missing license_header.%n%n"
                  + "license_header:%n"
                  + "  copyright_file: copyright-google.txt%n"
                  + "  license_file: license-header-apache-2.0.txt%n%n"));
      return messages.build();
    }

    if (licenseHeaderProto.getCopyrightFile().isEmpty()) {
      messages.add(
          String.format(
              "Missing copyright_file in license_header.%n%n"
                  + "license_header:%n"
                  + "  # ...%n"
                  + "  copyright_file: copyright-google.txt%n%n"));
    }

    if (licenseHeaderProto.getLicenseFile().isEmpty()) {
      messages.add(
          String.format(
              "Missing license_file in license_header.%n%n"
                  + "license_header:%n"
                  + "  # ...%n"
                  + "  license_file: license-header-apache-2.0.txt%n%n"));
    }

    return messages.build();
  }
}
