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

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

@AutoValue
public abstract class SamplePageStreamingView {

  @Nullable
  public abstract String resourceFieldName();

  @Nullable
  public abstract String resourceGetterName();

  public abstract boolean isResourceRepeated();

  public abstract boolean isResourceMap();

  @Nullable
  public abstract String resourceVarName();

  @Nullable
  public abstract String resourceKeyVarName();

  @Nullable
  public abstract String resourceValueVarName();

  @Nullable
  public abstract String resourceElementTypeName();

  @Nullable
  public abstract String pageVarName();

  @Nullable
  public abstract String handlePageVarName();

  @Nullable
  public abstract Boolean isResourceSetterInRequestBody();

  @Nullable
  public abstract String pageTokenName();

  @Nullable
  public abstract String nextPageTokenName();

  public static Builder newBuilder() {
    return new AutoValue_SamplePageStreamingView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract SamplePageStreamingView build();

    public abstract Builder resourceFieldName(String val);

    public abstract Builder resourceGetterName(String val);

    public abstract Builder isResourceRepeated(boolean val);

    public abstract Builder isResourceMap(boolean val);

    public abstract Builder resourceVarName(String val);

    public abstract Builder resourceKeyVarName(String val);

    public abstract Builder resourceValueVarName(String val);

    public abstract Builder resourceElementTypeName(String val);

    public abstract Builder pageVarName(String val);

    public abstract Builder handlePageVarName(String val);

    public abstract Builder isResourceSetterInRequestBody(Boolean val);

    public abstract Builder pageTokenName(String val);

    public abstract Builder nextPageTokenName(String val);
  }
}
