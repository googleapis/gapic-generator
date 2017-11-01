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
package com.google.api.codegen.transformer;

import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
import com.google.auto.value.AutoValue;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class ParamWithSimpleDoc {

  public static List<RequestObjectParamView> asRequestObjectParamViews(
      List<ParamWithSimpleDoc> ps) {
    List<RequestObjectParamView> views = new ArrayList<>();
    for (ParamWithSimpleDoc p : ps) {
      views.add(p.asRequestObjectParamView());
    }
    return views;
  }

  public static List<ParamDocView> asParamDocViews(List<ParamWithSimpleDoc> ps) {
    List<ParamDocView> views = new ArrayList<>();
    for (ParamWithSimpleDoc p : ps) {
      views.add(p.asParamDocView());
    }
    return views;
  }

  public abstract String name();

  @Nullable
  public abstract String nameAsMethodName();

  public abstract String elementTypeName();

  public abstract String typeName();

  public abstract String setCallName();

  public abstract String addCallName();

  public abstract String getCallName();

  public abstract boolean isMap();

  public abstract boolean isArray();

  public abstract boolean isPrimitive();

  public abstract boolean isOptional();

  @Nullable // Used in C#
  public abstract String defaultValue();

  public abstract List<String> docLines();

  public RequestObjectParamView asRequestObjectParamView() {
    return RequestObjectParamView.newBuilder()
        .name(name())
        .keyName(name())
        .nameAsMethodName(nameAsMethodName())
        .elementTypeName(elementTypeName())
        .typeName(typeName())
        .setCallName(setCallName())
        .addCallName(addCallName())
        .getCallName(getCallName())
        .isMap(isMap())
        .isArray(isArray())
        .isPrimitive(isPrimitive())
        .isOptional(isOptional())
        .defaultValue(defaultValue())
        .build();
  }

  public ParamDocView asParamDocView() {
    return SimpleParamDocView.newBuilder()
        .paramName(name())
        .typeName(typeName())
        .lines(docLines())
        .lines(docLines())
        .build();
  }

  public static Builder newBuilder() {
    return new AutoValue_ParamWithSimpleDoc.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder nameAsMethodName(String val);

    public abstract Builder elementTypeName(String val);

    public abstract Builder typeName(String val);

    public abstract Builder setCallName(String val);

    public abstract Builder addCallName(String val);

    public abstract Builder getCallName(String val);

    public abstract Builder isMap(boolean val);

    public abstract Builder isArray(boolean val);

    public abstract Builder isPrimitive(boolean val);

    public abstract Builder isOptional(boolean val);

    public abstract Builder defaultValue(String val);

    public abstract Builder docLines(List<String> val);

    public abstract ParamWithSimpleDoc build();
  }
}
