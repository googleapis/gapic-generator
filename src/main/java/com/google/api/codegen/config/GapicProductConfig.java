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
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.pathtemplate.ValidationException;
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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import com.google.gson.internal.LinkedTreeMap;
import com.google.protobuf.DescriptorProtos;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
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

    if (protoPackage != null) {
      if (configProto == null) {
        if (sourceProtos.size() == 0) {
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
    }

    ProtoParser protoParser = new ProtoParser();
    if (configProto == null) {
      configProto = ConfigProto.getDefaultInstance();
    }

    // Get list of fields from proto
    ResourceNameMessageConfigs messageConfigs =
        ResourceNameMessageConfigs.createMessageResourceTypesConfig(
            sourceProtos,
            model.getDiagReporter().getDiagCollector(),
            configProto,
            defaultPackage,
            protoParser);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs =
        createResourceNameConfigs(
            model.getDiagReporter().getDiagCollector(),
            configProto,
            sourceProtos,
            language,
            protoParser);

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

    ImmutableList<String> copyrightLines = null;
    ImmutableList<String> licenseLines = null;
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

    // Maps name of interfaces to found InterfaceConfigs from config yamls.
    Map<String, InterfaceConfigProto> interfaceConfigProtos = new LinkedTreeMap<>();

    // Parse config for interfaceConfigProtos.
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
    }

    // Parse proto file for interfaces.
    for (ProtoFile file : sourceProtos) {
      if (file.getProto().getServiceList().size() == 0) continue;
      for (DescriptorProtos.ServiceDescriptorProto service : file.getProto().getServiceList()) {
        String serviceFullName =
            String.format("%s.%s", file.getProto().getPackage(), service.getName());
        Interface apiInterface = symbolTable.lookupInterface(serviceFullName);
        if (apiInterface == null) {
          diagCollector.addDiag(
              Diag.error(SimpleLocation.TOPLEVEL, "interface not found: %s", service.getName()));
          continue;
        }
        InterfaceConfigProto interfaceConfigProto = interfaceConfigProtos.get(serviceFullName);
        if (interfaceConfigProto == null) {
          interfaceConfigProto = InterfaceConfigProto.getDefaultInstance();
        }
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
    return createResourceNameConfigs(diagCollector, configProto, null, language, null);
  }

  @VisibleForTesting
  @Nullable
  static ImmutableMap<String, ResourceNameConfig> createResourceNameConfigs(
      DiagCollector diagCollector,
      ConfigProto configProto,
      @Nullable List<ProtoFile> protoFiles,
      TargetLanguage language,
      ProtoParser protoParser) {
    ProtoFile file = null;
    if (protoFiles != null) {
      file = protoFiles.get(0);
    }
    ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigs =
        createSingleResourceNameConfigs(
            diagCollector, configProto, protoFiles, language, protoParser);
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
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }

    // TODO(andrealin): make protofiles be an empty list.
    if (protoFiles != null) {
      return mergeResourceNameConfigsFromProtoFile(
          diagCollector,
          protoFiles,
          singleResourceNameConfigs,
          fixedResourceNameConfigs,
          resourceNameOneofConfigs,
          protoParser);
    } else {
      ImmutableMap.Builder<String, ResourceNameConfig> resourceCollectionMap =
          new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());
      resourceCollectionMap.putAll(singleResourceNameConfigs);
      resourceCollectionMap.putAll(resourceNameOneofConfigs);
      resourceCollectionMap.putAll(fixedResourceNameConfigs);
      return resourceCollectionMap.build();
    }
  }

  /**
   * Create all the ResourceNameConfigs from the protofile, and merge them together with the
   * existing ResourceNameConfigs from the GAPIC config, and let the protofile resourceNames
   * override the GAPIC config resourceNames in event of clashes.
   *
   * @param diagCollector error and warning collector
   * @param sourceProtos list of ProtoFiles to create ResourceName configs from
   * @param singleResourceNamesFromGapicConfig Single resource names from GAPIC config
   * @param fixedResourceNameConfigsFromGapicConfig Fixed resource names from GAPIC config
   * @param resourceNameOneofConfigsFromGapicConfig Oneof resource names from GAPIC config.
   * @param protoParser util to parse annotations from ProtoFile
   * @return complete Map of resource_entity_id to ResourceNameConfig, to be used by transformers
   */
  private static ImmutableMap<String, ResourceNameConfig> mergeResourceNameConfigsFromProtoFile(
      DiagCollector diagCollector,
      List<ProtoFile> sourceProtos,
      ImmutableMap<String, SingleResourceNameConfig> singleResourceNamesFromGapicConfig,
      ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigsFromGapicConfig,
      ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigsFromGapicConfig,
      ProtoParser protoParser) {

    Collection<SingleResourceNameConfig> pathTemplatesFromConfig =
        singleResourceNamesFromGapicConfig.values();
    LinkedHashMap<String, SingleResourceNameConfig> singleResourceConfigsFromProtoFile =
        new LinkedHashMap<>();
    LinkedHashMap<String, FixedResourceNameConfig> fixedResourceConfigsFromProtoFile =
        new LinkedHashMap<>();
    LinkedHashMap<String, ResourceNameOneofConfig> resourceOneOfConfigsFromProtoFile =
        new LinkedHashMap<>();
    Map<String, ResourceSet> resourceSets = new HashMap<>();
    Map<String, ProtoFile> resourceSetNamesAndFiles = new HashMap<>();

    // Collect the Single- and Fixed- ResourceNameConfigs from proto annotations.
    for (ProtoFile protoFile : sourceProtos) {
      for (MessageType messageType : protoFile.getMessages()) {
        for (Field field : messageType.getFields()) {
          String resourcePath = protoParser.getResourcePath(field);
          ResourceSet resourceSet = protoParser.getResourceSet(field);
          if (resourceSet != null && !Strings.isNullOrEmpty(resourcePath)) {
            diagCollector.addDiag(
                Diag.error(
                    SimpleLocation.TOPLEVEL,
                    "A field can have *either* a Resource *or* a ResourceSet,"
                        + " but field %s has both resource path %s and resourceSet %s",
                    field.getSimpleName(),
                    resourcePath,
                    resourceSet.toString()));
            return null;
          }

          if (resourceSet != null) {
            // Create the resource one-ofs later, after the singleResourceNameConfigs are parsed.
            String resourceSetName =
                protoParser.getResourceEntityName(
                    field, protoParser.getDefaultResourceEntityName(field));
            resourceSets.put(resourceSetName, resourceSet);
            resourceSetNamesAndFiles.put(resourceSetName, protoFile);
          }

          if (resourcePath != null) {
            PathTemplate pathTemplate;
            try {
              pathTemplate = PathTemplate.create(resourcePath);
            } catch (ValidationException e) {
              diagCollector.addDiag(Diag.error(SimpleLocation.TOPLEVEL, e.getMessage()));
              return null;
            }

            try {
              // Only fixed paths will encode against no values.
              pathTemplate.encode(pathTemplate.encode());
            } catch (ValidationException e) {
              // This was not a fixed resourcename, so let's make a Single resource name config.
              createSingleResourceNameConfig(
                  diagCollector,
                  field,
                  pathTemplate,
                  pathTemplatesFromConfig,
                  singleResourceConfigsFromProtoFile,
                  protoFile,
                  protoParser);
              continue;
            }
            FixedResourceNameConfig fixedNameConfig =
                FixedResourceNameConfig.createFixedResourceNameConfig(
                    diagCollector, field, protoFile, protoParser);
            if (fixedNameConfig == null) {
              return null;
            }
            fixedResourceConfigsFromProtoFile.put(fixedNameConfig.getEntityId(), fixedNameConfig);
          }
        }
      }
    }

    // Create the ResourceNameOneOfConfigs.
    for (String resourceSetName : resourceSets.keySet()) {
      ResourceSet resourceSet = resourceSets.get(resourceSetName);
      ProtoFile protoFile = resourceSetNamesAndFiles.get(resourceSetName);
      ResourceNameOneofConfig resourceNameOneofConfig =
          ResourceNameOneofConfig.createResourceNameOneof(
              diagCollector,
              resourceSet,
              resourceSetName,
              ImmutableMap.copyOf(singleResourceConfigsFromProtoFile),
              ImmutableMap.copyOf(fixedResourceConfigsFromProtoFile),
              protoFile,
              protoParser);
      if (resourceNameOneofConfig == null) {
        return null;
      }
      resourceOneOfConfigsFromProtoFile.put(resourceSetName, resourceNameOneofConfig);
    }

    Map<String, SingleResourceNameConfig> finalSingleResourceNameConfigs =
        mergeResourceNameConfigs(
            diagCollector,
            singleResourceNamesFromGapicConfig,
            singleResourceConfigsFromProtoFile,
            SingleResourceNameConfig::getNamePattern);

    Map<String, FixedResourceNameConfig> finalFixedResourceNameConfigs =
        mergeResourceNameConfigs(
            diagCollector,
            fixedResourceNameConfigsFromGapicConfig,
            fixedResourceConfigsFromProtoFile,
            FixedResourceNameConfig::getFixedValue);

    Map<String, ResourceNameOneofConfig> finalResourceOneofNameConfigs =
        mergeResourceNameConfigs(
            diagCollector,
            resourceNameOneofConfigsFromGapicConfig,
            resourceOneOfConfigsFromProtoFile,
            ResourceNameOneofConfig::getEntityName);

    if (diagCollector.getErrorCount() > 0) {
      return null;
    } else {
      ImmutableMap.Builder<String, ResourceNameConfig> resourceNameConfigs =
          new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());
      resourceNameConfigs.putAll(finalSingleResourceNameConfigs);
      resourceNameConfigs.putAll(finalFixedResourceNameConfigs);
      resourceNameConfigs.putAll(finalResourceOneofNameConfigs);
      return resourceNameConfigs.build();
    }
  }

  public <T> List<T> getMost(List<T> list) {
    return list.subList(0, list.size() - 1);
  }

  private static <T extends ResourceNameConfig> ImmutableMap<String, T> mergeResourceNameConfigs(
      DiagCollector diagCollector,
      Map<String, T> configsFromGapicConfig,
      Map<String, T> configsFromProtoFile,
      Function<T, String> toStringFunction) {
    Map<String, T> mergedResourceNameConfigs = new HashMap<>(configsFromGapicConfig);

    // If protofile annotations clash with the configs from configProto, use the protofile.
    for (T resourceFromProtoFile : configsFromProtoFile.values()) {
      if (configsFromGapicConfig.containsKey(resourceFromProtoFile.getEntityId())) {
        T otherConfig = configsFromGapicConfig.get(resourceFromProtoFile.getEntityId());
        String thisStringRepresentation = toStringFunction.apply(resourceFromProtoFile);
        String otherStringRepresentation = toStringFunction.apply(otherConfig);
        if (!thisStringRepresentation.equals(otherStringRepresentation)) {
          diagCollector.addDiag(
              Diag.warning(
                  SimpleLocation.TOPLEVEL,
                  "For entity %s, resource path '%s'"
                      + " from protofile clashes with GAPIC config resource path %s."
                      + " Using path '%s' from protofile.",
                  resourceFromProtoFile.getEntityId(),
                  thisStringRepresentation,
                  otherStringRepresentation,
                  resourceFromProtoFile.getEntityId()));
        }
      }
      // Add the protofile resourceNameConfigs to the map of resourceNameConfigs.
      mergedResourceNameConfigs.put(resourceFromProtoFile.getEntityId(), resourceFromProtoFile);
    }
    return ImmutableMap.copyOf(mergedResourceNameConfigs);
  }

  private static ImmutableMap<String, SingleResourceNameConfig> createSingleResourceNameConfigs(
      DiagCollector diagCollector,
      ConfigProto configProto,
      @Nullable List<ProtoFile> sourceProtos,
      TargetLanguage language,
      ProtoParser protoParser) {
    ProtoFile file = null;
    if (sourceProtos == null) {
      sourceProtos = ImmutableList.of();
    } else {
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

  private static void createSingleResourceNameConfig(
      DiagCollector diagCollector,
      Field field,
      PathTemplate pathTemplate,
      Collection<SingleResourceNameConfig> resourceNamesFromConfig,
      LinkedHashMap<String, SingleResourceNameConfig> singleResourceNameConfigsMap,
      ProtoFile file,
      ProtoParser protoParser) {
    SingleResourceNameConfig singleResourceNameConfig =
        SingleResourceNameConfig.createSingleResourceName(
            diagCollector, field, pathTemplate, resourceNamesFromConfig, file, protoParser);
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
