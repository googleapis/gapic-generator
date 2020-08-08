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
public abstract class HttpMethodView {
  @Nullable
  public abstract List<String> pathParams();

  @Nullable
  public abstract List<String> queryParams();

  // Return the HTTP method, e.g. GET, POST
  public abstract String httpMethod();

  public abstract String fullMethodName();

  public abstract String pathTemplate();

  // The type name of the ResourceName used by this method.
  @Nullable
  public abstract String resourceNameTypeName();

  // The field name for the method's request object's ResourceName.
  @Nullable
  public abstract String resourceNameFieldName();

  @Nullable
  public abstract List<HttpMethodSelectorView> pathParamSelectors();

  @Nullable
  public abstract List<HttpMethodSelectorView> queryParamSelectors();

  @Nullable
  public abstract List<HttpMethodSelectorView> bodySelectors();

  public boolean hasBody() {
    return !bodySelectors().isEmpty();
  }

  public static Builder newBuilder() {
    return new AutoValue_HttpMethodView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract Builder pathParams(List<String> type);

    public abstract Builder queryParams(List<String> val);

    public abstract Builder httpMethod(String name);

    public abstract Builder fullMethodName(String name);

    public abstract Builder pathTemplate(String name);

    public abstract Builder resourceNameTypeName(String name);

    public abstract Builder resourceNameFieldName(String name);

    public abstract Builder pathParamSelectors(List<HttpMethodSelectorView> val);

    public abstract Builder queryParamSelectors(List<HttpMethodSelectorView> val);

    public abstract Builder bodySelectors(List<HttpMethodSelectorView> val);

    public abstract HttpMethodView build();
  }
}
