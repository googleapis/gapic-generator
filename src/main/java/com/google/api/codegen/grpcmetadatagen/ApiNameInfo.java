/* Copyright 2016 Google LLC
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
package com.google.api.codegen.grpcmetadatagen;

import com.google.auto.value.AutoValue;

/** Represents naming information connected to a particular API. */
@AutoValue
public abstract class ApiNameInfo {
  static AutoValue_ApiNameInfo create(
      String fullName, String shortName, String packageName, String majorVersion, String path) {
    return new AutoValue_ApiNameInfo(fullName, shortName, packageName, majorVersion, path);
  }

  /** The full name of the API, including branding. E.g., "Stackdriver Logging". */
  public abstract String fullName();

  /** A single-word short name of the API. E.g., "logging". */
  public abstract String shortName();

  /** The base name of the client library package. E.g., "google-cloud-logging-v1". */
  public abstract String packageName();

  /** The major version of the API, as used in the package name. E.g., "v1". */
  public abstract String majorVersion();

  /** The path to the API protos in the googleapis repo. */
  public abstract String protoPath();
}
