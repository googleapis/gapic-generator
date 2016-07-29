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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class PageStreamingDescriptorClassView {
  public abstract String name();

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract String resourceTypeName();

  public abstract String tokenTypeName();

  public abstract String defaultTokenValue();

  public abstract String fnSetRequestToken();

  public abstract String fnGetResponseToken();

  public abstract String fnGetResourcesField();

  public static Builder newBuilder() {
    return new AutoValue_PageStreamingDescriptorClassView.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder name(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder resourceTypeName(String val);

    public abstract Builder tokenTypeName(String val);

    public abstract Builder defaultTokenValue(String val);

    public abstract Builder fnSetRequestToken(String val);

    public abstract Builder fnGetResponseToken(String val);

    public abstract Builder fnGetResourcesField(String val);

    public abstract PageStreamingDescriptorClassView build();
  }
}
