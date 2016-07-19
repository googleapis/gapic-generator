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

import com.google.api.codegen.viewmodel.ListMethodDetailView.Builder;
import com.google.auto.value.AutoValue;

import java.util.List;

@AutoValue
public abstract class OptionalArrayMethodView implements ApiMethodView {

  public abstract ApiMethodType type();

  public abstract String apiClassName();

  public abstract String apiVariableName();

  public abstract InitCodeView initCode();

  public abstract ApiMethodDocView doc();

  public abstract String name();

  public abstract String requestTypeName();

  public abstract String key();

  public abstract String grpcMethodName();

  public abstract List<DynamicDefaultableParamView> methodParams();

  public abstract List<RequestObjectParamView> requiredRequestObjectParams();

  public abstract List<RequestObjectParamView> optionalRequestObjectParams();

  public abstract boolean hasReturnValue();

  public static Builder newBuilder() {
    return new AutoValue_OptionalArrayMethodView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {
    public abstract Builder type(ApiMethodType val);

    public abstract Builder apiClassName(String val);

    public abstract Builder apiVariableName(String val);

    public abstract Builder initCode(InitCodeView val);

    public abstract Builder doc(ApiMethodDocView val);

    public abstract Builder name(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder key(String val);

    public abstract Builder grpcMethodName(String val);

    public abstract Builder methodParams(List<DynamicDefaultableParamView> val);

    public abstract Builder requiredRequestObjectParams(List<RequestObjectParamView> val);

    public abstract Builder optionalRequestObjectParams(List<RequestObjectParamView> val);

    public abstract Builder hasReturnValue(boolean val);

    public abstract OptionalArrayMethodView build();
  }
}
