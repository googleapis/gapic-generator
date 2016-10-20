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
package com.google.api.codegen.gapic;

import com.google.auto.value.AutoValue;

/*
 * A context class which stores config data used by MainGapicProviderFactory.
 */
@AutoValue
public abstract class GapicGeneratorConfig {

  public abstract boolean disableTestGenerator();

  public abstract String id();

  public static Builder newBuilder() {
    return new AutoValue_GapicGeneratorConfig.Builder().disableTestGenerator(false);
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder disableTestGenerator(boolean val);

    public abstract Builder id(String val);

    public abstract GapicGeneratorConfig build();
  }
}
