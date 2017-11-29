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
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class ApiMethodDocView {

  public abstract Iterable<String> mainDocLines();

  public abstract List<ParamDocView> paramDocs();

  public abstract List<String> throwsDocLines();

  @Nullable
  public abstract List<String> returnsDocLines();

  @Nullable
  public abstract String returnTypeName();

  @Nullable
  public abstract String pageStreamingResourceTypeName();

  public static Builder newBuilder() {
    return new AutoValue_ApiMethodDocView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder mainDocLines(Iterable<String> lines);

    public abstract Builder paramDocs(List<ParamDocView> docs);

    public abstract Builder throwsDocLines(List<String> lines);

    public abstract Builder returnTypeName(String name);

    public abstract Builder returnsDocLines(List<String> lines);

    public abstract Builder pageStreamingResourceTypeName(String name);

    public abstract ApiMethodDocView build();
  }
}
