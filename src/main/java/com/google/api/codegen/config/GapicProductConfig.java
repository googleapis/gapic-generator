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

import com.google.api.Resource;
import com.google.api.ResourceSet;
import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.FixedResourceNameValueProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.configgen.transformer.LanguageTransformer;
import com.google.api.codegen.util.LicenseHeaderUtil;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.model.SymbolTable;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.protobuf.DescriptorProtos;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;

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

  /** Returns the version of config schema. */
  @Nullable
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

  @Nullable
  public static GapicProductConfig create(
      Model model, ConfigProto configProto, TargetLanguage language) {
    return create(model, configProto, null, language);
  }

  /**
   * Creates an instance of GapicProductConfig based on ConfigProto, linking up API interface
   * configurations with specified interfaces in interfaceConfigMap. On errors, null will be
   * returned, and diagnostics are reported to the model.
   *
   * @param model The protobuf model for which we are creating a config.
   * @param configProto The parsed set of config files from input
   * @param protoPackage The source proto package, as opposed to imported protos, that we will
   *     generate clients for.
   * @param language The language that this config will be used to generate a client in.
   */
  @Nullable
  public static GapicProductConfig create(
      Model model,
      @Nullable ConfigProto configProto,
      @Nullable String protoPackage,
      TargetLanguage language) {

    final String defaultPackage;

    if (protoPackage != null) {
      // Default to using --package option for value of default package and first API protoFile.
      defaultPackage = protoPackage;
    } else if (configProto != null) {
      // Otherwise use configProto to get the proto file containing the first interface listed in
      // the config proto, and use it as
      // the assigned file for generated resource names, and to get the default message namespace.
      ProtoFile file =
          model.getSymbolTable().lookupInterface(configProto.getInterfaces(0).getName()).getFile();
      defaultPackage = file.getProto().getPackage();
    } else {
      throw new NullPointerException("configProto and protoPackage cannot both be null.");
    }

    List<ProtoFile> sourceProtos =
        model
            .getFiles()
            .stream()
            .filter(f -> f.getProto().getPackage().equals(defaultPackage))
            .collect(Collectors.toList());

    if (protoPackage != null && configProto == null) {
      if (sourceProtos.isEmpty()) {
        model
            .getDiagReporter()
            .getDiagCollector()
            .addDiag(
                Diag.error(
                    SimpleLocation.TOPLEVEL,
                    "There are no source proto files with package %s",
                    defaultPackage));
      }
      sourceProtos.forEach(model::addRoot);
    }

    ProtoParser protoParser = new ProtoParser();
    if (configProto == null) {
      configProto = ConfigProto.getDefaultInstance();
    }

    DiagCollector diagCollector = model.getDiagReporter().getDiagCollector();

    Map<Resource, ProtoFile> resourceDefs =
        protoParser.getResourceDefs(sourceProtos, diagCollector);
    Map<ResourceSet, ProtoFile> resourceSetDefs =
        protoParser.getResourceSetDefs(sourceProtos, diagCollector);

    // Get list of fields from proto
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtos, configProto, defaultPackage, resourceDefs, resourceSetDefs, protoParser);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        createResourceNameConfigs(
            diagCollector,
            configProto,
            sourceProtos,
            language,
            resourceDefs,
            resourceSetDefs,
            protoParser);

    if (resourceNameConfigs == null) {
      return null;
    }

    TransportProtocol transportProtocol = TransportProtocol.GRPC;

    String clientPackageName;
    LanguageSettingsProto settings =
        configProto.getLanguageSettingsMap().get(language.toString().toLowerCase());
    if (settings == null) {
      settings = LanguageSettingsProto.getDefaultInstance();
      String basePackageName =
          Optional.ofNullable(protoPackage).orElse(protoParser.getPackageName(model));
      clientPackageName =
          LanguageTransformer.getFormattedPackageName(language.name(), basePackageName);
    } else {
      clientPackageName = settings.getPackageName();
    }

    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createInterfaceConfigMap(
            model.getDiagReporter().getDiagCollector(),
            configProto,
            sourceProtos,
            defaultPackage,
            settings,
            messageConfigs,
            resourceNameConfigs,
            model.getSymbolTable(),
            language,
            protoParser);

    ImmutableList<String> copyrightLines;
    ImmutableList<String> licenseLines;
    String configSchemaVersion = null;

    try {
      LicenseHeaderUtil licenseHeaderUtil =
          LicenseHeaderUtil.create(
              configProto, settings, model.getDiagReporter().getDiagCollector());
      copyrightLines = licenseHeaderUtil.loadCopyrightLines();
      licenseLines = licenseHeaderUtil.loadLicenseLines();
    } catch (Exception e) {
      model
          .getDiagReporter()
          .getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    if (!configProto.equals(ConfigProto.getDefaultInstance())) {
      configSchemaVersion = configProto.getConfigSchemaVersion();
      if (Strings.isNullOrEmpty(configSchemaVersion)) {
        model
            .getDiagReporter()
            .getDiagCollector()
            .addDiag(
                Diag.error(
                    SimpleLocation.TOPLEVEL,
                    "config_schema_version field is required in GAPIC yaml."));
      }
    }

    if (interfaceConfigMap == null || copyrightLines == null || licenseLines == null) {
      return null;
    }
    return new AutoValue_GapicProductConfig(
        interfaceConfigMap,
        clientPackageName,
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
        createResourceNameConfigs(model.getDiagCollector(), configProto, language);

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
      LicenseHeaderUtil licenseHeaderUtil =
          LicenseHeaderUtil.create(configProto, settings, model.getDiagCollector());
      copyrightLines = licenseHeaderUtil.loadCopyrightLines();
      licenseLines = licenseHeaderUtil.loadLicenseLines();
    } catch (Exception e) {
      model
          .getDiagCollector()
          .addDiag(Diag.error(SimpleLocation.TOPLEVEL, "Exception: %s", e.getMessage()));
      e.printStackTrace(System.err);
      throw new RuntimeException(e);
    }

    String configSchemaVersion = configProto.getConfigSchemaVersion();
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
    return createDummyInstance(ImmutableMap.of(), "", "", null, "1.0.0");
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
        ImmutableList.of(),
        ImmutableList.of(),
        ImmutableMap.of(),
        // Default to gRPC.
        TransportProtocol.GRPC,
        createResponseFieldConfigMap(messageConfigs, ImmutableMap.of()),
        configSchemaVersion);
  }

  private static ImmutableMap<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector,
      ConfigProto configProto,
      List<ProtoFile> sourceProtos,
      String defaultPackageName,
      LanguageSettingsProto languageSettings,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      SymbolTable symbolTable,
      TargetLanguage language,
      ProtoParser protoParser) {
    // Return value; maps interface names to their InterfaceConfig.
    ImmutableMap.Builder<String, InterfaceConfig> interfaceConfigMap = ImmutableMap.builder();

    // Maps name of interfaces to found interfaces from proto.
    Map<String, Interface> protoInterfaces = new LinkedHashMap<>();

    // Parse proto file for interfaces.
    for (ProtoFile file : sourceProtos) {
      for (DescriptorProtos.ServiceDescriptorProto service : file.getProto().getServiceList()) {
        String serviceFullName =
            String.format("%s.%s", file.getProto().getPackage(), service.getName());
        Interface apiInterface = symbolTable.lookupInterface(serviceFullName);
        if (apiInterface == null) {
          diagCollector.addDiag(
              Diag.error(SimpleLocation.TOPLEVEL, "interface not found: %s", service.getName()));
          continue;
        }
        protoInterfaces.put(serviceFullName, apiInterface);
      }
    }

    // Maps name of interfaces to found InterfaceConfigs from config yamls.
    Map<String, InterfaceConfigProto> interfaceConfigProtos = new HashMap<>();

    // Parse GAPIC config for interfaceConfigProtos and their corresponding proto interfaces.
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
      interfaceConfigProtos.put(interfaceConfigProto.getName(), interfaceConfigProto);
      protoInterfaces.put(interfaceConfigProto.getName(), apiInterface);
    }

    // Generate GapicInterfaceConfigs from the proto interfaces and interfaceConfigProtos.
    for (Entry<String, Interface> interfaceEntry : protoInterfaces.entrySet()) {
      String serviceFullName = interfaceEntry.getKey();
      InterfaceConfigProto interfaceConfigProto =
          interfaceConfigProtos.getOrDefault(
              serviceFullName, InterfaceConfigProto.getDefaultInstance());
      Interface apiInterface = interfaceEntry.getValue();
      String interfaceNameOverride = languageSettings.getInterfaceNamesMap().get(serviceFullName);

      GapicInterfaceConfig interfaceConfig =
          GapicInterfaceConfig.createInterfaceConfig(
              diagCollector,
              language,
              defaultPackageName,
              interfaceConfigProto,
              apiInterface,
              interfaceNameOverride,
              messageConfigs,
              resourceNameConfigs,
              protoParser);
      if (interfaceConfig == null) {
        continue;
      }
      interfaceConfigMap.put(serviceFullName, interfaceConfig);
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

  private static ImmutableMap<String, ResourceNameConfig> createResourceNameConfigs(
      DiagCollector diagCollector, ConfigProto configProto, TargetLanguage language) {
    return createResourceNameConfigs(
        diagCollector, configProto, null, language, ImmutableMap.of(), ImmutableMap.of(), null);
  }

  /**
   * Create all the ResourceNameOneofConfig from the protofile and GAPIC config, and let the GAPIC
   * config resourceNames override the protofile resourceNames in event of clashes.
   */
  @VisibleForTesting
  @Nullable
  static ImmutableMap<String, ResourceNameConfig> createResourceNameConfigs(
      DiagCollector diagCollector,
      ConfigProto configProto,
      @Nullable List<ProtoFile> protoFiles,
      TargetLanguage language,
      Map<Resource, ProtoFile> resourceDefs,
      Map<ResourceSet, ProtoFile> resourceSetDefs,
      ProtoParser protoParser) {
    ProtoFile file = null;
    if (protoFiles != null) {
      file = protoFiles.get(0);
    }
    ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigsFromGapicConfig =
        createSingleResourceNameConfigs(diagCollector, configProto, protoFiles, language);
    ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs =
        createFixedResourceNameConfigs(
            diagCollector, configProto.getFixedResourceNameValuesList(), file);
    ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigsFromGapicConfig =
        createResourceNameOneofConfigs(
            diagCollector,
            configProto.getCollectionOneofsList(),
            singleResourceNameConfigsFromGapicConfig,
            fixedResourceNameConfigs,
            file);
    if (diagCollector.getErrorCount() > 0) {
      ToolUtil.reportDiags(diagCollector, true);
      return null;
    }

    ImmutableMap<String, SingleResourceNameConfig>
        fullyQualifiedSingleResourceNameConfigsFromProtoFile =
            createSingleResourceNameConfigsFromProtoFile(diagCollector, resourceDefs, protoParser);
    ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigsFromProtoFile =
        createResourceNameOneofConfigsFromProtoFile(
            diagCollector,
            fullyQualifiedSingleResourceNameConfigsFromProtoFile,
            resourceSetDefs,
            protoParser);

    // Populate a SingleResourceNameConfigs map, using just the unqualified names.
    Map<String, SingleResourceNameConfig> singleResourceConfigsFromProtoFile = new HashMap<>();
    for (String fullName : fullyQualifiedSingleResourceNameConfigsFromProtoFile.keySet()) {
      int periodIndex = fullName.lastIndexOf('.');
      SingleResourceNameConfig config =
          fullyQualifiedSingleResourceNameConfigsFromProtoFile.get(fullName);
      singleResourceConfigsFromProtoFile.put(fullName.substring(periodIndex + 1), config);
    }

    // Combine the ResourceNameConfigs from the GAPIC and protofile.
    Map<String, SingleResourceNameConfig> finalSingleResourceNameConfigs =
        mergeResourceNameConfigs(
            diagCollector,
            singleResourceNameConfigsFromGapicConfig,
            singleResourceConfigsFromProtoFile);
    Map<String, ResourceNameOneofConfig> finalResourceOneofNameConfigs =
        mergeResourceNameConfigs(
            diagCollector,
            resourceNameOneofConfigsFromGapicConfig,
            resourceNameOneofConfigsFromProtoFile);

    ImmutableMap.Builder<String, ResourceNameConfig> resourceNameConfigs =
        new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());
    resourceNameConfigs.putAll(finalSingleResourceNameConfigs);
    resourceNameConfigs.putAll(fixedResourceNameConfigs);
    resourceNameConfigs.putAll(finalResourceOneofNameConfigs);
    return resourceNameConfigs.build();
  }

  // Return map of fully qualified SingleResourceNameConfig name to its derived config.
  private static ImmutableMap<String, SingleResourceNameConfig>
      createSingleResourceNameConfigsFromProtoFile(
          DiagCollector diagCollector,
          Map<Resource, ProtoFile> resourceDefs,
          ProtoParser protoParser) {

    // Map of fully qualified Resource name to its derived config.
    LinkedHashMap<String, SingleResourceNameConfig> fullyQualifiedSingleResources =
        new LinkedHashMap<>();
    // Create the SingleResourceNameConfigs.
    for (Resource resource : resourceDefs.keySet()) {
      String resourcePath = resource.getPath();
      ProtoFile protoFile = resourceDefs.get(resource);
      createSingleResourceNameConfig(
          diagCollector,
          resource,
          protoFile,
          resourcePath,
          protoParser,
          fullyQualifiedSingleResources);
    }

    if (diagCollector.getErrorCount() > 0) {
      ToolUtil.reportDiags(diagCollector, true);
      return null;
    }
    return ImmutableMap.copyOf(fullyQualifiedSingleResources);
  }

  // Return map of fully qualified ResourceNameOneofConfig name to its derived config.
  private static ImmutableMap<String, ResourceNameOneofConfig>
      createResourceNameOneofConfigsFromProtoFile(
          DiagCollector diagCollector,
          ImmutableMap<String, SingleResourceNameConfig> fullyQualifiedSingleResourcesFromProtoFile,
          Map<ResourceSet, ProtoFile> resourceSetDefs,
          ProtoParser protoParser) {

    // Map of fully qualified ResourceSet name to its derived config.
    ImmutableMap.Builder<String, ResourceNameOneofConfig> resourceOneOfConfigsFromProtoFile =
        ImmutableMap.builder();

    // Create the ResourceNameOneOfConfigs.
    for (ResourceSet resourceSet : resourceSetDefs.keySet()) {
      ProtoFile protoFile = resourceSetDefs.get(resourceSet);
      String resourceSetName = resourceSet.getName();
      ResourceNameOneofConfig resourceNameOneofConfig =
          ResourceNameOneofConfig.createResourceNameOneof(
              diagCollector,
              resourceSet,
              resourceSetName,
              fullyQualifiedSingleResourcesFromProtoFile,
              protoParser,
              protoFile);
      if (resourceNameOneofConfig == null) {
        return null;
      }
      resourceOneOfConfigsFromProtoFile.put(resourceSetName, resourceNameOneofConfig);
    }

    if (diagCollector.getErrorCount() > 0) {
      ToolUtil.reportDiags(diagCollector, true);
      return null;
    }

    return resourceOneOfConfigsFromProtoFile.build();
  }

  private static <T extends ResourceNameConfig> ImmutableMap<String, T> mergeResourceNameConfigs(
      DiagCollector diagCollector,
      Map<String, T> configsFromGapicConfig,
      Map<String, T> configsFromProtoFile) {
    Map<String, T> mergedResourceNameConfigs = new HashMap<>(configsFromProtoFile);

    // If protofile annotations clash with the configs from configProto, use the configProto.
    for (T resourceFromGapicConfig : configsFromGapicConfig.values()) {
      if (configsFromProtoFile.containsKey(resourceFromGapicConfig.getEntityId())) {
        diagCollector.addDiag(
            Diag.warning(
                SimpleLocation.TOPLEVEL,
                "Resource[Set] entity %s from protofile clashes with a"
                    + " Resource[Set] of the same name from the GAPIC config."
                    + " Using the GAPIC config entity.",
                resourceFromGapicConfig.getEntityId()));
      }
      // Add the protofile resourceNameConfigs to the map of resourceNameConfigs.
      mergedResourceNameConfigs.put(resourceFromGapicConfig.getEntityId(), resourceFromGapicConfig);
    }
    return ImmutableMap.copyOf(mergedResourceNameConfigs);
  }

  private static ImmutableMap<String, SingleResourceNameConfig> createSingleResourceNameConfigs(
      DiagCollector diagCollector,
      ConfigProto configProto,
      @Nullable List<ProtoFile> sourceProtos,
      TargetLanguage language) {
    ProtoFile file = null;
    if (sourceProtos != null) {
      file = sourceProtos.get(0);
    }
    LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap =
        new LinkedHashMap<>();
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
      @Nullable ProtoFile file,
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

  // Construct a new SingleResourceNameConfig from the given Resource, and add the newly
  // created config as a value to the map param, keyed on the package-qualified entity_id.
  private static void createSingleResourceNameConfig(
      DiagCollector diagCollector,
      Resource resource,
      ProtoFile file,
      String pathTemplate,
      ProtoParser protoParser,
      LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap) {
    SingleResourceNameConfig singleResourceNameConfig =
        SingleResourceNameConfig.createSingleResourceName(
            resource, pathTemplate, file, diagCollector);
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
      String fullyQualifiedName = singleResourceNameConfig.getEntityId();
      fullyQualifiedName =
          StringUtils.prependIfMissing(fullyQualifiedName, protoParser.getProtoPackage(file) + ".");
      singleResourceNameConfigsMap.put(fullyQualifiedName, singleResourceNameConfig);
    }
  }

  private static ImmutableMap<String, FixedResourceNameConfig> createFixedResourceNameConfigs(
      DiagCollector diagCollector,
      Iterable<FixedResourceNameValueProto> fixedConfigProtos,
      @Nullable ProtoFile file) {
    ImmutableMap.Builder<String, FixedResourceNameConfig> fixedConfigBuilder =
        ImmutableMap.builder();
    for (FixedResourceNameValueProto fixedConfigProto : fixedConfigProtos) {
      FixedResourceNameConfig fixedConfig =
          FixedResourceNameConfig.createFixedResourceNameConfig(
              diagCollector, fixedConfigProto, file);
      if (fixedConfig == null) {
        continue;
      }
      fixedConfigBuilder.put(fixedConfig.getEntityId(), fixedConfig);
    }
    return fixedConfigBuilder.build();
  }

  private static ImmutableMap<String, ResourceNameOneofConfig> createResourceNameOneofConfigs(
      DiagCollector diagCollector,
      Iterable<CollectionOneofProto> oneofConfigProtos,
      ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs,
      ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigs,
      @Nullable ProtoFile file) {
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
