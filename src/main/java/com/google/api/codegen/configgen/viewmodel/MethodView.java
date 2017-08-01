/* Copyright 2017 Google Inc
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
package com.google.api.codegen.configgen.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class MethodView {
  public abstract String name();

  @Nullable
  public abstract FlatteningView flattening();

  public boolean hasFlattening() {
    return flattening() != null;
  }

  public abstract List<String> requiredFields();

  public abstract boolean requestObjectMethod();

  @Nullable
  public abstract PageStreamingView pageStreaming();

  public boolean hasPageStreaming() {
    return pageStreaming() != null;
  }

  public abstract String retryCodesName();

  public abstract String retryParamsName();

  public abstract List<FieldNamePatternView> fieldNamePatterns();

  public abstract String timeoutMillis();

  public static Builder newBuilder() {
    return new AutoValue_MethodView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder flattening(FlatteningView val);

    public abstract Builder requiredFields(List<String> val);

    public abstract Builder requestObjectMethod(boolean val);

    public abstract Builder pageStreaming(PageStreamingView val);

    public abstract Builder retryCodesName(String val);

    public abstract Builder retryParamsName(String val);

    public abstract Builder fieldNamePatterns(List<FieldNamePatternView> val);

    public abstract Builder timeoutMillis(String val);

    public abstract MethodView build();
  }
}
