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

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.FixedResourceNameValueProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.LicenseHeaderProto;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.SymbolTable;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashMap;
import javax.annotation.Nullable;

/** ApiConfig represents the code-gen config for an API library. */
@AutoValue
public abstract class ApiConfig {
  abstract ImmutableMap<String, InterfaceConfig> getInterfaceConfigMap();

  /** Returns the package name. */
  public abstract String getPackageName();

  /** Returns the location of the domain layer, if any. */
  public abstract String getDomainLayerLocation();

  /** Returns the resource name messages configuration. If none was specified, returns null. */
  @Nullable
  public abstract ResourceNameMessageConfigs getResourceNameMessageConfigs();

  /** Returns the lines from the configured copyright file. */
  public abstract ImmutableList<String> getCopyrightLines();

  /** Returns the lines from the configured license file. */
  public abstract ImmutableList<String> getLicenseLines();

  /** Returns a map from entity names to resource name configs. */
  public abstract ImmutableMap<String, ResourceNameConfig> getResourceNameConfigs();

  /**
   * Creates an instance of ApiConfig based on ConfigProto, linking up API interface configurations
   * with specified interfaces in interfaceConfigMap. On errors, null will be returned, and
   * diagnostics are reported to the model.
   */
  @Nullable
  public static ApiConfig createApiConfig(Model model, ConfigProto configProto) {
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            model.getDiagCollector(), configProto);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        createResourceNameConfigs(model.getDiagCollector(), configProto);

    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createInterfaceConfigMap(
            model.getDiagCollector(),
            configProto,
            messageConfigs,
            resourceNameConfigs,
            model.getSymbolTable());

    LanguageSettingsProto settings =
        configProto.getLanguageSettings().get(configProto.getLanguage());
    if (settings == null) {
      settings = LanguageSettingsProto.getDefaultInstance();
    }

    ImmutableList<String> copyrightLines = null;
    ImmutableList<String> licenseLines = null;
    try {
      copyrightLines = loadCopyrightLines(model.getDiagCollector(), configProto.getLicenseHeader());
      licenseLines = loadLicenseLines(model.getDiagCollector(), configProto.getLicenseHeader());
    } catch (Exception e) {
      model
          .getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    if (interfaceConfigMap == null || copyrightLines == null || licenseLines == null) {
      return null;
    } else {
      return new AutoValue_ApiConfig(
          interfaceConfigMap,
          settings.getPackageName(),
          settings.getDomainLayerLocation(),
          messageConfigs,
          copyrightLines,
          licenseLines,
          resourceNameConfigs);
    }
  }

  /** Creates an ApiConfig with no content. Exposed for testing. */
  @VisibleForTesting
  public static ApiConfig createDummyApiConfig() {
    return createDummyApiConfig(ImmutableMap.<String, InterfaceConfig>of(), "", "", null);
  }

  /** Creates an ApiConfig with fixed content. Exposed for testing. */
  @VisibleForTesting
  public static ApiConfig createDummyApiConfig(
      ImmutableMap<String, InterfaceConfig> interfaceConfigMap,
      String packageName,
      String domainLayerLocation,
      ResourceNameMessageConfigs messageConfigs) {
    return new AutoValue_ApiConfig(
        interfaceConfigMap,
        packageName,
        domainLayerLocation,
        messageConfigs,
        ImmutableList.<String>of(),
        ImmutableList.<String>of(),
        ImmutableMap.<String, ResourceNameConfig>of());
  }

  private static ImmutableMap<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector,
      ConfigProto configProto,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      SymbolTable symbolTable) {
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
          InterfaceConfig.createInterfaceConfig(
              diagCollector,
              configProto.getLanguage(),
              interfaceConfigProto,
              iface,
              messageConfigs,
              resourceNameConfigs);
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

  private static ImmutableList<String> loadCopyrightLines(
      DiagCollector diagCollector, LicenseHeaderProto licenseHeaderProto) throws IOException {
    if (licenseHeaderProto == null) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, "license_header missing"));
      return null;
    }
    if (Strings.isNullOrEmpty(licenseHeaderProto.getCopyrightFile())) {
      diagCollector.addDiag(
          Diag.error(SimpleLocation.TOPLEVEL, "license_header.copyright_file missing"));
      return null;
    }

    return getResourceLines(licenseHeaderProto.getCopyrightFile());
  }

  private static ImmutableList<String> loadLicenseLines(
      DiagCollector diagCollector, LicenseHeaderProto licenseHeaderProto) throws IOException {
    if (licenseHeaderProto == null) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, "license_header missing"));
      return null;
    }
    if (Strings.isNullOrEmpty(licenseHeaderProto.getLicenseFile())) {
      diagCollector.addDiag(
          Diag.error(SimpleLocation.TOPLEVEL, "license_header.license_file missing"));
      return null;
    }

    return getResourceLines(licenseHeaderProto.getLicenseFile());
  }

  private static ImmutableList<String> getResourceLines(String resourceFileName)
      throws IOException {
    InputStream fileStream = ConfigProto.class.getResourceAsStream(resourceFileName);
    InputStreamReader fileReader = new InputStreamReader(fileStream, Charsets.UTF_8);
    return ImmutableList.copyOf(CharStreams.readLines(fileReader));
  }

  private static ImmutableMap<String, ResourceNameConfig> createResourceNameConfigs(
      DiagCollector diagCollector, ConfigProto configProto) {
    ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs =
        createSingleResourceNameConfigs(diagCollector, configProto);
    ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs =
        createFixedResourceNameConfigs(diagCollector, configProto.getFixedResourceNameValuesList());
    ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigs =
        createResourceNameOneofConfigs(
            diagCollector,
            configProto.getCollectionOneofsList(),
            singleResourceNameConfigs,
            fixedResourceNameConfigs);

    ImmutableMap.Builder<String, ResourceNameConfig> resourceCollectionMap = ImmutableMap.builder();
    resourceCollectionMap.putAll(singleResourceNameConfigs);
    resourceCollectionMap.putAll(resourceNameOneofConfigs);
    resourceCollectionMap.putAll(fixedResourceNameConfigs);
    return resourceCollectionMap.build();
  }

  private static ImmutableMap<String, SingleResourceNameConfig> createSingleResourceNameConfigs(
      DiagCollector diagCollector, ConfigProto configProto) {
    LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap =
        new LinkedHashMap<>();
    for (CollectionConfigProto collectionConfigProto : configProto.getCollectionsList()) {
      createSingleResourceNameConfig(
          diagCollector, collectionConfigProto, singleResourceNameConfigsMap);
    }
    for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
      for (CollectionConfigProto collectionConfigProto :
          interfaceConfigProto.getCollectionsList()) {
        createSingleResourceNameConfig(
            diagCollector, collectionConfigProto, singleResourceNameConfigsMap);
      }
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return ImmutableMap.copyOf(singleResourceNameConfigsMap);
    }
  }

  private static void createSingleResourceNameConfig(
      DiagCollector diagCollector,
      CollectionConfigProto collectionConfigProto,
      LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap) {
    SingleResourceNameConfig singleResourceNameConfig =
        SingleResourceNameConfig.createSingleResourceName(diagCollector, collectionConfigProto);
    if (singleResourceNameConfig == null) {
      return;
    }
    if (singleResourceNameConfigsMap.containsKey(singleResourceNameConfig.getEntityName())) {
      SingleResourceNameConfig otherConfig =
          singleResourceNameConfigsMap.get(singleResourceNameConfig.getEntityName());
      if (!singleResourceNameConfig.getNamePattern().equals(otherConfig.getNamePattern())) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Inconsistent collection configs across interfaces. Entity name: "
                    + singleResourceNameConfig.getEntityName()));
      }
    } else {
      singleResourceNameConfigsMap.put(
          singleResourceNameConfig.getEntityName(), singleResourceNameConfig);
    }
  }

  private static ImmutableMap<String, FixedResourceNameConfig> createFixedResourceNameConfigs(
      DiagCollector diagCollector, Iterable<FixedResourceNameValueProto> fixedConfigProtos) {
    ImmutableMap.Builder<String, FixedResourceNameConfig> fixedConfigBuilder =
        ImmutableMap.builder();
    for (FixedResourceNameValueProto fixedConfigProto : fixedConfigProtos) {
      FixedResourceNameConfig fixedConfig =
          FixedResourceNameConfig.createFixedResourceNameConfig(diagCollector, fixedConfigProto);
      if (fixedConfig == null) {
        continue;
      }
      fixedConfigBuilder.put(fixedConfig.getEntityName(), fixedConfig);
    }
    return fixedConfigBuilder.build();
  }

  private static ImmutableMap<String, ResourceNameOneofConfig> createResourceNameOneofConfigs(
      DiagCollector diagCollector,
      Iterable<CollectionOneofProto> oneofConfigProtos,
      ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs,
      ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs) {
    ImmutableMap.Builder<String, ResourceNameOneofConfig> oneofConfigBuilder =
        ImmutableMap.builder();
    for (CollectionOneofProto oneofProto : oneofConfigProtos) {
      ResourceNameOneofConfig oneofConfig =
          ResourceNameOneofConfig.createResourceNameOneof(
              diagCollector, oneofProto, singleResourceNameConfigs, fixedResourceNameConfigs);
      if (oneofConfig == null) {
        continue;
      }
      oneofConfigBuilder.put(oneofConfig.getEntityName(), oneofConfig);
    }
    return oneofConfigBuilder.build();
  }

  /** Returns the InterfaceConfig for the given API interface. */
  public InterfaceConfig getInterfaceConfig(Interface iface) {
    return getInterfaceConfigMap().get(iface.getFullName());
  }

  public Iterable<SingleResourceNameConfig> getSingleResourceNameConfigs() {
    return Iterables.filter(getResourceNameConfigs().values(), SingleResourceNameConfig.class);
  }

  public SingleResourceNameConfig getSingleResourceNameConfig(String entityName) {
    ResourceNameConfig resourceNameConfig = getResourceNameConfigs().get(entityName);
    if (resourceNameConfig != null && resourceNameConfig instanceof SingleResourceNameConfig) {
      return (SingleResourceNameConfig) resourceNameConfig;
    }
    return null;
  }
}
