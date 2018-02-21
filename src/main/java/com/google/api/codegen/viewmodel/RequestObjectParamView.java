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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class RequestObjectParamView implements Comparable<RequestObjectParamView> {
  public abstract String name();

  public abstract String keyName();

  @Nullable
  public abstract String nameAsMethodName();

  public abstract String elementTypeName();

  public abstract String typeName();

  public abstract String setCallName();

  public abstract String addCallName();

  public abstract String getCallName();

  @Nullable
  public abstract String transformParamFunctionName();

  @Nullable
  public abstract String formatMethodName();

  public abstract boolean isMap();

  public abstract boolean isArray();

  public boolean isCollection() {
    return isMap() || isArray();
  }

  public abstract boolean isPrimitive();

  public abstract boolean isOptional();

  @Nullable // Used in C#
  public abstract String defaultValue();

  public boolean hasDefaultValue() {
    return defaultValue() != null;
  }

  @Nullable // Used in C#
  public abstract String optionalDefault();

  public boolean hasOptionalDefault() {
    return optionalDefault() != null;
  }

  public boolean hasTransformParamFunction() {
    return transformParamFunctionName() != null;
  }

  public boolean hasFormatMethodName() {
    return formatMethodName() != null;
  }

  // Methods for getting/setting the fields of the generated class.
  public abstract List<StaticMemberView> fieldCopyMethods();

  public static Builder newBuilder() {
    return new AutoValue_RequestObjectParamView.Builder()
        .fieldCopyMethods(new ArrayList<StaticMemberView>());
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder keyName(String val);

    public abstract Builder nameAsMethodName(String val);

    public abstract Builder elementTypeName(String val);

    public abstract Builder typeName(String val);

    public abstract Builder setCallName(String val);

    public abstract Builder addCallName(String val);

    public abstract Builder getCallName(String val);

    public abstract Builder transformParamFunctionName(String val);

    public abstract Builder formatMethodName(String val);

    public abstract Builder isMap(boolean val);

    public abstract Builder isArray(boolean val);

    public abstract Builder isPrimitive(boolean val);

    public abstract Builder isOptional(boolean val);

    public abstract Builder defaultValue(String val);

    public abstract Builder optionalDefault(String val);

    public abstract Builder fieldCopyMethods(List<StaticMemberView> val);

    public abstract RequestObjectParamView build();
  }

  @Override
  public int compareTo(RequestObjectParamView o) {
    return this.name().compareTo(o.name());
  }
}
