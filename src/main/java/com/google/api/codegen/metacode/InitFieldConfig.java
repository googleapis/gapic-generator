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

  public static Builder newBuilder() {
    return new AutoValue_InitFieldConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder fieldPath(String val);

    public abstract Builder entityName(String val);

    public abstract Builder value(InitValue val);

    public abstract InitFieldConfig build();
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
}
