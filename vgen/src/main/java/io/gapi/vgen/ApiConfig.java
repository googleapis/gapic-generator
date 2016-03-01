package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.SymbolTable;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * ApiConfig represents the code-gen config for an API library.
 */
public class ApiConfig {
  private final Map<String, InterfaceConfig> interfaceConfigMap;
  private final String packageName;

  /**
   * Creates an instance of ApiConfig based on ConfigProto, linking up
   * API interface configurations with specified interfaces in interfaceConfigMap.
   * On errors, null will be returned, and diagnostics are reported to
   * the model.
   */
  @Nullable public static ApiConfig createApiConfig(Model model, ConfigProto configProto) {
    Map<String, InterfaceConfig> interfaceConfigMap = createInterfaceConfigMap(
        model, configProto, model.getSymbolTable());
    if (interfaceConfigMap == null) {
      return null;
    } else {
      return new ApiConfig(interfaceConfigMap, getPackageName(configProto));
    }
  }

  private static String getPackageName(ConfigProto configProto) {
    Map<String, LanguageSettingsProto> settingsMap =
        configProto.getLanguageSettings();
    String language = configProto.getLanguage();
    if (settingsMap.containsKey(language)) {
      return settingsMap.get(language).getPackageName();
    } else {
      return null;
    }
  }

  private static Map<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector, ConfigProto configProto, SymbolTable symbolTable) {
    Map<String, InterfaceConfig> interfaceConfigMap = new HashMap<>();
    for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
      Interface iface = symbolTable.lookupInterface(interfaceConfigProto.getName());
      if (iface == null || !iface.isReachable()) {
        diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL,
            "interface not found: %s", interfaceConfigProto.getName()));
        continue;
      }
      InterfaceConfig interfaceConfig =
          InterfaceConfig.createInterfaceConfig(diagCollector, interfaceConfigProto, iface);
      if (interfaceConfig == null) {
        continue;
      }
      interfaceConfigMap.put(interfaceConfigProto.getName(), interfaceConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return interfaceConfigMap;
    }
  }

  private ApiConfig(Map<String, InterfaceConfig> interfaceConfigMap, String packageName) {
    this.interfaceConfigMap = interfaceConfigMap;
    this.packageName = packageName;
  }

  /**
   * Returns the InterfaceConfig for the given API interface.
   */
  public InterfaceConfig getInterfaceConfig(Interface iface) {
    InterfaceConfig interfaceConfig = interfaceConfigMap.get(iface.getFullName());
    if (interfaceConfig == null) {
      throw new IllegalArgumentException("no interface config for interface '"
          + iface.getFullName() + "'");
    }
    return interfaceConfig;
  }

  /**
   * Returns the package name.
   */
  public String getPackageName() {
    return packageName;
  }

}
