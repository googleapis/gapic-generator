package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.common.collect.ImmutableList;

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
    Map<String, MethodConfig> methodConfigMap = createMethodConfigMap(
        diagCollector, interfaceConfigProto, iface);
    if (collectionConfigs == null || methodConfigMap == null) {
      return null;
    } else {
      return new InterfaceConfig(collectionConfigs, methodConfigMap);
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

  private static Map<String, MethodConfig> createMethodConfigMap(
      DiagCollector diagCollector, InterfaceConfigProto interfaceConfigProto, Interface iface) {
    Map<String, MethodConfig> methodConfigMap = new HashMap<>();

    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      Method method = iface.lookupMethod(methodConfigProto.getName());
      if (method == null) {
        diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "method not found: %s", methodConfigProto.getName()));
        continue;
      }
      MethodConfig methodConfig =
          MethodConfig.createMethodConfig(diagCollector, methodConfigProto, method);
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
      Map<String, MethodConfig> methodConfigMap) {
    this.collectionConfigs = collectionConfigs;
    this.methodConfigMap = methodConfigMap;
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
}
