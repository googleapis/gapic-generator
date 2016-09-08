/* Copyright 2016 Google Inc
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
package com.google.api.codegen.discovery;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class FieldInfo {

  public abstract String name();

  public abstract TypeInfo type();

  public abstract String description();

  public static Builder newBuilder() {
    return new AutoValue_FieldInfo.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder name(String val);

    public abstract Builder type(TypeInfo val);

    public abstract Builder description(String val);

    public abstract FieldInfo build();
  }
}
