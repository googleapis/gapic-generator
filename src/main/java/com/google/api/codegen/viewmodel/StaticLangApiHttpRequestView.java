/* Copyright 2017 Google Inc
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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import java.util.List;
import javax.annotation.Nullable;

/**
 * This ViewModel defines the view model structure of a generic HTTP request.
 *
 * <p>For example, this can be used to represent a Discovery Document's "schemas", "properties",
 * "additionalProperties", and "items".
 *
 * <p>This contains a subset of properties in the JSON Schema
 * https://tools.ietf.org/html/draft-zyp-json-schema-03#section-5.7.
 */
@AutoValue
public abstract class StaticLangApiHttpRequestView
    implements Comparable<StaticLangApiHttpRequestView> {
  @Nullable
  // TODO(andrealin) Populate and render this field.
  public abstract String description();

  // The possibly-transformed ID of the schema from the Discovery Doc.
  public abstract String name();

  // The type name for this Schema.
  public abstract String typeName();

  public abstract List<SimpleParamView> queryParams();

  public abstract List<SimpleParamView> pathParams();

  private ImmutableList<SimpleParamView> allParams;

  public List<SimpleParamView> allParams() {
    if (allParams != null) {
      return allParams;
    }
    ImmutableList.Builder<SimpleParamView> builder = new ImmutableList.Builder<>();
    builder.addAll(pathParams());
    builder.addAll(queryParams());
    allParams = builder.build();
    return allParams;
  }

  @Nullable
  // The object to include in the request, e.g. the Disk to be inserted for a CreateDisk operation.
  public abstract SimpleParamView requestObject();

  @Nullable
  // The response object, e.g. a Disk object returned after a GetDisk.
  public abstract SimpleParamView responseObject();

  // If there is a non-null request object.
  public boolean hasRequestObject() {
    return requestObject() != null;
  }

  // If there is a non-null response object.
  public boolean hasResponseObject() {
    return responseObject() != null;
  }

  public static StaticLangApiHttpRequestView.Builder newBuilder() {
    return new AutoValue_StaticLangApiHttpRequestView.Builder();
  }

  @AutoValue.Builder
  public abstract static class Builder {
    public abstract StaticLangApiHttpRequestView.Builder description(String val);

    public abstract StaticLangApiHttpRequestView.Builder name(String val);

    public abstract StaticLangApiHttpRequestView.Builder typeName(String val);

    public abstract StaticLangApiHttpRequestView.Builder queryParams(List<SimpleParamView> val);

    public abstract StaticLangApiHttpRequestView.Builder pathParams(List<SimpleParamView> val);

    public abstract StaticLangApiHttpRequestView.Builder requestObject(SimpleParamView val);

    public abstract StaticLangApiHttpRequestView.Builder responseObject(SimpleParamView val);

    public abstract StaticLangApiHttpRequestView build();
  }

  public int compareTo(StaticLangApiHttpRequestView o) {
    return this.name().compareTo(o.name());
  }
}
