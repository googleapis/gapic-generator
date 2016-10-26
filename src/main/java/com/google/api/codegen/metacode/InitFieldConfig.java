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
package com.google.api.codegen.metacode;

import com.google.auto.value.AutoValue;
import javax.annotation.Nullable;

/*
 * A meta data class which stores the configuration data of a initialized field.
 */
@AutoValue
public abstract class InitFieldConfig {
  private static final String randomValueToken = "$RANDOM";

  public abstract String fieldPath();

  @Nullable
  public abstract String entityName();

  @Nullable
  public abstract String value();

  /*
   * Parses the given config string and returns the corresponding object.
   */
  public static InitFieldConfig from(String initFieldConfigString) {
    String fieldName = null;
    String entityName = null;
    String value = null;

    String[] equalsParts = initFieldConfigString.split("[=]");
    if (equalsParts.length > 2) {
      throw new IllegalArgumentException("Inconsistent: found multiple '=' characters");
    } else if (equalsParts.length == 2) {
      value = parseValueString(equalsParts[1], equalsParts[0]);
    }

    String[] fieldSpecs = equalsParts[0].split("[%]");
    fieldName = fieldSpecs[0];
    if (fieldSpecs.length == 2) {
      entityName = fieldSpecs[1];
    } else if (fieldSpecs.length > 2) {
      throw new IllegalArgumentException("Inconsistent: found multiple '%' characters");
    }
    return new AutoValue_InitFieldConfig(fieldName, entityName, value);
  }

  public boolean hasSimpleInitValue() {
    return entityName() == null && value() != null;
  }

  public boolean isFormattedConfig() {
    return entityName() != null;
  }

  public boolean hasFormattedInitValue() {
    return entityName() != null && value() != null;
  }

  private static String parseValueString(String valueString, String stringToHash) {
    if (valueString.contains(randomValueToken)) {
      String randomValue = Integer.toString(Math.abs(stringToHash.hashCode()));
      valueString = valueString.replace(randomValueToken, randomValue);
    }
    return valueString;
  }
}
