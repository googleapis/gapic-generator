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
package com.google.api.codegen.discovery.viewmodel;

import com.google.api.codegen.discovery.config.AuthType;
import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class SampleAuthView {

  public abstract AuthType type();

  public abstract String instructionsUrl();

  public abstract List<String> scopes();

  public abstract boolean isScopesSingular();

  @Nullable
  public abstract String authFuncName();

  @Nullable
  public abstract String authVarName();

  /**
   * Returns a list of fully-qualified scope constants.
   *
   * <p>Go specific. For example: [logging.ReadScope, logging.WriteScope]
   */
  @Nullable
  public abstract List<String> scopeConsts();

  public static Builder newBuilder() {
    return new AutoValue_SampleAuthView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder type(AuthType val);

    public abstract Builder instructionsUrl(String val);

    public abstract Builder scopes(List<String> val);

    public abstract Builder isScopesSingular(boolean val);

    public abstract Builder authFuncName(String val);

    public abstract Builder authVarName(String val);

    public abstract Builder scopeConsts(List<String> val);

    public abstract SampleAuthView build();
  }
}
