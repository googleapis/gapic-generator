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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;

/** This ViewModel defines the view model structure of field. */
@AutoValue
public abstract class StaticMemberView implements Comparable<StaticMemberView> {
  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String name();

  // The type of this object; most likely a String.
  public abstract String typeName();

  // The name of the function to set this variable.
  public abstract String fieldSetFunction();

  // The name of the function to get this variable.
  public abstract String fieldGetFunction();

  public static Builder newBuilder() {
    return new AutoValue_StaticMemberView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder typeName(String val);

    public abstract Builder name(String val);

    public abstract Builder fieldSetFunction(String val);

    public abstract Builder fieldGetFunction(String val);

    public abstract StaticMemberView build();
  }

  @Override
  public int compareTo(StaticMemberView o) {
    return this.name().compareTo(o.name());
  }
}
