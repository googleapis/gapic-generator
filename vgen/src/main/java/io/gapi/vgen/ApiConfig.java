package io.gapi.vgen;

import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.SymbolTable;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

import javax.annotation.Nullable;

/**
 * ApiConfig represents the code-gen config for an API library.
 */
@com.google.auto.value.AutoValue
public abstract class ApiConfig {
  abstract ImmutableMap<String, InterfaceConfig> getInterfaceConfigMap();

  /**
   * Returns the package name.
   */
  @Nullable
  public abstract String getPackageName();

  /**
   * Whether or not we should generate code samples.
   */
  public abstract boolean generateSamples();

  /**
   * Creates an instance of ApiConfig based on ConfigProto, linking up
   * API interface configurations with specified interfaces in interfaceConfigMap.
   * On errors, null will be returned, and diagnostics are reported to
   * the model.
   */
  @Nullable
  public static ApiConfig createApiConfig(Model model, ConfigProto configProto) {
    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createInterfaceConfigMap(model, configProto, model.getSymbolTable());
    if (interfaceConfigMap == null) {
      return null;
    } else {
      return new AutoValue_ApiConfig(
          interfaceConfigMap, getPackageName(configProto), configProto.getGenerateSamples());
    }
  }

  private static String getPackageName(ConfigProto configProto) {
    Map<String, LanguageSettingsProto> settingsMap = configProto.getLanguageSettings();
    String language = configProto.getLanguage();
    if (settingsMap.containsKey(language)) {
      return settingsMap.get(language).getPackageName();
    } else {
      return null;
    }
  }

  private static ImmutableMap<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector, ConfigProto configProto, SymbolTable symbolTable) {
    ImmutableMap.Builder<String, InterfaceConfig> interfaceConfigMap =
        ImmutableMap.<String, InterfaceConfig>builder();
    for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
      Interface iface = symbolTable.lookupInterface(interfaceConfigProto.getName());
      if (iface == null || !iface.isReachable()) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "interface not found: %s",
                interfaceConfigProto.getName()));
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
      return interfaceConfigMap.build();
    }
  }

  /**
   * Returns the InterfaceConfig for the given API interface.
   */
  public InterfaceConfig getInterfaceConfig(Interface iface) {
    InterfaceConfig interfaceConfig = getInterfaceConfigMap().get(iface.getFullName());
    if (interfaceConfig == null) {
      throw new IllegalArgumentException(
          "no interface config for interface '" + iface.getFullName() + "'");
    }
    return interfaceConfig;
  }
}
