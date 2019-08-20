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
import com.google.rpc.Code;
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

    // add no_retry configs for unknown/unspecified methods
    RetryParamsDefinitionProto.Builder defaultParamsBuilder =
        retryPolicyToParamsBuilder(RetryPolicy.getDefaultInstance(), 0, "no_retry_params");
    paramsDefMap.putIfAbsent(defaultParamsBuilder.getName(), defaultParamsBuilder.build());

    RetryCodesDefinitionProto.Builder defaultCodesBuilder =
        retryPolicyToCodesBuilder(RetryPolicy.getDefaultInstance(), "no_retry_codes");
    codesDefMap.putIfAbsent(defaultCodesBuilder.getName(), defaultCodesBuilder.build());

    // build retry-to-interface mapping from gRPC ServiceConfig
    int retryIndex = 1;
    int noRetryIndex = 1;
    for (com.google.api.codegen.grpc.MethodConfig methodConfig : config.getMethodConfigList()) {
      RetryPolicy retryPolicy = methodConfig.getRetryPolicy();
      String policyName;
      if (methodConfig.getRetryOrHedgingPolicyCase() == RetryOrHedgingPolicyCase.RETRY_POLICY) {
        policyName = "retry_policy_" + retryIndex++;
      } else {
        // make "unique" no_retry configs because the MethodConfig timeout may differ
        policyName = "no_retry_" + noRetryIndex++;
      }

      long timeout = Durations.toMillis(methodConfig.getTimeout());

      // construct retry params from RetryPolicy
      RetryParamsDefinitionProto.Builder paramsBuilder =
          retryPolicyToParamsBuilder(retryPolicy, timeout, policyName + "_params");
      paramsDefMap.putIfAbsent(paramsBuilder.getName(), paramsBuilder.build());

      // construct retry codes from RetryPolicy
      RetryCodesDefinitionProto.Builder codesBuilder =
          retryPolicyToCodesBuilder(retryPolicy, policyName + "_codes");
      codesDefMap.putIfAbsent(codesBuilder.getName(), codesBuilder.build());

      // apply the MethodConfig.RetryPolicy to names
      for (Name name : methodConfig.getNameList()) {
        applyRetryPolicyToName(
            name,
            codesBuilder.getName(),
            paramsBuilder.getName(),
            methodCodesMap,
            methodParamsMap,
            protoInterfaces);
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
   * Applies the converted RetryPolicy to the name(s) specified in the given MethodConfig.Name.
   *
   * <p>If a Name contains a local method name, the corresponding RetryPolicy takes precedence over
   * a top-level Service-defined RetryPolicy (overwrites). If a Name contains only a service name,
   * the corresponding RetryPolicy is applied to all methods in that service interface, without
   * overwriting existing method-specific entries.
   */
  private static void applyRetryPolicyToName(
      Name name,
      String codesName,
      String paramsName,
      Map<String, String> methodCodesMap,
      Map<String, String> methodParamsMap,
      Map<String, Interface> protoInterfaces) {
    String service = name.getService();
    String method = name.getMethod();

    // a method-specific name overwrites an existing service-defined entry
    if (!Strings.isNullOrEmpty(method)) {
      String fullName = fullyQualifiedName(service, method);
      methodCodesMap.put(fullName, codesName);
      methodParamsMap.put(fullName, paramsName);
      return;
    }

    // apply the RetryPolicy to all methods in the service interface that don't already have an
    // entry
    Interface interProto = protoInterfaces.get(service);
    for (Method methodProto : interProto.getMethods()) {
      String fullName = methodProto.getFullName();
      methodCodesMap.putIfAbsent(fullName, codesName);
      methodParamsMap.putIfAbsent(fullName, paramsName);
    }
  }

  private static RetryParamsDefinitionProto.Builder retryPolicyToParamsBuilder(
      RetryPolicy retryPolicy, long timeout, String policyName) {
    return RetryParamsDefinitionProto.newBuilder()
        .setMaxRetryDelayMillis(Durations.toMillis(retryPolicy.getMaxBackoff()))
        .setInitialRetryDelayMillis(Durations.toMillis(retryPolicy.getInitialBackoff()))
        .setRetryDelayMultiplier(
            convertFloatToDouble(retryPolicy.getBackoffMultiplier()))
        .setTotalTimeoutMillis(timeout)
        .setName(policyName);
  }

  private static RetryCodesDefinitionProto.Builder retryPolicyToCodesBuilder(
      RetryPolicy retryPolicy, String codesName) {
    RetryCodesDefinitionProto.Builder codesBuilder = RetryCodesDefinitionProto.newBuilder();
    for (Code code : retryPolicy.getRetryableStatusCodesList()) {
      codesBuilder.addRetryCodes(code.name());
    }
    return codesBuilder.setName(codesName);
  }

  /**
   * Converts a float to a double via their string representations without any rounding.
   *
   * <p>This is necessary because when a float is converted to a double the double is more precise.
   * When that same double is printed (e.g. in a template), the more precise value is used. For
   * example, a float of 1.3 is printed as 1.2999999523162842. This is undesirable because this
   * multiplier does not need to be this precise (it was originally a float) and it is not the
   * expected value.
   */
  private static double convertFloatToDouble(float f) {
    return Double.valueOf(Float.toString(f));
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
