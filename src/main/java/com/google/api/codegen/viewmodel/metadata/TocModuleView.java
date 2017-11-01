/* Copyright 2017 Google LLC
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

import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class TocModuleView implements ModuleView {
  public abstract String moduleName();

  public abstract String fullName();

  public abstract List<TocContentView> contents();

  public static Builder newBuilder() {
    return new AutoValue_TocModuleView.Builder();
  }

  public String type() {
    return TocModuleView.class.getSimpleName();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder moduleName(String val);

    public abstract Builder fullName(String val);

    public abstract Builder contents(List<TocContentView> val);

    public abstract TocModuleView build();
  }
}
