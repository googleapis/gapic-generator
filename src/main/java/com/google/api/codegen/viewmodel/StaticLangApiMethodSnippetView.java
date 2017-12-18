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

@AutoValue
public abstract class StaticLangApiMethodSnippetView {

  public abstract StaticLangApiMethodView method();

  public abstract String snippetMethodName();

  public abstract String callerResponseTypeName();

  public abstract String apiClassName();

  public abstract String apiVariableName();

  public static Builder newBuilder() {
    return new AutoValue_StaticLangApiMethodSnippetView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder method(StaticLangApiMethodView val);

    public abstract Builder snippetMethodName(String val);

    public abstract Builder callerResponseTypeName(String val);

    public abstract Builder apiClassName(String val);

    public abstract Builder apiVariableName(String val);

    public abstract StaticLangApiMethodSnippetView build();
  }
}
