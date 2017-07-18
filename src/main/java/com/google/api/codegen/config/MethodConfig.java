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
package com.google.api.codegen.config;

import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Oneof;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.List;
import javax.annotation.Nullable;
import org.joda.time.Duration;

/**
 * MethodConfig represents the code-gen config for a method, and includes the specification of
 * features like page streaming and parameter flattening.
 */
public abstract class MethodConfig {

  @Nullable
  public abstract PageStreamingConfig getPageStreaming();

  @Nullable
  public abstract GrpcStreamingConfig getGrpcStreaming();

  @Nullable
  public abstract ImmutableList<FlatteningConfig> getFlatteningConfigs();

  public abstract String getRetryCodesConfigName();

  public abstract String getRetrySettingsConfigName();

  public abstract Duration getTimeout();

  public abstract Iterable<FieldConfig> getRequiredFieldConfigs();

  public abstract Iterable<FieldConfig> getOptionalFieldConfigs();

  public abstract ResourceNameTreatment getDefaultResourceNameTreatment();

  @Nullable
  public abstract BatchingConfig getBatching();

  public abstract boolean hasRequestObjectMethod();

  public abstract ImmutableMap<String, String> getFieldNamePatterns();

  public abstract List<String> getSampleCodeInitFields();

  @Nullable
  public abstract String getRerouteToGrpcInterface();

  public abstract VisibilityConfig getVisibility();

  public abstract ReleaseLevel getReleaseLevel();

  @Nullable
  public abstract LongRunningConfig getLongRunningConfig();

  /** Returns true if the method is a streaming method */
  public static boolean isGrpcStreamingMethod(Method method) {
    return method.getRequestStreaming() || method.getResponseStreaming();
  }

  /** Returns true if this method has page streaming configured. */
  public boolean isPageStreaming() {
    return getPageStreaming() != null;
  }

  /** Returns true if this method has grpc streaming configured. */
  public boolean isGrpcStreaming() {
    return getGrpcStreaming() != null;
  }

  /** Returns the grpc streaming configuration of the method. */
  public GrpcStreamingType getGrpcStreamingType() {
    if (isGrpcStreaming()) {
      return getGrpcStreaming().getType();
    } else {
      return GrpcStreamingType.NonStreaming;
    }
  }

  /** Returns true if this method has flattening configured. */
  public boolean isFlattening() {
    return getFlatteningConfigs() != null;
  }

  /** Returns true if this method has batching configured. */
  public boolean isBatching() {
    return getBatching() != null;
  }

  public boolean isLongRunningOperation() {
    return getLongRunningConfig() != null;
  }

  public Iterable<FieldType> getRequiredFields() {
    return FieldConfig.toFieldTypeIterable(getRequiredFieldConfigs());
  }

  public Iterable<FieldType> getOptionalFields() {
    return FieldConfig.toFieldTypeIterable(getOptionalFieldConfigs());
  }

  /** Return the list of "one of" instances associated with the fields. */
  public Iterable<Oneof> getOneofs() {
    ImmutableSet.Builder<Oneof> answer = ImmutableSet.builder();

    for (FieldType field : getOptionalFields()) {
      if (field.getProtoBasedField().getOneof() == null) {
        continue;
      }
      answer.add(field.getProtoBasedField().getOneof());
    }

    return answer.build();
  }
}
