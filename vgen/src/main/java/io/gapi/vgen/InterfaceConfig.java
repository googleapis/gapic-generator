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
package io.gapi.vgen;

import com.google.api.gax.core.RetrySettings;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import org.joda.time.Duration;

import io.grpc.Status;

import java.util.Collection;
import java.util.EnumSet;

import javax.annotation.Nullable;

/**
 * InterfaceConfig represents the code-gen config for an API interface, and includes the
 * configuration for methods and resource names.
 */
public class InterfaceConfig {
  private final ImmutableMap<String, CollectionConfig> collectionConfigs;
  private final ImmutableMap<String, MethodConfig> methodConfigMap;
  private final ImmutableMap<String, ImmutableSet<Status.Code>> retryCodesDefinition;
  private final ImmutableMap<String, RetrySettings> retrySettingsDefinition;

  /**
   * Creates an instance of InterfaceConfig based on ConfigProto, linking up method configurations
   * with specified methods in methodConfigMap. On errors, null will be returned, and diagnostics
   * are reported to the model.
   */
  @Nullable
  public static InterfaceConfig createInterfaceConfig(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto, Interface iface) {
    ImmutableMap<String, CollectionConfig> collectionConfigs =
        createCollectionConfigs(diagCollector, interfaceConfigProto);

    ImmutableMap<String, ImmutableSet<Status.Code>> retryCodesDefinition =
        createRetryCodesDefinition(diagCollector, interfaceConfigProto);
    ImmutableMap<String, RetrySettings> retrySettingsDefinition =
        createRetrySettingsDefinition(diagCollector, interfaceConfigProto);

    ImmutableMap<String, MethodConfig> methodConfigMap = null;
    if (retryCodesDefinition != null && retrySettingsDefinition != null) {
      methodConfigMap =
          createMethodConfigMap(
              diagCollector,
              interfaceConfigProto,
              iface,
              retryCodesDefinition.keySet(),
              retrySettingsDefinition.keySet());
    }

    if (collectionConfigs == null || methodConfigMap == null) {
      return null;
    } else {
      return new InterfaceConfig(
          collectionConfigs, methodConfigMap, retryCodesDefinition, retrySettingsDefinition);
    }
  }

  private static ImmutableMap<String, CollectionConfig> createCollectionConfigs(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, CollectionConfig> collectionConfigsBuilder =
        ImmutableMap.builder();

    for (CollectionConfigProto collectionConfigProto : interfaceConfigProto.getCollectionsList()) {
      CollectionConfig collectionConfig =
          CollectionConfig.createCollection(diagCollector, collectionConfigProto);
      if (collectionConfig == null) {
        continue;
      }
      collectionConfigsBuilder.put(collectionConfig.getEntityName(), collectionConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return collectionConfigsBuilder.build();
    }
  }

  private static ImmutableMap<String, ImmutableSet<Status.Code>> createRetryCodesDefinition(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, ImmutableSet<Status.Code>> builder =
        ImmutableMap.<String, ImmutableSet<Status.Code>>builder();
    for (RetryCodesDefinitionProto retryDef : interfaceConfigProto.getRetryCodesDefList()) {
      EnumSet<Status.Code> codes = EnumSet.noneOf(Status.Code.class);
      for (String codeText : retryDef.getRetryCodesList()) {
        try {
          codes.add(Status.Code.valueOf(codeText));
        } catch (IllegalArgumentException e) {
          diagCollector.addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "status code not found: '%s' (in interface %s)",
                  codeText,
                  interfaceConfigProto.getName()));
        }
      }
      builder.put(retryDef.getName(), Sets.immutableEnumSet(codes));
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  private static ImmutableMap<String, RetrySettings> createRetrySettingsDefinition(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, RetrySettings> builder =
        ImmutableMap.<String, RetrySettings>builder();
    for (RetryParamsDefinitionProto retryDef : interfaceConfigProto.getRetryParamsDefList()) {
      try {
        RetrySettings settings =
            RetrySettings.newBuilder()
                .setInitialRetryDelay(Duration.millis(retryDef.getInitialRetryDelayMillis()))
                .setRetryDelayMultiplier(retryDef.getRetryDelayMultiplier())
                .setMaxRetryDelay(Duration.millis(retryDef.getMaxRetryDelayMillis()))
                .setInitialRpcTimeout(Duration.millis(retryDef.getInitialRpcTimeoutMillis()))
                .setRpcTimeoutMultiplier(retryDef.getRpcTimeoutMultiplier())
                .setMaxRpcTimeout(Duration.millis(retryDef.getMaxRpcTimeoutMillis()))
                .setTotalTimeout(Duration.millis(retryDef.getTotalTimeoutMillis()))
                .build();
        builder.put(retryDef.getName(), settings);
      } catch (IllegalStateException | NullPointerException e) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "error while creating retry params: %s (in interface %s)",
                e,
                interfaceConfigProto.getName()));
      }
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  private static ImmutableMap<String, MethodConfig> createMethodConfigMap(
      DiagCollector diagCollector,
      InterfaceConfigProto interfaceConfigProto,
      Interface iface,
      ImmutableSet<String> retryCodesConfigNames,
      ImmutableSet<String> retryParamsConfigNames) {
    ImmutableMap.Builder<String, MethodConfig> methodConfigMapBuilder = ImmutableMap.builder();

    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      Method method = iface.lookupMethod(methodConfigProto.getName());
      if (method == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL, "method not found: %s", methodConfigProto.getName()));
        continue;
      }
      MethodConfig methodConfig =
          MethodConfig.createMethodConfig(
              diagCollector,
              methodConfigProto,
              method,
              retryCodesConfigNames,
              retryParamsConfigNames);
      if (methodConfig == null) {
        continue;
      }
      methodConfigMapBuilder.put(methodConfigProto.getName(), methodConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return methodConfigMapBuilder.build();
    }
  }

  private InterfaceConfig(
      ImmutableMap<String, CollectionConfig> collectionConfigs,
      ImmutableMap<String, MethodConfig> methodConfigMap,
      ImmutableMap<String, ImmutableSet<Status.Code>> retryCodesDefinition,
      ImmutableMap<String, RetrySettings> retryParamsDefinition) {
    this.collectionConfigs = collectionConfigs;
    this.methodConfigMap = methodConfigMap;
    this.retryCodesDefinition = retryCodesDefinition;
    this.retrySettingsDefinition = retryParamsDefinition;
  }

  public CollectionConfig getCollectionConfig(String entityName) {
    return collectionConfigs.get(entityName);
  }

  /**
   * Returns the list of CollectionConfigs.
   */
  public Collection<CollectionConfig> getCollectionConfigs() {
    return collectionConfigs.values();
  }

  /**
   * Returns the MethodConfig for the given method.
   */
  public MethodConfig getMethodConfig(Method method) {
    MethodConfig methodConfig = methodConfigMap.get(method.getSimpleName());
    if (methodConfig == null) {
      throw new IllegalArgumentException(
          "no method config for method '" + method.getFullName() + "'");
    }
    return methodConfig;
  }

  public ImmutableMap<String, ImmutableSet<Status.Code>> getRetryCodesDefinition() {
    return retryCodesDefinition;
  }

  public ImmutableMap<String, RetrySettings> getRetrySettingsDefinition() {
    return retrySettingsDefinition;
  }
}
