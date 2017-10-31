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
public abstract class ParseResourceFunctionView {
  public abstract String entityName();

  public abstract String name();

  public abstract String pathTemplateName();

  public abstract String pathTemplateGetterName();

  public abstract String entityNameParamName();

  public abstract String outputResourceId();

  public static Builder newBuilder() {
    return new AutoValue_ParseResourceFunctionView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder entityName(String val);

    public abstract Builder name(String val);

    public abstract Builder pathTemplateName(String val);

    public abstract Builder pathTemplateGetterName(String val);

    public abstract Builder entityNameParamName(String val);

    public abstract Builder outputResourceId(String val);

    public abstract ParseResourceFunctionView build();
  }
}
