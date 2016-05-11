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
package com.google.api.codegen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;

import javax.annotation.Nullable;

/**
 * FlatteningConfig represents the flattening configuration for a method.
 */
public class FlatteningConfig {
  private final ImmutableList<ImmutableList<Field>> flatteningGroups;

  /**
   * Creates an instance of FlatteningConfig based on FlatteningConfigProto, linking it up with the
   * provided method.
   */
  @Nullable
  public static FlatteningConfig createFlattening(
      DiagCollector diagCollector, FlatteningConfigProto flattening, Method method) {
    boolean missing = false;
    ImmutableList.Builder<ImmutableList<Field>> flatteningGroupsBuilder = ImmutableList.builder();
    for (FlatteningGroupProto flatteningGroup : flattening.getGroupsList()) {
      ImmutableList.Builder<Field> parametersBuilder = ImmutableList.builder();
      for (String parameter : flatteningGroup.getParametersList()) {
        Field parameterField = method.getInputMessage().lookupField(parameter);
        if (parameterField != null) {
          parametersBuilder.add(parameterField);
        } else {
          diagCollector.addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "Field missing for flattening: method = %s, message type = %s, field = %s",
                  method.getFullName(),
                  method.getInputMessage().getFullName(),
                  parameter));
          missing = true;
        }
      }
      flatteningGroupsBuilder.add(parametersBuilder.build());
    }
    if (missing) {
      return null;
    }
    return new FlatteningConfig(flatteningGroupsBuilder.build());
  }

  private FlatteningConfig(ImmutableList<ImmutableList<Field>> flatteningGroups) {
    this.flatteningGroups = flatteningGroups;
  }

  /**
   * Returns the list of group lists of fields which may be flattened in combination.
   */
  public ImmutableList<ImmutableList<Field>> getFlatteningGroups() {
    return flatteningGroups;
  }
}
