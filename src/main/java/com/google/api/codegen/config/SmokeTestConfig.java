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
package com.google.api.codegen.config;

import com.google.api.codegen.SmokeTestConfigProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import java.util.List;

/** SmokeTestConfig represents the smoke test configuration for a method. */
@AutoValue
public abstract class SmokeTestConfig {
  public abstract MethodModel getMethod();

  public abstract List<String> getInitFieldConfigStrings();

  public abstract String getFlatteningName();

  public static SmokeTestConfig createSmokeTestConfig(
      InterfaceModel apiInterface,
      SmokeTestConfigProto smokeTestConfigProto,
      DiagCollector diagCollector) {
    MethodModel testedMethod = null;
    for (MethodModel method : apiInterface.getMethods()) {
      if (method.getSimpleName().equals(smokeTestConfigProto.getMethod())) {
        testedMethod = method;
        break;
      }
    }

    if (testedMethod != null) {
      return new AutoValue_SmokeTestConfig(
          testedMethod,
          smokeTestConfigProto.getInitFieldsList(),
          smokeTestConfigProto.getFlatteningGroupName());
    } else {
      diagCollector.addDiag(
          Diag.error(SimpleLocation.TOPLEVEL, "The configured smoke test method does not exist."));
      return null;
    }
  }
}
