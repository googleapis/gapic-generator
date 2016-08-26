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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

@AutoValue
public abstract class InitCodeView {
  public abstract List<InitCodeLineView> lines();

  public abstract List<FieldSettingView> fieldSettings();

  /**
   * Used to hold information about the types used in method samples.
   */
  @Nullable
  public abstract List<ImportTypeView> aliasingTypes();

  /**
   * The file name of the client library generated. This is used for import statements in samples.
   */
  public abstract String apiFileName();

  public static Builder newBuilder() {
    return new AutoValue_InitCodeView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder lines(List<InitCodeLineView> val);

    public abstract Builder fieldSettings(List<FieldSettingView> val);

    public abstract Builder aliasingTypes(List<ImportTypeView> aliasingTypesMap);

    public abstract Builder apiFileName(String apiFileName);

    public abstract InitCodeView build();
  }
}
