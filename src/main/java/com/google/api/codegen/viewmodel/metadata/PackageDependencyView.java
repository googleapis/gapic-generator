/* Copyright 2016 Google LLC
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
package com.google.api.codegen.viewmodel.metadata;

import com.google.api.codegen.config.VersionBound;
import com.google.auto.value.AutoValue;

/** Represents a dependency of the package being generated. */
@AutoValue
public abstract class PackageDependencyView implements Comparable<PackageDependencyView> {

  /** The name of the dependency package */
  public abstract String name();

  /** The version bounds on the dependency */
  public abstract VersionBound versionBound();

  public static PackageDependencyView create(String name, VersionBound versionBound) {
    return new AutoValue_PackageDependencyView(name, versionBound);
  }

  @Override
  public int compareTo(PackageDependencyView other) {
    return this.name().compareTo(other.name());
  }
}
