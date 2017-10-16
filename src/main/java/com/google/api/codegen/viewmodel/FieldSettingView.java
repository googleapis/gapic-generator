/* Copyright 2016 Google Inc
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
import javax.annotation.Nullable;

@AutoValue
public abstract class FieldSettingView {

  public abstract String fieldSetFunction();

  public abstract String fieldAddFunction();

  public abstract String fieldGetFunction();

  public abstract String identifier();

  public abstract InitCodeLineView initCodeLine();

  public abstract String fieldName();

  public abstract boolean isMap();

  public abstract boolean isArray();

  @Nullable
  public abstract OneofConfigView oneofConfig();

  public boolean isOneof() {
    return oneofConfig() != null;
  }

  public abstract String elementTypeName();

  public abstract Boolean required();

  public static Builder newBuilder() {
    return new AutoValue_FieldSettingView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder fieldSetFunction(String val);

    public abstract Builder fieldAddFunction(String val);

    public abstract Builder fieldGetFunction(String val);

    public abstract Builder identifier(String val);

    public abstract Builder initCodeLine(InitCodeLineView val);

    public abstract Builder fieldName(String val);

    public abstract Builder isMap(boolean val);

    public abstract Builder isArray(boolean val);

    public abstract Builder oneofConfig(OneofConfigView val);

    public abstract Builder elementTypeName(String val);

    public abstract Builder required(Boolean val);

    public abstract FieldSettingView build();
  }
}
