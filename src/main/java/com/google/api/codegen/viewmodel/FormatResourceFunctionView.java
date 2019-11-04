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
import java.util.List;

@AutoValue
public abstract class FormatResourceFunctionView {
  public abstract String entityName();

  public abstract String name();

  public abstract String resourceName();

  public abstract List<ResourceIdParamView> resourceIdParams();

  public abstract String pathTemplateName();

  public abstract String pathTemplateGetterName();

  public abstract String pattern();

  /** True iff this resource name will cease to exist in the proto annotation world. */
  public abstract boolean isResourceNameDeprecated();

  public static Builder newBuilder() {
    return new AutoValue_FormatResourceFunctionView.Builder().isResourceNameDeprecated(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder entityName(String val);

    public abstract Builder name(String val);

    public abstract Builder resourceName(String val);

    public abstract Builder resourceIdParams(List<ResourceIdParamView> val);

    public abstract Builder pathTemplateName(String val);

    public abstract Builder pathTemplateGetterName(String val);

    public abstract Builder pattern(String val);

    public abstract Builder isResourceNameDeprecated(boolean val);

    public abstract FormatResourceFunctionView build();
  }
}
