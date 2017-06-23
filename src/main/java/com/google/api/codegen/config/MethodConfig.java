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
package com.google.api.codegen.config;

import com.google.api.codegen.FlatteningGroupProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.config.GrpcStreamingConfig.GrpcStreamingType;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Oneof;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.protobuf.Empty;
import java.util.List;
import javax.annotation.Nullable;
import org.joda.time.Duration;

/**
 * GapicMethodConfig represents the code-gen config for a method, and includes the specification of
 * features like page streaming and parameter flattening.
 */
public abstract class MethodConfig {
  public abstract Method getMethod();

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

  @Nullable
  static ImmutableList<FlatteningConfig> createFlattening(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      MethodConfigProto methodConfigProto,
      Method method) {
    boolean missing = false;
    ImmutableList.Builder<FlatteningConfig> flatteningGroupsBuilder = ImmutableList.builder();
    for (FlatteningGroupProto flatteningGroup : methodConfigProto.getFlattening().getGroupsList()) {
      FlatteningConfig groupConfig =
          FlatteningConfig.createFlattening(
              diagCollector,
              messageConfigs,
              resourceNameConfigs,
              methodConfigProto,
              flatteningGroup,
              method);
      if (groupConfig == null) {
        missing = true;
      } else {
        flatteningGroupsBuilder.add(groupConfig);
      }
    }
    if (missing) {
      return null;
    }

    return flatteningGroupsBuilder.build();
  }

  static Iterable<Field> getRequiredFields(
      DiagCollector diagCollector, Method method, List<String> requiredFieldNames) {
    ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
    for (String fieldName : requiredFieldNames) {
      Field requiredField = method.getInputMessage().lookupField(fieldName);
      if (requiredField == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Required field '%s' not found (in method %s)",
                fieldName,
                method.getFullName()));
        return null;
      }
      fieldsBuilder.add(requiredField);
    }
    return fieldsBuilder.build();
  }

  static Iterable<Field> getOptionalFields(Method method, List<String> requiredFieldNames) {
    ImmutableList.Builder<Field> fieldsBuilder = ImmutableList.builder();
    for (Field field : method.getInputType().getMessageType().getFields()) {
      if (requiredFieldNames.contains(field.getSimpleName())) {
        continue;
      }
      fieldsBuilder.add(field);
    }
    return fieldsBuilder.build();
  }

  static Iterable<FieldConfig> createFieldNameConfigs(
      DiagCollector diagCollector,
      ResourceNameMessageConfigs messageConfigs,
      ResourceNameTreatment defaultResourceNameTreatment,
      ImmutableMap<String, String> fieldNamePatterns,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      Iterable<Field> fields) {
    ImmutableList.Builder<FieldConfig> fieldConfigsBuilder = ImmutableList.builder();
    for (Field field : fields) {
      fieldConfigsBuilder.add(
          FieldConfig.createFieldConfig(
              diagCollector,
              messageConfigs,
              fieldNamePatterns,
              resourceNameConfigs,
              field,
              null,
              defaultResourceNameTreatment));
    }
    return fieldConfigsBuilder.build();
  }

  /** Returns true if the method is a streaming method */
  public static boolean isGrpcStreamingMethod(Method method) {
    return method.getRequestStreaming() || method.getResponseStreaming();
  }

  /** Returns true if the method returns google.protobuf.empty message */
  public static boolean isReturnEmptyMessageMethod(Method method) {
    MessageType returnMessageType = method.getOutputMessage();
    return Empty.getDescriptor().getFullName().equals(returnMessageType.getFullName());
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

  public Iterable<Field> getRequiredFields() {
    return FieldConfig.toFieldIterable(getRequiredFieldConfigs());
  }

  public Iterable<Field> getOptionalFields() {
    return FieldConfig.toFieldIterable(getOptionalFieldConfigs());
  }

  /** Return the list of "one of" instances associated with the fields. */
  public Iterable<Oneof> getOneofs() {
    ImmutableSet.Builder<Oneof> answer = ImmutableSet.builder();

    for (Field field : getOptionalFields()) {
      if (field.getOneof() == null) {
        continue;
      }
      answer.add(field.getOneof());
    }

    return answer.build();
  }
}
