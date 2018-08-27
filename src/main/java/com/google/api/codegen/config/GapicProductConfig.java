/* Copyright 2016 Google LLC
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

import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.FixedResourceNameValueProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.LicenseHeaderProto;
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.util.ProtoAnnotations;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
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
import com.google.protobuf.DescriptorProtos;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
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

  /**
   * Returns the version of config schema.
   *
   * <p>TODO(eoogbe): Validate the value in GAPIC config advisor.
   */
  public abstract String getConfigSchemaVersion();

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
        getDefaultResourceNameFieldConfigMap(),
        getConfigSchemaVersion());
  }

  /**
   * Creates an instance of GapicProductConfig based on ConfigProto, linking up API interface
   * configurations with specified interfaces in interfaceConfigMap. On errors, null will be
   * returned, and diagnostics are reported to the model.
   *
   * @param configProto The parsed set of config files from input
   * @param protoPackage The source proto package, as opposed to imported protos, that we will
   *     generate clients for.
   */
  @Nullable
  public static GapicProductConfig create(
      Model model,
      @Nullable ConfigProto configProto,
      @Nullable String protoPackage,
      TargetLanguage language) {

    ProtoFile file;
    String defaultPackage;

    if (protoPackage != null) {
      // Default to using --package option for value of default package and first API protoFile.
      defaultPackage = protoPackage;
      file =
          model
              .getFiles()
              .stream()
              .filter(f -> f.getProto().getPackage().equals(defaultPackage))
              .findFirst()
              .orElseThrow(
                  () ->
                      new NoSuchElementException(
                          String.format(
                              "No proto package %s in input descriptor set.", defaultPackage)));
    } else {
      // Otherwise use configProto to get the proto file containing the first interface listed in
      // the config proto, and use it as
      // the assigned file for generated resource names, and to get the default message namespace
      file =
          model.getSymbolTable().lookupInterface(configProto.getInterfaces(0).getName()).getFile();
      defaultPackage = file.getProto().getPackage();
    }

    // Get list of fields from proto
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            model, configProto, defaultPackage);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        createResourceNameConfigs(
            model.getDiagReporter().getDiagCollector(), configProto, file, language);

    TransportProtocol transportProtocol = TransportProtocol.GRPC;

    LanguageSettingsProto settings = null;
    if (configProto != null) {
      settings = configProto.getLanguageSettingsMap().get(language.toString().toLowerCase());
    }
    if (settings == null) {
      settings = LanguageSettingsProto.getDefaultInstance();
    }

    List<ProtoFile> sourceProtos =
        model
            .getFiles()
            .stream()
            .filter(f -> f.getProto().getPackage().equals(defaultPackage))
            .collect(Collectors.toList());
    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createInterfaceConfigMap(
            model.getDiagReporter().getDiagCollector(),
            configProto,
            sourceProtos,
            settings,
            messageConfigs,
            resourceNameConfigs,
            model.getSymbolTable(),
            language);

    ImmutableList<String> copyrightLines = null;
    ImmutableList<String> licenseLines = null;
    try {
      LicenseHeaderProto licenseHeader =
          configProto
              .getLicenseHeader()
              .toBuilder()
              .mergeFrom(settings.getLicenseHeaderOverride())
              .build();
      copyrightLines =
          loadCopyrightLines(model.getDiagReporter().getDiagCollector(), licenseHeader);
      licenseLines = loadLicenseLines(model.getDiagReporter().getDiagCollector(), licenseHeader);
    } catch (Exception e) {
      model
          .getDiagReporter()
          .getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    String configSchemaVersion = configProto.getConfigSchemaVersion();
    // TODO(eoogbe): Move the validation logic to GAPIC config advisor.
    if (Strings.isNullOrEmpty(configSchemaVersion)) {
      model
          .getDiagReporter()
          .getDiagCollector()
          .addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "config_schema_version field is required in GAPIC yaml."));
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
        createResponseFieldConfigMap(messageConfigs, resourceNameConfigs),
        configSchemaVersion);
  }

  public static GapicProductConfig create(
      DiscoApiModel model, ConfigProto configProto, TargetLanguage language) {
    String defaultPackage =
        configProto
            .getLanguageSettingsMap()
            .get(language.toString().toLowerCase())
            .getPackageName();

    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            model, configProto, defaultPackage);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        createResourceNameConfigs(model.getDiagCollector(), configProto, null, language);

    TransportProtocol transportProtocol = TransportProtocol.HTTP;

    LanguageSettingsProto settings =
        configProto.getLanguageSettingsMap().get(language.toString().toLowerCase());
    if (settings == null) {
      settings = LanguageSettingsProto.getDefaultInstance();
    }

    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createDiscoGapicInterfaceConfigMap(
            model, configProto, settings, messageConfigs, resourceNameConfigs, language);

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
      model
          .getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    String configSchemaVersion = configProto.getConfigSchemaVersion();
    // TODO(eoogbe): Move the validation logic to GAPIC config advisor.
    if (Strings.isNullOrEmpty(configSchemaVersion)) {
      model
          .getDiagCollector()
          .addDiag(
              Diag.error(
                  SimpleLocation.TOPLEVEL,
                  "config_schema_version field is required in GAPIC yaml."));
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
        createResponseFieldConfigMap(messageConfigs, resourceNameConfigs),
        configSchemaVersion);
  }

  /** Creates an GapicProductConfig with no content. Exposed for testing. */
  @VisibleForTesting
  public static GapicProductConfig createDummyInstance() {
    return createDummyInstance(ImmutableMap.<String, InterfaceConfig>of(), "", "", null, "1.0.0");
  }

  /** Creates an GapicProductConfig with fixed content. Exposed for testing. */
  @VisibleForTesting
  public static GapicProductConfig createDummyInstance(
      ImmutableMap<String, InterfaceConfig> interfaceConfigMap,
      String packageName,
      String domainLayerLocation,
      ResourceNameMessageConfigs messageConfigs) {
    return createDummyInstance(
        interfaceConfigMap, packageName, domainLayerLocation, messageConfigs, "1.0.0");
  }

  /** Creates an GapicProductConfig with fixed content. Exposed for testing. */
  @VisibleForTesting
  public static GapicProductConfig createDummyInstance(
      ImmutableMap<String, InterfaceConfig> interfaceConfigMap,
      String packageName,
      String domainLayerLocation,
      ResourceNameMessageConfigs messageConfigs,
      String configSchemaVersion) {
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
        createResponseFieldConfigMap(messageConfigs, ImmutableMap.<String, ResourceNameConfig>of()),
        configSchemaVersion);
  }

  private static ImmutableMap<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector,
      @Nullable ConfigProto configProto,
      List<ProtoFile> sourceProtos,
      LanguageSettingsProto languageSettings,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      SymbolTable symbolTable,
      TargetLanguage language) {
    ImmutableMap.Builder<String, InterfaceConfig> interfaceConfigMap = ImmutableMap.builder();

    if (configProto != null) {
      // Parse config for interfaces.
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
            languageSettings.getInterfaceNamesMap().get(interfaceConfigProto.getName());

        GapicInterfaceConfig interfaceConfig =
            GapicInterfaceConfig.createInterfaceConfig(
                diagCollector,
                language,
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
    }
    // Parse proto file for interfaces.
    for (ProtoFile file : sourceProtos) {
      if (file.getProto().getServiceList().size() == 0) continue;
      for (DescriptorProtos.ServiceDescriptorProto service : file.getProto().getServiceList()) {
        String serviceFullName =
            String.format("%s.%s", file.getProto().getPackage(), service.getName());
        Interface apiInterface = symbolTable.lookupInterface(serviceFullName);
        if (apiInterface == null || !apiInterface.isReachable()) {
          diagCollector.addDiag(
              Diag.error(SimpleLocation.TOPLEVEL, "interface not found: %s", service.getName()));
          continue;
        }
        String interfaceNameOverride =
            languageSettings.getInterfaceNamesMap().get(service.getName());

        GapicInterfaceConfig interfaceConfig =
            GapicInterfaceConfig.createInterfaceConfig(
                diagCollector,
                language,
                null,
                apiInterface,
                interfaceNameOverride,
                messageConfigs,
                resourceNameConfigs);
        if (interfaceConfig == null) {
          continue;
        }
        interfaceConfigMap.put(service.getName(), interfaceConfig);
      }
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      return interfaceConfigMap.build();
    }
  }

  private static ImmutableMap<String, InterfaceConfig> createDiscoGapicInterfaceConfigMap(
      DiscoApiModel model,
      ConfigProto configProto,
      LanguageSettingsProto languageSettings,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      TargetLanguage language) {
    ImmutableMap.Builder<String, InterfaceConfig> interfaceConfigMap = ImmutableMap.builder();
    for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
      String interfaceNameOverride =
          languageSettings.getInterfaceNamesMap().get(interfaceConfigProto.getName());

      DiscoGapicInterfaceConfig interfaceConfig =
          DiscoGapicInterfaceConfig.createInterfaceConfig(
              model,
              language,
              interfaceConfigProto,
              interfaceNameOverride,
              messageConfigs,
              resourceNameConfigs);
      if (interfaceConfig == null) {
        continue;
      }
      interfaceConfigMap.put(interfaceConfigProto.getName(), interfaceConfig);
    }

    if (model.getDiagCollector().getErrorCount() > 0) {
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
      DiagCollector diagCollector,
      ConfigProto configProto,
      ProtoFile file,
      TargetLanguage language) {
    ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs =
        createSingleResourceNameConfigs(diagCollector, configProto, file, language);
    ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs =
        createFixedResourceNameConfigs(diagCollector, configProto, file);
    ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigs =
        createResourceNameOneofConfigs(
            diagCollector, configProto, singleResourceNameConfigs, fixedResourceNameConfigs, file);

    ImmutableMap.Builder<String, ResourceNameConfig> resourceCollectionMap = ImmutableMap.builder();
    resourceCollectionMap.putAll(singleResourceNameConfigs);
    resourceCollectionMap.putAll(resourceNameOneofConfigs);
    resourceCollectionMap.putAll(fixedResourceNameConfigs);
    return resourceCollectionMap.build();
  }

  private static ImmutableMap<String, SingleResourceNameConfig> createSingleResourceNameConfigs(
      DiagCollector diagCollector,
      @Nullable ConfigProto configProto,
      ProtoFile file,
      TargetLanguage language) {
    LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap =
        new LinkedHashMap<>();

    if (configProto != null) {
      // Use configProto to make ResourceNameConfigs.
      for (CollectionConfigProto collectionConfigProto : configProto.getCollectionsList()) {
        createSingleResourceNameConfig(
            diagCollector, collectionConfigProto, singleResourceNameConfigsMap, file, language);
      }
      for (InterfaceConfigProto interfaceConfigProto : configProto.getInterfacesList()) {
        for (CollectionConfigProto collectionConfigProto :
            interfaceConfigProto.getCollectionsList()) {
          createSingleResourceNameConfig(
              diagCollector, collectionConfigProto, singleResourceNameConfigsMap, file, language);
        }
      }
    }
    // Add more ResourceNameConfigs from proto annotations. Overwrite the configs from configProto
    // if any clash.
    if (file != null) {
      for (MessageType messageType : file.getMessages()) {
        for (Field field : messageType.getFields()) {
          String resourcePath = ProtoAnnotations.getResourcePath(field);
          if (resourcePath != null) {
            createSingleResourceNameConfig(
                diagCollector, field, singleResourceNameConfigsMap, file);
          }
        }
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
      ProtoFile file,
      TargetLanguage language) {
    SingleResourceNameConfig singleResourceNameConfig =
        SingleResourceNameConfig.createSingleResourceName(
            diagCollector, collectionConfigProto, file, language);
    if (singleResourceNameConfig == null) {
      return;
    }
    if (singleResourceNameConfigsMap.containsKey(singleResourceNameConfig.getEntityId())) {
      SingleResourceNameConfig otherConfig =
          singleResourceNameConfigsMap.get(singleResourceNameConfig.getEntityId());
      if (!singleResourceNameConfig.getNamePattern().equals(otherConfig.getNamePattern())) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Inconsistent collection configs across interfaces. Entity name: "
                    + singleResourceNameConfig.getEntityId()));
      }
    } else {
      singleResourceNameConfigsMap.put(
          singleResourceNameConfig.getEntityId(), singleResourceNameConfig);
    }
  }

  private static void createSingleResourceNameConfig(
      DiagCollector diagCollector,
      Field field,
      LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap,
      ProtoFile file) {
    SingleResourceNameConfig singleResourceNameConfig =
        SingleResourceNameConfig.createSingleResourceName(diagCollector, field, file);
    if (singleResourceNameConfig == null) {
      return;
    }
    if (singleResourceNameConfigsMap.containsKey(singleResourceNameConfig.getEntityId())) {
      SingleResourceNameConfig otherConfig =
          singleResourceNameConfigsMap.get(singleResourceNameConfig.getEntityId());
      if (!singleResourceNameConfig.getNamePattern().equals(otherConfig.getNamePattern())) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Inconsistent collection configs across interfaces. Entity name: "
                    + singleResourceNameConfig.getEntityId()));
      }
    } else {
      singleResourceNameConfigsMap.put(
          singleResourceNameConfig.getEntityId(), singleResourceNameConfig);
    }
  }

  private static ImmutableMap<String, FixedResourceNameConfig> createFixedResourceNameConfigs(
      DiagCollector diagCollector, ConfigProto configProto, ProtoFile file) {
    ImmutableMap.Builder<String, FixedResourceNameConfig> fixedConfigBuilder =
        ImmutableMap.builder();
    if (configProto != null) {
      Iterable<FixedResourceNameValueProto> fixedConfigProtos =
          configProto.getFixedResourceNameValuesList();
      for (FixedResourceNameValueProto fixedConfigProto : fixedConfigProtos) {
        FixedResourceNameConfig fixedConfig =
            FixedResourceNameConfig.createFixedResourceNameConfig(
                diagCollector, fixedConfigProto, file);
        if (fixedConfig == null) {
          continue;
        }
        fixedConfigBuilder.put(fixedConfig.getEntityId(), fixedConfig);
      }
    }
    return fixedConfigBuilder.build();
  }

  private static ImmutableMap<String, ResourceNameOneofConfig> createResourceNameOneofConfigs(
      DiagCollector diagCollector,
      ConfigProto configProto,
      ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs,
      ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs,
      ProtoFile file) {
    ImmutableMap.Builder<String, ResourceNameOneofConfig> oneofConfigBuilder =
        ImmutableMap.builder();
    if (configProto != null) {
      Iterable<CollectionOneofProto> oneofConfigProtos = configProto.getCollectionOneofsList();
      for (CollectionOneofProto oneofProto : oneofConfigProtos) {
        ResourceNameOneofConfig oneofConfig =
            ResourceNameOneofConfig.createResourceNameOneof(
                diagCollector,
                oneofProto,
                singleResourceNameConfigs,
                fixedResourceNameConfigs,
                file);
        if (oneofConfig == null) {
          continue;
        }
        oneofConfigBuilder.put(oneofConfig.getEntityName(), oneofConfig);
      }
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
  public InterfaceConfig getInterfaceConfig(InterfaceModel apiInterface) {
    return getInterfaceConfigMap().get(apiInterface.getFullName());
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
