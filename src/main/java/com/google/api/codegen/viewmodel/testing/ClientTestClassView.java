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
package com.google.api.codegen.viewmodel.testing;

import com.google.auto.value.AutoValue;
import java.util.List;

@AutoValue
public abstract class ClientTestClassView {

  public abstract String name();

  public abstract String apiClassName();

  public abstract String apiSettingsClassName();

  public abstract List<MockServiceUsageView> mockServices();

  public abstract List<TestCaseView> testCases();

  public static Builder newBuilder() {
    return new AutoValue_ClientTestClassView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {

    public abstract Builder name(String val);

    public abstract Builder apiClassName(String val);

    public abstract Builder apiSettingsClassName(String val);

    public abstract Builder mockServices(List<MockServiceUsageView> val);

    public abstract Builder testCases(List<TestCaseView> val);

    public abstract ClientTestClassView build();
  }
}
