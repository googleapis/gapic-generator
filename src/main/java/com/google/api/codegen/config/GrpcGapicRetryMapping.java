/* Copyright 2019 Google LLC
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
package com.google.api.codegen.config;

import com.google.api.codegen.RetryCodesDefinitionProto;
import com.google.api.codegen.RetryParamsDefinitionProto;
import com.google.api.codegen.grpc.MethodConfig.Name;
import com.google.api.codegen.grpc.MethodConfig.RetryOrHedgingPolicyCase;
import com.google.api.codegen.grpc.MethodConfig.RetryPolicy;
import com.google.api.codegen.grpc.ServiceConfig;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.util.Durations;
import java.util.HashMap;
import java.util.Map;

@AutoValue
public abstract class GrpcGapicRetryMapping {

  public static GrpcGapicRetryMapping create(
      ServiceConfig config, ImmutableMap<String, Interface> protoInterfaces) {
    Map<String, String> methodCodesMap = new HashMap<>();
    Map<String, String> methodParamsMap = new HashMap<>();
    Map<String, RetryCodesDefinitionProto> codesDefMap = new HashMap<>();
    Map<String, RetryParamsDefinitionProto> paramsDefMap = new HashMap<>();

    // build retry-to-interface mapping from gRPC ServiceConfig
    int retryNdx = 1;
    for (com.google.api.codegen.grpc.MethodConfig methodConfig : config.getMethodConfigList()) {
      RetryPolicy retryPolicy = methodConfig.getRetryPolicy();
      String policyName = "no_retry";
      if (methodConfig.getRetryOrHedgingPolicyCase() == RetryOrHedgingPolicyCase.RETRY_POLICY) {
        policyName = "retry_policy_" + retryNdx++;
      }

      long timeout = Durations.toMillis(methodConfig.getTimeout());

      // construct retry params from RetryPolicy
      RetryParamsDefinitionProto.Builder paramsBuilder = RetryParamsDefinitionProto.newBuilder();
      paramsBuilder.setMaxRetryDelayMillis(Durations.toMillis(retryPolicy.getMaxBackoff()));
      paramsBuilder.setInitialRetryDelayMillis(Durations.toMillis(retryPolicy.getInitialBackoff()));
      paramsBuilder.setRetryDelayMultiplier(floatToDouble(retryPolicy.getBackoffMultiplier()));
      paramsBuilder.setTotalTimeoutMillis(timeout);
      paramsBuilder.setName(policyName + "_params");
      paramsDefMap.putIfAbsent(paramsBuilder.getName(), paramsBuilder.build());

      // construct retry codes from RetryPolicy
      RetryCodesDefinitionProto.Builder codesBuilder = RetryCodesDefinitionProto.newBuilder();
      codesBuilder.setName(policyName + "_codes");
      retryPolicy
          .getRetryableStatusCodesList()
          .forEach(code -> codesBuilder.addRetryCodes(code.name()));
      codesDefMap.putIfAbsent(codesBuilder.getName(), codesBuilder.build());

      // apply specific method config (overwrites) or apply service config to all methods (does
      // not overwrite method-specific policies)
      for (Name name : methodConfig.getNameList()) {
        String serviceBase = name.getService();

        if (!Strings.isNullOrEmpty(name.getMethod())) {
          String fullName = fullyQualifiedName(serviceBase, name.getMethod());
          methodCodesMap.put(fullName, codesBuilder.getName());
          methodParamsMap.put(fullName, paramsBuilder.getName());
          continue;
        }

        Interface interProto = protoInterfaces.get(serviceBase);
        for (Method method : interProto.getMethods()) {
          String fullName = method.getFullName();
          methodCodesMap.putIfAbsent(fullName, codesBuilder.getName());
          methodParamsMap.putIfAbsent(fullName, paramsBuilder.getName());
        }
      }
    }

    // make immutable
    ImmutableMap<String, String> codes = ImmutableMap.copyOf(methodCodesMap);
    ImmutableMap<String, String> params = ImmutableMap.copyOf(methodParamsMap);
    ImmutableMap<String, RetryCodesDefinitionProto> codesProto = ImmutableMap.copyOf(codesDefMap);
    ImmutableMap<String, RetryParamsDefinitionProto> paramsProto =
        ImmutableMap.copyOf(paramsDefMap);

    return new AutoValue_GrpcGapicRetryMapping.Builder()
        .setMethodCodesMap(codes)
        .setMethodParamsMap(params)
        .setCodesDefMap(codesProto)
        .setParamsDefMap(paramsProto)
        .build();
  }

  public abstract ImmutableMap<String, String> methodCodesMap();

  public abstract ImmutableMap<String, String> methodParamsMap();

  public abstract ImmutableMap<String, RetryCodesDefinitionProto> codesDefMap();

  public abstract ImmutableMap<String, RetryParamsDefinitionProto> paramsDefMap();

  public static Builder builder() {
    return new AutoValue_GrpcGapicRetryMapping.Builder();
  }

  /**
   * Converts a float to a double rounding to the nearest hundredth.
   *
   * <p>This is necessary because when a float is converted to a double the double is more precise.
   * When that same double is printed (e.g. in a template), the more precise value is used. For
   * example, a float of 1.3 is printed as 1.2999999523162842. This is undesirable because this
   * multiplier does not need to be this precise (it was originally a float) and it is not the
   * expected value.
   */
  private static double floatToDouble(float f) {
    return Math.round(f * 100) / 100.0;
  }

  private static String fullyQualifiedName(String service, String method) {
    return service + "." + method;
  }

  @AutoValue.Builder
  abstract static class Builder {

    abstract Builder setMethodCodesMap(ImmutableMap<String, String> methodCodesMap);

    abstract Builder setMethodParamsMap(ImmutableMap<String, String> methodParamsMap);

    abstract Builder setCodesDefMap(ImmutableMap<String, RetryCodesDefinitionProto> codesDefMap);

    abstract Builder setParamsDefMap(ImmutableMap<String, RetryParamsDefinitionProto> paramsDefMap);

    abstract GrpcGapicRetryMapping build();
  }
}
