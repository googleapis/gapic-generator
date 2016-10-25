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
import com.google.common.io.CharStreams;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
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

  public abstract ImmutableMap<String, CollectionConfig> collectionConfigs();

  public abstract ImmutableMap<String, CollectionOneofConfig> collectionOneofConfigs();

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
    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createInterfaceConfigMap(
            model.getDiagCollector(), configProto, messageConfigs, model.getSymbolTable());
    LanguageSettingsProto settings =
        configProto.getLanguageSettings().get(configProto.getLanguage());
    ImmutableMap<String, CollectionConfig> collectionConfigs =
        createCollectionConfigs(model.getDiagCollector(), configProto.getInterfacesList());
    ImmutableMap<String, CollectionOneofConfig> collectionOneofConfigs =
        createCollectionOneofConfigs(
            model.getDiagCollector(), configProto.getCollectionOneofsList(), collectionConfigs);
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
          collectionConfigs,
          collectionOneofConfigs);
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
        ImmutableMap.<String, CollectionConfig>of(),
        ImmutableMap.<String, CollectionOneofConfig>of());
  }

  private static ImmutableMap<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector,
      ConfigProto configProto,
      ResourceNameMessageConfigs messageConfigs,
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
              messageConfigs);
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

  private static ImmutableMap<String, CollectionConfig> createCollectionConfigs(
      DiagCollector diagCollector, Iterable<InterfaceConfigProto> interfaceConfigProtos) {
    LinkedHashMap<String, CollectionConfig> collectionConfigsMap = new LinkedHashMap<>();
    for (InterfaceConfigProto interfaceConfigProto : interfaceConfigProtos) {
      for (CollectionConfigProto collectionConfigProto :
          interfaceConfigProto.getCollectionsList()) {
        CollectionConfig collectionConfig =
            CollectionConfig.createCollection(diagCollector, collectionConfigProto);
        if (collectionConfig == null) {
          continue;
        }
        if (collectionConfigsMap.containsKey(collectionConfig.getEntityName())) {
          CollectionConfig otherConfig = collectionConfigsMap.get(collectionConfig.getEntityName());
          if (!collectionConfig.getNamePattern().equals(otherConfig.getNamePattern())) {
            throw new IllegalArgumentException(
                "Inconsistent collection configs across interfaces. Entity name: "
                    + collectionConfig.getEntityName());
          }
        } else {
          collectionConfigsMap.put(collectionConfig.getEntityName(), collectionConfig);
        }
      }
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return ImmutableMap.copyOf(collectionConfigsMap);
    }
  }

  private static ImmutableMap<String, CollectionOneofConfig> createCollectionOneofConfigs(
      DiagCollector diagCollector,
      Iterable<CollectionOneofProto> oneofConfigProtos,
      ImmutableMap<String, CollectionConfig> collectionConfigs) {
    ImmutableMap.Builder<String, CollectionOneofConfig> oneofConfigBuilder = ImmutableMap.builder();
    for (CollectionOneofProto oneofProto : oneofConfigProtos) {
      CollectionOneofConfig oneofConfig =
          CollectionOneofConfig.createCollectionOneof(diagCollector, oneofProto, collectionConfigs);
      if (oneofConfig == null) {
        continue;
      }
      oneofConfigBuilder.put(oneofConfig.getOneofName(), oneofConfig);
    }
    return oneofConfigBuilder.build();
  }

  /** Returns the InterfaceConfig for the given API interface. */
  public InterfaceConfig getInterfaceConfig(Interface iface) {
    return getInterfaceConfigMap().get(iface.getFullName());
  }

  public CollectionConfig getCollectionConfig(String entityName) {
    return collectionConfigs().get(entityName);
  }

  /** Returns the list of CollectionConfigs. */
  public Collection<CollectionConfig> getCollectionConfigs() {
    return collectionConfigs().values();
  }
}
