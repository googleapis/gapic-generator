/* Copyright 2018 Google LLC
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
import javax.annotation.Nullable;

@AutoValue
public abstract class RestMethodConfigView implements ApiMethodView {
  public abstract String name();

  public abstract String method();

  public abstract String uriTemplate();

  @Nullable
  public abstract String body();

  @Nullable
  public abstract List<String> additionalBindings();

  @Nullable
  public abstract List<RestPlaceholderConfigView> placeholders();

  public abstract boolean hasBody();

  public abstract boolean hasPlaceholders();

  public abstract boolean hasAdditionalBindings();

  public static Builder newBuilder() {
    return new AutoValue_RestMethodConfigView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder method(String val);

    public abstract Builder uriTemplate(String val);

    public abstract Builder body(String val);

    public abstract Builder additionalBindings(List<String> val);

    public abstract Builder placeholders(List<RestPlaceholderConfigView> val);

    public abstract Builder hasBody(boolean val);

    public abstract Builder hasPlaceholders(boolean val);

    public abstract Builder hasAdditionalBindings(boolean val);

    public abstract RestMethodConfigView build();
  }
}
