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
package com.google.api.codegen.config;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * VersionBound represents bounds on a version for a dependency specified in the
 * PackageMetadataConfig.
 */
@AutoValue
public abstract class VersionBound {
  public abstract String lower();

  @Nullable
  public abstract String upper();

  public static VersionBound create(String lower, String upper) {
    return new AutoValue_VersionBound(lower, upper);
  }
}
