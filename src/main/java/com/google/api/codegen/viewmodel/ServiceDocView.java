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
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class ServiceDocView {
  public abstract List<String> lines();

  @Nullable
  public abstract ApiMethodView exampleApiMethod();

  public abstract String apiVarName();

  public abstract String apiClassName();

  public abstract String settingsVarName();

  public abstract String settingsClassName();

  public abstract boolean hasDefaultInstance();

  public static Builder newBuilder() {
    return new AutoValue_ServiceDocView.Builder();
  }

  public String firstLine() {
    if (lines().size() > 0) {
      return lines().get(0);
    }
    return "";
  }

  public List<String> remainingLines() {
    if (lines().size() > 0) {
      return lines().subList(1, lines().size());
    }
    return Collections.<String>emptyList();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder lines(List<String> val);

    public abstract Builder exampleApiMethod(ApiMethodView val);

    public abstract Builder apiVarName(String val);

    public abstract Builder apiClassName(String val);

    public abstract Builder settingsVarName(String val);

    public abstract Builder settingsClassName(String val);

    public abstract Builder hasDefaultInstance(boolean hasDefaultInstance);

    public abstract ServiceDocView build();
  }
}
