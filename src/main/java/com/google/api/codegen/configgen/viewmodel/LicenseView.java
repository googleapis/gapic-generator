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
package com.google.api.codegen.configgen.viewmodel;

import com.google.auto.value.AutoValue;

/** Represents the configuration for the license header to put on generated files. */
@AutoValue
public abstract class LicenseView {
  /** The file containing the copyright line(s). */
  public abstract String copyrightFile();

  /** The file containing the raw license header without any copyright line(s). */
  public abstract String licenseFile();

  public static Builder newBuilder() {
    return new AutoValue_LicenseView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder copyrightFile(String val);

    public abstract Builder licenseFile(String val);

    public abstract LicenseView build();
  }
}
