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
package com.google.api.codegen.viewmodel.testing;

import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class PageStreamingResponseView {

  public abstract String resourcesVarName();

  public abstract String resourceTypeName();

  public abstract List<String> resourcesFieldGetterNames();

  public abstract String resourcesIterateMethod();

  public static Builder newBuilder() {
    return new AutoValue_PageStreamingResponseView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder resourcesVarName(String val);

    public abstract Builder resourceTypeName(String val);

    public abstract Builder resourcesFieldGetterNames(List<String> val);

    public abstract Builder resourcesIterateMethod(String val);

    public abstract PageStreamingResponseView build();
  }
}
