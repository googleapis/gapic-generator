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

import java.util.List;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class SampleBodyView {

  public abstract String serviceVarName();

  public abstract String serviceTypeName();

  public abstract List<String> resources();

  public abstract String inputVarName();

  public abstract String inputTypeName();

  public abstract boolean hasInputRequest();

  public abstract String inputRequestVarName();

  public abstract String inputRequestTypeName();

  public abstract boolean hasOutput();

  public abstract String outputVarName();

  public abstract String outputTypeName();

  public abstract List<SampleFieldView> fields();

  public abstract boolean isPageStreaming();

  public abstract String resourceGetterName();

  public abstract String resourceTypeName();

  public abstract boolean isResourceMap();

  public static Builder newBuilder() {
    return new AutoValue_SampleBodyView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder serviceVarName(String val);

    public abstract Builder serviceTypeName(String val);

    public abstract Builder resources(List<String> val);

    public abstract Builder inputVarName(String val);

    public abstract Builder inputTypeName(String val);

    public abstract Builder hasInputRequest(boolean val);

    public abstract Builder inputRequestVarName(String val);

    public abstract Builder inputRequestTypeName(String val);

    public abstract Builder hasOutput(boolean val);

    public abstract Builder outputVarName(String val);

    public abstract Builder outputTypeName(String val);

    public abstract Builder fields(List<SampleFieldView> val);

    public abstract Builder isPageStreaming(boolean val);

    public abstract Builder resourceGetterName(String val);

    public abstract Builder resourceTypeName(String val);

    public abstract Builder isResourceMap(boolean val);

    public abstract SampleBodyView build();
  }
}
