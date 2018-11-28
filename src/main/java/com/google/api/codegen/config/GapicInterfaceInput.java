/* Copyright 2018 Google LLC
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
package com.google.api.codegen.config;

import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;
import java.util.Map;

/** Struct class that holds a proto interface, and its corresponding GAPIC interface config. */
@AutoValue
public abstract class GapicInterfaceInput {

  public abstract Interface getInterface();

  public abstract InterfaceConfigProto getInterfaceConfigProto();

  /**
   * Get this client's appropriately-rerouted methods and their corresponding GAPIC method configs.
   */
  public abstract Map<Method, MethodConfigProto> getMethodsToGenerate();

  public abstract Builder toBuilder();

  public static Builder newBuilder() {
    return new AutoValue_GapicInterfaceInput.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder setInterface(Interface val);

    public abstract Builder setInterfaceConfigProto(InterfaceConfigProto val);

    public abstract Builder setMethodsToGenerate(Map<Method, MethodConfigProto> val);

    public abstract GapicInterfaceInput build();
  }
}
