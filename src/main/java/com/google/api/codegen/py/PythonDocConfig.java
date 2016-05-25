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
package com.google.api.codegen.py;

import com.google.api.codegen.DocConfig;
import com.google.api.codegen.metacode.InitCode;
import com.google.api.codegen.metacode.InputParameter;
import com.google.api.tools.framework.model.Field;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;

/**
 * Represents the Python documentation settings for an Api method.
 */
@AutoValue
abstract class PythonDocConfig extends DocConfig {
  public static PythonDocConfig.Builder newBuilder() {
    return new AutoValue_PythonDocConfig.Builder();
  }
  
  @AutoValue.Builder
  abstract static class Builder extends DocConfig.Builder<Builder> {
    public abstract PythonDocConfig build();

    public abstract Builder setApiName(String serviceName);

    public abstract Builder setMethodName(String methodName);

    public abstract Builder setReturnType(String returnType);

    public abstract Builder setInitCode(InitCode initCode);

    public abstract Builder setParams(ImmutableList<InputParameter> params);

    public abstract Builder setPagedVariant(boolean paged);

    public abstract Builder setCallableVariant(boolean callable);

    public abstract Builder setResourcesFieldForUnpagedListCallable(Field field);

    @Override
    protected Builder setInitCodeProxy(InitCode initCode) {
      return setInitCode(initCode);
    }

    @Override
    protected Builder setParamsProxy(ImmutableList<InputParameter> params) {
      return setParams(params);
    }
  }
}
