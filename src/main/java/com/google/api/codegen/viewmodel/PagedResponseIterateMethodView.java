/* Copyright 2016 Google LLC
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

@AutoValue
public abstract class PagedResponseIterateMethodView {

  public abstract String overloadResourceTypeName();

  public abstract String overloadResourceTypeParseFunctionName();

  public abstract String overloadResourceTypeIterateMethodName();

  public abstract String overloadResourceTypeGetValuesMethodName();

  public abstract String iterateMethodName();

  public abstract String getValuesMethodName();

  public static Builder newBuilder() {
    return new AutoValue_PagedResponseIterateMethodView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder overloadResourceTypeName(String val);

    public abstract Builder overloadResourceTypeParseFunctionName(String val);

    public abstract Builder overloadResourceTypeIterateMethodName(String val);

    public abstract Builder overloadResourceTypeGetValuesMethodName(String val);

    public abstract Builder iterateMethodName(String val);

    public abstract Builder getValuesMethodName(String val);

    public abstract PagedResponseIterateMethodView build();
  }
}
