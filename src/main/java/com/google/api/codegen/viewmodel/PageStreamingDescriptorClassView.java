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
package com.google.api.codegen.viewmodel;

import com.google.auto.value.AutoValue;
import java.util.List;
import javax.annotation.Nullable;

@AutoValue
public abstract class PageStreamingDescriptorClassView {
  public abstract String name();

  public abstract String typeName();

  public abstract String requestTypeName();

  public abstract String responseTypeName();

  public abstract String resourceTypeName();

  public abstract String tokenTypeName();

  public abstract String defaultTokenValue();

  public abstract String requestTokenSetFunction();

  @Nullable
  public abstract String requestPageSizeSetFunction();

  @Nullable
  public abstract String requestPageSizeGetFunction();

  public abstract String responseTokenGetFunction();

  public abstract List<String> resourcesFieldGetFunctions();

  public abstract boolean resourcesFieldIsMap();

  public boolean requestHasPageSize() {
    return requestPageSizeSetFunction() != null && requestPageSizeGetFunction() != null;
  }

  public static Builder newBuilder() {
    return new AutoValue_PageStreamingDescriptorClassView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String val);

    public abstract Builder typeName(String val);

    public abstract Builder requestTypeName(String val);

    public abstract Builder responseTypeName(String val);

    public abstract Builder resourceTypeName(String val);

    public abstract Builder tokenTypeName(String val);

    public abstract Builder defaultTokenValue(String val);

    public abstract Builder requestTokenSetFunction(String val);

    public abstract Builder requestPageSizeSetFunction(String val);

    public abstract Builder requestPageSizeGetFunction(String val);

    public abstract Builder responseTokenGetFunction(String val);

    public abstract Builder resourcesFieldGetFunctions(List<String> val);

    public abstract Builder resourcesFieldIsMap(boolean val);

    public abstract PageStreamingDescriptorClassView build();
  }
}
