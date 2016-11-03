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
package com.google.api.codegen.config;

import com.google.api.codegen.InvalidCollectionProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/**
 * UnformattedResourceNameConfig represents a resource name configuration that accepts a particular
 * unformatted value.
 */
@AutoValue
public abstract class UnformattedResourceNameConfig implements ResourceNameConfig {

  public abstract String getUnformattedValue();

  @Override
  public ResourceNameType getResourceNameType() {
    return ResourceNameType.SINGLE;
  }

  @Nullable
  public static UnformattedResourceNameConfig createInvalidCollection(
      DiagCollector diagCollector, InvalidCollectionProto invalidCollectionProto) {

    String entityName = invalidCollectionProto.getEntityName();
    String invalidValue = invalidCollectionProto.getInvalidValue();

    if (entityName == null || invalidValue == null) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "incorrectly configured InvalidCollection: name: "
                  + entityName
                  + ", value: "
                  + invalidValue));
      return null;
    }

    return new AutoValue_UnformattedResourceNameConfig(entityName, invalidValue);
  }
}
