/* Copyright 2017 Google LLC
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

/** This ViewModel defines the view model structure of ResourceName. */
@AutoValue
public abstract class StaticLangApiResourceNameView
    implements Comparable<StaticLangApiResourceNameView> {

  // The possibly-transformed ID of the schema from the Discovery Doc
  public abstract String name();

  // The type name for this Schema when rendered as a field in its parent Schema, e.g. "List<Operation>".
  public abstract String typeName();

  // The template for the path, e.g. "projects/{projects}/topic/{topic}"
  public abstract String pathTemplate();

  // The list of path parameter views.
  public abstract List<StaticLangMemberView> pathParams();

  public static Builder newBuilder() {
    return new AutoValue_StaticLangApiResourceNameView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder typeName(String val);

    public abstract Builder pathTemplate(String val);

    public abstract Builder pathParams(List<StaticLangMemberView> val);

    public abstract StaticLangApiResourceNameView build();
  }

  @Override
  public int compareTo(StaticLangApiResourceNameView o) {
    return this.name().compareTo(o.name());
  }
}
