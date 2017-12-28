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
package com.google.api.codegen.metacode;

import com.google.auto.value.AutoValue;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

/*
 * A meta data class which stores the configuration data of a initialized field.
 */
@AutoValue
public abstract class InitFieldConfig {
  public static final String PROJECT_ID_VARIABLE_NAME = "project_id";
  public static final String RANDOM_TOKEN = "$RANDOM";
  public static final Pattern RANDOM_TOKEN_PATTERN = Pattern.compile(Pattern.quote(RANDOM_TOKEN));

  private static final String PROJECT_ID_TOKEN = "$PROJECT_ID";

  public abstract String fieldPath();

  @Nullable
  public abstract String entityName();

  @Nullable
  public abstract InitValue value();

  /*
   * Parses the given config string and returns the corresponding object.
   */
  public static InitFieldConfig from(String initFieldConfigString) {
    String fieldName = null;
    String entityName = null;
    InitValue value = null;

    String[] equalsParts = initFieldConfigString.split("[=]");
    if (equalsParts.length > 2) {
      throw new IllegalArgumentException("Inconsistent: found multiple '=' characters");
    } else if (equalsParts.length == 2) {
      value = parseValueString(equalsParts[1]);
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

  private static InitValue parseValueString(String valueString) {
    InitValue initValue = InitValue.createLiteral(valueString);
    if (valueString.contains(RANDOM_TOKEN)) {
      initValue = InitValue.createRandom(valueString);
    } else if (valueString.contains(PROJECT_ID_TOKEN)) {
      if (!valueString.equals(PROJECT_ID_TOKEN)) {
        throw new IllegalArgumentException("Inconsistent: found project ID as a substring ");
      }
      valueString = PROJECT_ID_VARIABLE_NAME;
      initValue = InitValue.createVariable(valueString);
    }
    return initValue;
  }
}
