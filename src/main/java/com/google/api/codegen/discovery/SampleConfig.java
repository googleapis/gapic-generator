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
package com.google.api.codegen.discovery;

import com.google.api.codegen.ApiaryConfig;
import com.google.api.codegen.DiscoveryImporter;
import com.google.auto.value.AutoValue;
import com.google.protobuf.Field;
import com.google.protobuf.Field.Cardinality;
import com.google.protobuf.Method;
import com.google.protobuf.Type;
import java.util.ArrayList;
import java.util.List;

@AutoValue
public abstract class SampleConfig {

  public static SampleConfig createSampleConfig(Method method, ApiaryConfig apiaryConfig) {
    Builder sampleConfig = newBuilder();
    sampleConfig.apiTitle(apiaryConfig.getApiTitle());
    sampleConfig.apiName(apiaryConfig.getApiName());
    sampleConfig.apiVersion(apiaryConfig.getApiVersion());
    sampleConfig.resources(apiaryConfig.getResources(method.getName()));
    sampleConfig.methodInfo(MethodInfo.createMethodInfo(method, apiaryConfig));
    return sampleConfig.build();
  }

  public abstract String apiTitle();

  public abstract String apiName();

  public abstract String apiVersion();

  public abstract List<String> resources();

  public abstract MethodInfo methodInfo();

  public static Builder newBuilder() {
    return new AutoValue_SampleConfig.Builder();
  }

  @AutoValue.Builder
  public static abstract class Builder {

    public abstract Builder apiTitle(String val);

    public abstract Builder apiName(String val);

    public abstract Builder apiVersion(String val);

    public abstract Builder resources(List<String> val);

    public abstract Builder methodInfo(MethodInfo val);

    public abstract SampleConfig build();
  }
}
