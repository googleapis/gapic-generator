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
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.tools.framework.model.BoundedDiagCollector;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
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
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * GapicProductConfig represents client code-gen config for an API product contained in a
 * {api}_gapic.yaml configuration file.
 */
@AutoValue
public abstract class GapicProductConfig implements ProductConfig {
  public abstract ImmutableMap<String, ? extends InterfaceConfig> getInterfaceConfigMap();

  /** Returns the package name. */
  @Override
  public abstract String getPackageName();

  /** Returns the location of the domain layer, if any. */
  public abstract String getDomainLayerLocation();

  /** Returns the release level, if any. */
  public abstract ReleaseLevel getReleaseLevel();

  /** Returns the resource name messages configuration. If none was specified, returns null. */
  @Nullable
  public abstract ResourceNameMessageConfigs getResourceNameMessageConfigs();

  /** Returns the lines from the configured copyright file. */
  @Override
  public abstract ImmutableList<String> getCopyrightLines();

  /** Returns the lines from the configured license file. */
  @Override
  public abstract ImmutableList<String> getLicenseLines();

  /** Returns a map from entity names to resource name configs. */
  public abstract ImmutableMap<String, ResourceNameConfig> getResourceNameConfigs();

  /** Returns the type of transport for the generated client. Defaults to Grpc. */
  public abstract TransportProtocol getTransportProtocol();

  /**
   * Returns a map from fully qualified field names to FieldConfigs for all fields that have a
   * resource name type specified. This is the default field config for each field, and should be
   * used when not in the context of a particular method or flattening configuration.
   */
  public abstract ImmutableMap<String, FieldConfig> getDefaultResourceNameFieldConfigMap();

  public GapicProductConfig withPackageName(String packageName) {
    return new AutoValue_GapicProductConfig(
        getInterfaceConfigMap(),
        packageName,
        getDomainLayerLocation(),
        getReleaseLevel(),
        getResourceNameMessageConfigs(),
        getCopyrightLines(),
        getLicenseLines(),
        getResourceNameConfigs(),
        getTransportProtocol(),
        getDefaultResourceNameFieldConfigMap());
  }

  /**
   * Creates an instance of GapicProductConfig based on ConfigProto, linking up API interface
   * configurations with specified interfaces in interfaceConfigMap. On errors, null will be
   * returned, and diagnostics are reported to the model.
   */
  @Nullable
  public static GapicProductConfig create(Model model, ConfigProto configProto) {

    // Get the proto file containing the first interface listed in the config proto, and use it as
    // the assigned file for generated resource names, and to get the default message namespace
    ProtoFile file =
        model.getSymbolTable().lookupInterface(configProto.getInterfaces(0).getName()).getFile();
    String defaultPackage = file.getProto().getPackage();

    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            model, configProto, defaultPackage);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        createResourceNameConfigs(model.getDiagCollector(), configProto, file);

    TransportProtocol transportProtocol = TransportProtocol.GRPC;

    LanguageSettingsProto settings =
        configProto.getLanguageSettings().get(configProto.getLanguage());
    if (settings == null) {
      settings = LanguageSettingsProto.getDefaultInstance();
    }

    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createInterfaceConfigMap(
            model.getDiagCollector(),
            configProto,
            settings,
            messageConfigs,
            resourceNameConfigs,
            model.getSymbolTable());

    ImmutableList<String> copyrightLines = null;
    ImmutableList<String> licenseLines = null;
    try {
      LicenseHeaderProto licenseHeader =
          configProto
              .getLicenseHeader()
              .toBuilder()
              .mergeFrom(settings.getLicenseHeaderOverride())
              .build();
      copyrightLines = loadCopyrightLines(model.getDiagCollector(), licenseHeader);
      licenseLines = loadLicenseLines(model.getDiagCollector(), licenseHeader);
    } catch (Exception e) {
      model
          .getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    if (interfaceConfigMap == null || copyrightLines == null || licenseLines == null) {
      return null;
    }
    return new AutoValue_GapicProductConfig(
        interfaceConfigMap,
        settings.getPackageName(),
        settings.getDomainLayerLocation(),
        settings.getReleaseLevel(),
        messageConfigs,
        copyrightLines,
        licenseLines,
        resourceNameConfigs,
        transportProtocol,
        createResponseFieldConfigMap(messageConfigs, resourceNameConfigs));
  }

  public static GapicProductConfig create(
      Document document, ConfigProto configProto, DiscoGapicNamer discoGapicNamer) {
    String defaultPackage =
        configProto.getLanguageSettingsMap().get(configProto.getLanguage()).getPackageName();

    DiagCollector diagCollector = new BoundedDiagCollector();

    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            document, diagCollector, configProto, defaultPackage, discoGapicNamer);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        createResourceNameConfigs(diagCollector, configProto, null);

    TransportProtocol transportProtocol = TransportProtocol.HTTP;

    LanguageSettingsProto settings =
        configProto.getLanguageSettingsMap().get(configProto.getLanguage());
    if (settings == null) {
      settings = LanguageSettingsProto.getDefaultInstance();
    }

    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createDiscoGapicInterfaceConfigMap(
            document,
            diagCollector,
            configProto,
            settings,
            messageConfigs,
            resourceNameConfigs,
            discoGapicNamer);

    ImmutableList<String> copyrightLines;
    ImmutableList<String> licenseLines;
    try {
      LicenseHeaderProto licenseHeader =
          configProto
              .getLicenseHeader()
              .toBuilder()
              .mergeFrom(settings.getLicenseHeaderOverride())
              .build();
      copyrightLines = getResourceLines(licenseHeader.getCopyrightFile());
      licenseLines = getResourceLines(licenseHeader.getLicenseFile());
    } catch (Exception e) {
      diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    return new AutoValue_GapicProductConfig(
        interfaceConfigMap,
        settings.getPackageName(),
        settings.getDomainLayerLocation(),
        settings.getReleaseLevel(),
        messageConfigs,
        copyrightLines,
        licenseLines,
        resourceNameConfigs,
        transportProtocol,
        createResponseFieldConfigMap(messageConfigs, resourceNameConfigs));
  }

  /** Creates an GapicProductConfig with no content. Exposed for testing. */
  @VisibleForTesting
  public static GapicProductConfig createDummyInstance() {
    return createDummyInstance(ImmutableMap.<String, InterfaceConfig>of(), "", "", null);
  }

  /** Creates an GapicProductConfig with fixed content. Exposed for testing. */
  @VisibleForTesting
  public static GapicProductConfig createDummyInstance(
      ImmutableMap<String, InterfaceConfig> interfaceConfigMap,
      String packageName,
      String domainLayerLocation,
      ResourceNameMessageConfigs messageConfigs) {
    return new AutoValue_GapicProductConfig(
        interfaceConfigMap,
        packageName,
        domainLayerLocation,
        ReleaseLevel.UNSET_RELEASE_LEVEL,
        messageConfigs,
        ImmutableList.<String>of(),
        ImmutableList.<String>of(),
        ImmutableMap.<String, ResourceNameConfig>of(),
        // Default to gRPC.
        TransportProtocol.GRPC,
        createResponseFieldConfigMap(
            messageConfigs, ImmutableMap.<String, ResourceNameConfig>of()));
  }

  private static ImmutableMap<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector,
      ConfigProto configProto,
      LanguageSettingsProto languageSettings,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      SymbolTable symbolTable) {
    ImmutableMap.Builder<String, InterfaceConfig> interfaceConfigMap = ImmutableMap.builder();
    for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
      Interface apiInterface = symbolTable.lookupInterface(interfaceConfigProto.getName());
      if (apiInterface == null || !apiInterface.isReachable()) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "interface not found: %s",
                interfaceConfigProto.getName()));
        continue;
      }
      String interfaceNameOverride =
          languageSettings.getInterfaceNames().get(interfaceConfigProto.getName());

      GapicInterfaceConfig interfaceConfig =
          GapicInterfaceConfig.createInterfaceConfig(
              diagCollector,
              configProto.getLanguage(),
              interfaceConfigProto,
              apiInterface,
              interfaceNameOverride,
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

  private static ImmutableMap<String, InterfaceConfig> createDiscoGapicInterfaceConfigMap(
      Document document,
      DiagCollector diagCollector,
      ConfigProto configProto,
      LanguageSettingsProto languageSettings,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      DiscoGapicNamer discoGapicNamer) {
    ImmutableMap.Builder<String, InterfaceConfig> interfaceConfigMap = ImmutableMap.builder();
    for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
      String interfaceNameOverride =
          languageSettings.getInterfaceNames().get(interfaceConfigProto.getName());

      DiscoGapicInterfaceConfig interfaceConfig =
          DiscoGapicInterfaceConfig.createInterfaceConfig(
              document,
              diagCollector,
              configProto.getLanguage(),
              interfaceConfigProto,
              interfaceNameOverride,
              messageConfigs,
              resourceNameConfigs,
              discoGapicNamer);
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
      DiagCollector diagCollector, ConfigProto configProto, ProtoFile file) {
    ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs =
        createSingleResourceNameConfigs(diagCollector, configProto, file);
    ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs =
        createFixedResourceNameConfigs(
            diagCollector, configProto.getFixedResourceNameValuesList(), file);
    ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigs =
        createResourceNameOneofConfigs(
            diagCollector,
            configProto.getCollectionOneofsList(),
            singleResourceNameConfigs,
            fixedResourceNameConfigs,
            file);

    ImmutableMap.Builder<String, ResourceNameConfig> resourceCollectionMap = ImmutableMap.builder();
    resourceCollectionMap.putAll(singleResourceNameConfigs);
    resourceCollectionMap.putAll(resourceNameOneofConfigs);
    resourceCollectionMap.putAll(fixedResourceNameConfigs);
    return resourceCollectionMap.build();
  }

  private static ImmutableMap<String, SingleResourceNameConfig> createSingleResourceNameConfigs(
      DiagCollector diagCollector, ConfigProto configProto, ProtoFile file) {
    LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap =
        new LinkedHashMap<>();
    for (CollectionConfigProto collectionConfigProto : configProto.getCollectionsList()) {
      createSingleResourceNameConfig(
          diagCollector, collectionConfigProto, singleResourceNameConfigsMap, file);
    }
    for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
      for (CollectionConfigProto collectionConfigProto :
          interfaceConfigProto.getCollectionsList()) {
        createSingleResourceNameConfig(
            diagCollector, collectionConfigProto, singleResourceNameConfigsMap, file);
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
      LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap,
      ProtoFile file) {
    SingleResourceNameConfig singleResourceNameConfig =
        SingleResourceNameConfig.createSingleResourceName(
            diagCollector, collectionConfigProto, file);
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
      DiagCollector diagCollector,
      Iterable<FixedResourceNameValueProto> fixedConfigProtos,
      ProtoFile file) {
    ImmutableMap.Builder<String, FixedResourceNameConfig> fixedConfigBuilder =
        ImmutableMap.builder();
    for (FixedResourceNameValueProto fixedConfigProto : fixedConfigProtos) {
      FixedResourceNameConfig fixedConfig =
          FixedResourceNameConfig.createFixedResourceNameConfig(
              diagCollector, fixedConfigProto, file);
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
      ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs,
      ProtoFile file) {
    ImmutableMap.Builder<String, ResourceNameOneofConfig> oneofConfigBuilder =
        ImmutableMap.builder();
    for (CollectionOneofProto oneofProto : oneofConfigProtos) {
      ResourceNameOneofConfig oneofConfig =
          ResourceNameOneofConfig.createResourceNameOneof(
              diagCollector, oneofProto, singleResourceNameConfigs, fixedResourceNameConfigs, file);
      if (oneofConfig == null) {
        continue;
      }
      oneofConfigBuilder.put(oneofConfig.getEntityName(), oneofConfig);
    }
    return oneofConfigBuilder.build();
  }

  private static ImmutableMap<String, FieldConfig> createResponseFieldConfigMap(
      ResourceNameMessageConfigs messageConfig,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs) {

    ImmutableMap.Builder<String, FieldConfig> builder = ImmutableMap.builder();
    if (messageConfig == null) {
      return builder.build();
    }
    Map<String, FieldConfig> map = new HashMap<>();
    for (FieldModel field : messageConfig.getFieldsWithResourceNamesByMessage().values()) {
      map.put(
          field.getFullName(),
          FieldConfig.createMessageFieldConfig(
              messageConfig, resourceNameConfigs, field, ResourceNameTreatment.STATIC_TYPES));
    }
    builder.putAll(map);
    return builder.build();
  }

  /** Returns the GapicInterfaceConfig for the given API interface. */
  public GapicInterfaceConfig getInterfaceConfig(Interface apiInterface) {
    return (GapicInterfaceConfig) getInterfaceConfigMap().get(apiInterface.getFullName());
  }

  /** Returns the GapicInterfaceConfig for the given API interface. */
  @Override
  public GapicInterfaceConfig getInterfaceConfig(InterfaceModel apiInterface) {
    return (GapicInterfaceConfig) getInterfaceConfigMap().get(apiInterface.getFullName());
  }

  /** Returns the GapicInterfaceConfig for the given API method. */
  public InterfaceConfig getInterfaceConfig(String fullName) {
    return getInterfaceConfigMap().get(fullName);
  }

  public Iterable<SingleResourceNameConfig> getSingleResourceNameConfigs() {
    return Iterables.filter(getResourceNameConfigs().values(), SingleResourceNameConfig.class);
  }

  /**
   * Returns a SingleResourceNameConfig object for the given entity name. If the entityName
   * corresponds to a ResourceNameOneofConfig which contains at least one SingleResourceNameConfig,
   * then the first of those SingleResourceNameConfigs is returned. If the entityName is neither a
   * SingleResourceNameConfig or ResourceNameOneofConfig containing a SingleResourceNameConfig, then
   * returns null.
   */
  public SingleResourceNameConfig getSingleResourceNameConfig(String entityName) {
    ResourceNameConfig resourceNameConfig = getResourceNameConfigs().get(entityName);
    if (resourceNameConfig != null && resourceNameConfig instanceof SingleResourceNameConfig) {
      return (SingleResourceNameConfig) resourceNameConfig;
    }
    if (resourceNameConfig != null && resourceNameConfig instanceof ResourceNameOneofConfig) {
      ResourceNameOneofConfig oneofConfig = (ResourceNameOneofConfig) resourceNameConfig;
      if (Iterables.size(oneofConfig.getSingleResourceNameConfigs()) > 0) {
        return Iterables.get(oneofConfig.getSingleResourceNameConfigs(), 0);
      }
    }
    return null;
  }
}
