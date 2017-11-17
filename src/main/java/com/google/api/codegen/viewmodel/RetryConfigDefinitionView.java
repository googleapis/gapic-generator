/* Copyright 2016 Google LLC
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

import com.google.api.gax.retrying.RetrySettings;
import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class RetryConfigDefinitionView {

  public abstract Name name();

  public abstract List<String> retryCodes();

  public abstract RetrySettings params();

  public static Builder newBuilder() {
    return new AutoValue_RetryConfigDefinitionView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(Name val);

    public abstract Builder retryCodes(List<String> val);

    public abstract Builder params(RetrySettings val);

    public abstract RetryConfigDefinitionView build();
  }

  @AutoValue
  public abstract static class Name implements Comparable<Name> {
    public abstract String retrySettingsConfigName();

    public abstract String retryCodesConfigName();

    public static Name create(String paramsName, String codesName) {
      return new AutoValue_RetryConfigDefinitionView_Name(paramsName, codesName);
    }

    public int compareTo(Name other) {
      int c = retrySettingsConfigName().compareTo(other.retrySettingsConfigName());
      if (c != 0) {
        return c;
      }
      return retryCodesConfigName().compareTo(other.retryCodesConfigName());
    }
  }
}
