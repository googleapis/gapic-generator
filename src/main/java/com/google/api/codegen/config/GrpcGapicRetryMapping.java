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
    for (com.google.api.codegen.grpc.MethodConfig mc : config.getMethodConfigList()) {
      RetryPolicy rp = mc.getRetryPolicy();
      String pName = "no_retry";
      if (mc.getRetryOrHedgingPolicyCase() == RetryOrHedgingPolicyCase.RETRY_POLICY) {
        pName = "retry_policy_" + retryNdx++;
      }

      long timeout = Durations.toMillis(mc.getTimeout());

      // construct retry params from RetryPolicy
      RetryParamsDefinitionProto.Builder rpb = RetryParamsDefinitionProto.newBuilder();
      rpb.setMaxRetryDelayMillis(Durations.toMillis(rp.getMaxBackoff()));
      rpb.setInitialRetryDelayMillis(Durations.toMillis(rp.getInitialBackoff()));
      rpb.setRetryDelayMultiplier(floatToDouble(rp.getBackoffMultiplier()));
      rpb.setTotalTimeoutMillis(timeout);
      rpb.setName(pName + "_params");
      paramsDefMap.putIfAbsent(rpb.getName(), rpb.build());

      // construct retry codes from RetryPolicy
      RetryCodesDefinitionProto.Builder rcb = RetryCodesDefinitionProto.newBuilder();
      rcb.setName(pName + "_codes");
      rp.getRetryableStatusCodesList()
          .forEach(
              code -> {
                rcb.addRetryCodes(code.name());
              });
      codesDefMap.putIfAbsent(rcb.getName(), rcb.build());

      // apply specific method config (overwrites) or apply service config to all methods (does
      // not overwrite method-specific policies)
      for (Name name : mc.getNameList()) {
        String serviceBase = name.getService();

        if (!Strings.isNullOrEmpty(name.getMethod())) {
          String fqn = fullyQualifiedName(serviceBase, name.getMethod());
          methodCodesMap.put(fqn, rcb.getName());
          methodParamsMap.put(fqn, rpb.getName());
          continue;
        }

        Interface interProto = protoInterfaces.get(serviceBase);
        interProto
            .getMethods()
            .forEach(
                method -> {
                  String fqn = method.getFullName();
                  methodCodesMap.putIfAbsent(fqn, rcb.getName());
                  methodParamsMap.putIfAbsent(fqn, rpb.getName());
                });
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

  /** converts a float to a double rounding to the nearest hundredth */
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
