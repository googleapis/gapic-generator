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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class FormattedInitValueView implements InitValueView {
  public abstract String apiVariableName();

  public abstract String apiWrapperName();

  public abstract String fullyQualifiedApiWrapperName();

  public abstract String formatFunctionName();

  public abstract String formatSpec();

  public abstract List<String> formatArgs();

  public String type() {
    return FormattedInitValueView.class.getSimpleName();
  }

  public static Builder newBuilder() {
    return new AutoValue_FormattedInitValueView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder apiVariableName(String val);

    public abstract Builder apiWrapperName(String val);

    public abstract Builder fullyQualifiedApiWrapperName(String val);

    public abstract Builder formatFunctionName(String val);

    public abstract Builder formatSpec(String val);

    public abstract Builder formatArgs(List<String> val);

    public abstract FormattedInitValueView build();
  }
}
