package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * ApiConfig represents the code-gen config for an api, and includes the
 * configuration for methods and resource names.
 */
public class ApiConfig {
  private final Map<String, MethodConfig> methodConfigMap;
  private final String packageName;

  /**
   * Creates an instance of ApiConfig based on ConfigProto, linking up
   * method configuration with the methods specified in methodMap.
   * On errors, null will be returned, and diagnostics are reported to
   * the model.
   */
  @Nullable public static ApiConfig create(Model model,
      ConfigProto configProto) {
    Map<String, Method> methodMap = new HashMap<>();
    for (Interface iface : model.getSymbolTable().getInterfaces()) {
      if (!iface.isReachable()) {
        continue;
      }
      for (Method method : iface.getMethods()) {
        methodMap.put(method.getFullName(), method);
      }
    }

    Map<String, MethodConfig> methodConfigMap = createMethodConfigMap(
        model, configProto, methodMap);
    if (methodConfigMap == null) {
      return null;
    } else {
      return new ApiConfig(methodConfigMap, configProto.getPackageName());
    }
  }

  private static Map<String, MethodConfig> createMethodConfigMap(
      DiagCollector diagCollector, ConfigProto configProto,
      Map<String, Method> methodMap) {
    Map<String, MethodConfig> methodConfigMap = new HashMap<>();

    for (MethodConfigProto methodConfigProto : configProto.getMethodsList()) {
      Method method = methodMap.get(methodConfigProto.getName());
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

  private ApiConfig(Map<String, MethodConfig> methodConfigMap, String packageName) {
    this.methodConfigMap = methodConfigMap;
    this.packageName = packageName;
  }

  /**
   * Returns the MethodConfig for the given method.
   */
  public MethodConfig getMethodConfig(Method method) {
    MethodConfig methodConfig = methodConfigMap.get(method.getFullName());
    if (methodConfig == null) {
      throw new IllegalArgumentException("no method config for method '"
          + method.getFullName() + "'");
    }
    return methodConfig;
  }

  /**
   * Returns the package name.
   */
  public String getPackageName() {
    return packageName;
  }

}
