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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class ListMethodDetailView {
  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract String resourceTypeName();

  public abstract String resourceFieldName();

  public abstract String iterateMethodName();

  public abstract List<String> resourcesFieldGetFunction();

  public static Builder newBuilder() {
    return new AutoValue_ListMethodDetailView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder requestTypeName(String name);

    public abstract Builder responseTypeName(String name);

    public abstract Builder resourceTypeName(String name);

    public abstract Builder resourceFieldName(String name);

    public abstract Builder iterateMethodName(String name);

    public abstract Builder resourcesFieldGetFunction(List<String> name);

    public abstract ListMethodDetailView build();
  }
}
