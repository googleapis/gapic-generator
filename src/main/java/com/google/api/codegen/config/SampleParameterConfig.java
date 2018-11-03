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
import javax.annotation.Nullable;

/** SampleParameterConfig represents the configuration for standalone sample parameters. */
@AutoValue
public abstract class SampleParameterConfig {

  /** The identifier of the parameter, usually a protobuf field path. */
  public abstract String identifier();

  /**
   * Configured by attributes.parameter.read_file in the spec. If true, the value of this parameter
   * will be replaced by the contents of the file with the name that equals to the original value of
   * this parameter. Only allowed when the parameter represents a bytes field or a repeated bytes
   * field.
   *
   * <p>If the parameter is a bytes field, the value set to this parameter will be the file name.
   *
   * <p>If the parameter is a repeated bytes field, the value set to this parameter will be the a
   * list of file names separated by comma. For example, "foo.jpg,bar.jpg,baz.jpg".
   */
  public abstract boolean readFromFile();

  /** If true, the parameter will be an argument passed to the sample function. */
  public boolean isSampleArgument() {
    return sampleArgumentName() != null && !sampleArgumentName().isEmpty();
  }

  /**
   * If not null, the parameter will be an argument passed to the sample function. Returns the name
   * of the argument.
   */
  @Nullable
  public abstract String sampleArgumentName();

  public static Builder newBuilder() {
    return new AutoValue_SampleParameterConfig.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder identifier(String val);

    public abstract Builder readFromFile(boolean val);

    public abstract Builder sampleArgumentName(String val);

    public abstract SampleParameterConfig build();
  }
}
