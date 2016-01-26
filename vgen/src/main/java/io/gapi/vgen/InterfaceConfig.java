package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import io.grpc.Status;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * InterfaceConfig represents the code-gen config for an API interface, and includes the
 * configuration for methods and resource names.
 */
public class InterfaceConfig {
  private final ImmutableList<CollectionConfig> collectionConfigs;
  private final Map<String, MethodConfig> methodConfigMap;
  private final ImmutableMap<String, ImmutableSet<Status.Code>> retryDefinition;

  /**
   * Creates an instance of InterfaceConfig based on ConfigProto, linking up
   * method configurations with specified methods in methodConfigMap.
   * On errors, null will be returned, and diagnostics are reported to
   * the model.
   */
  @Nullable public static InterfaceConfig createInterfaceConfig(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto, Interface iface) {
    ImmutableList<CollectionConfig> collectionConfigs = createCollectionConfigs(
        diagCollector, interfaceConfigProto);

    ImmutableMap<String, ImmutableSet<Status.Code>> retryDefinition =
        createRetryDefinition(diagCollector, interfaceConfigProto);

    Map<String, MethodConfig> methodConfigMap = null;
    if (retryDefinition != null) {
      methodConfigMap =
          createMethodConfigMap(diagCollector, interfaceConfigProto, iface, retryDefinition.keySet());
    }

    if (collectionConfigs == null || methodConfigMap == null || retryDefinition == null) {
      return null;
    } else {
      return new InterfaceConfig(collectionConfigs, methodConfigMap, retryDefinition);
    }
  }

  private static ImmutableList<CollectionConfig> createCollectionConfigs(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableList.Builder<CollectionConfig> collectionConfigsBuilder = ImmutableList.builder();

    for (CollectionConfigProto collectionConfigProto : interfaceConfigProto.getCollectionsList()) {
      CollectionConfig collectionConfig =
          CollectionConfig.createCollection(diagCollector, collectionConfigProto);
      if (collectionConfig == null) {
        continue;
      }
      collectionConfigsBuilder.add(collectionConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return collectionConfigsBuilder.build();
    }
  }

  private static ImmutableMap<String, ImmutableSet<Status.Code>> createRetryDefinition(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto) {
    ImmutableMap.Builder<String, ImmutableSet<Status.Code>> builder =
        ImmutableMap.<String, ImmutableSet<Status.Code>>builder();
    for (RetryConfigDefinitionProto retryDef : interfaceConfigProto.getRetryDefList()) {
      EnumSet<Status.Code> codes = EnumSet.noneOf(Status.Code.class);
      for (String codeText : retryDef.getRetryCodesList()) {
        try {
          codes.add(Status.Code.valueOf(codeText));
        } catch (IllegalArgumentException e) {
          diagCollector.addDiag(
              Diag.error(SimpleLocation.TOPLEVEL, "status code not found %s", codeText));
        }
      }
      builder.put(retryDef.getName(), Sets.immutableEnumSet(codes));
    }
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }
    return builder.build();
  }

  private static Map<String, MethodConfig> createMethodConfigMap(
      DiagCollector diagCollector,
      InterfaceConfigProto interfaceConfigProto,
      Interface iface,
      ImmutableSet<String> retryConfigNames) {
    Map<String, MethodConfig> methodConfigMap = new HashMap<>();

    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      Method method = iface.lookupMethod(methodConfigProto.getName());
      if (method == null) {
        diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "method not found: %s", methodConfigProto.getName()));
        continue;
      }
      MethodConfig methodConfig =
          MethodConfig.createMethodConfig(diagCollector, methodConfigProto, method, retryConfigNames);
      if (methodConfig == null) {
        continue;
      }
      methodConfigMap.put(methodConfigProto.getName(), methodConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return methodConfigMap;
    }
  }

  private InterfaceConfig(ImmutableList<CollectionConfig> collectionConfigs,
      Map<String, MethodConfig> methodConfigMap,
      ImmutableMap<String, ImmutableSet<Status.Code>> retryDefinition) {
    this.collectionConfigs = collectionConfigs;
    this.methodConfigMap = methodConfigMap;
    this.retryDefinition = retryDefinition;
  }

  /**
   * Returns the list of CollectionConfigs.
   */
  public ImmutableList<CollectionConfig> getCollectionConfigs() {
    return collectionConfigs;
  }

  /**
   * Returns the MethodConfig for the given method.
   */
  public MethodConfig getMethodConfig(Method method) {
    MethodConfig methodConfig = methodConfigMap.get(method.getSimpleName());
    if (methodConfig == null) {
      throw new IllegalArgumentException("no method config for method '"
          + method.getFullName() + "'");
    }
    return methodConfig;
  }

  public ImmutableMap<String, ImmutableSet<Status.Code>> getRetryDefinition() {
    return retryDefinition;
  }
}
