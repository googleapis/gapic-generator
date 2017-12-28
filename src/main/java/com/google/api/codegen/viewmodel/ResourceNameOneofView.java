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
public abstract class ResourceNameOneofView implements ResourceNameView {
  @Override
  public ResourceNameType type() {
    return ResourceNameType.ONEOF;
  }

  @Override
  public abstract String typeName();

  @Override
  public abstract String paramName();

  @Override
  public abstract String propertyName();

  @Override
  public abstract String enumName();

  @Override
  public abstract String docName();

  @Override
  public abstract int index();

  public abstract List<ResourceNameView> children();

  public static Builder newBuilder() {
    return new AutoValue_ResourceNameOneofView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder typeName(String val);

    public abstract Builder paramName(String val);

    public abstract Builder propertyName(String val);

    public abstract Builder enumName(String val);

    public abstract Builder docName(String val);

    public abstract Builder index(int val);

    public abstract Builder children(List<ResourceNameView> val);

    public abstract ResourceNameOneofView build();
  }
}
