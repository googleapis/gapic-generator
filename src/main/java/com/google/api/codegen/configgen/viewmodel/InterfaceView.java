/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;

/** Represents the API interface configuration. */
@AutoValue
public abstract class InterfaceView {
  /** The fully-qualified name of the API interface. */
  public abstract String name();

  /** The definition for retryable codes. */
  public abstract List<RetryCodeView> retryCodesDef();

  /** The defintion for retry/backoff parameters. */
  public abstract List<RetryParamView> retryParamsDef();

  /** The list of resource collection configurations. */
  public abstract List<CollectionView> collections();

  /** The list of method configurations. */
  public abstract List<MethodView> methods();

  public static Builder newBuilder() {
    return new AutoValue_InterfaceView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder name(String val);

    public abstract Builder retryCodesDef(List<RetryCodeView> val);

    public abstract Builder retryParamsDef(List<RetryParamView> val);

    public abstract Builder collections(List<CollectionView> val);

    public abstract Builder methods(List<MethodView> val);

    public abstract InterfaceView build();
  }
}
