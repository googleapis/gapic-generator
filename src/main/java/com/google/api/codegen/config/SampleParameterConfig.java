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

import com.google.auto.value.AutoValue;

/** SampleParameterConfig represents the configuration for standalone sample parameters. */
@AutoValue
public abstract class SampleParameterConfig {

  public abstract String field();

  /**
   * If true, the value of this parameter will be replaced by the contents of the file with the name
   * that equals to the original value of this parameter. Only allowed when the parameter represents
   * a bytes field.
   */
  public abstract boolean isFile();

  /** If true, the parameter will be an argument passed to the sample function. */
  public boolean isInputParameter() {
    return !inputParameter().isEmpty();
  }

  /** The comment of the parameter. */
  public abstract String comment();

  /**
   * If not empty, the parameter will be a parameter passed to the sample function. Returns the name
   * of the parameter.
   */
  public abstract String inputParameter();

  public static Builder newBuilder() {
    return new AutoValue_SampleParameterConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder field(String val);

    public abstract Builder isFile(boolean val);

    public abstract Builder inputParameter(String val);

    public abstract Builder comment(String val);

    public abstract SampleParameterConfig build();
  }
}
