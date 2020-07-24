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

import com.google.api.ResourceReference;
import com.google.api.codegen.CollectionConfigProto;
import com.google.api.codegen.CollectionOneofProto;
import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.DeprecatedCollectionConfigProto;
import com.google.api.codegen.InterfaceConfigProto;
import com.google.api.codegen.LanguageSettingsProto;
import com.google.api.codegen.MethodConfigProto;
import com.google.api.codegen.ReleaseLevel;
import com.google.api.codegen.ResourceNameTreatment;
import com.google.api.codegen.VisibilityProto;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.configgen.mergers.LanguageSettingsMerger;
import com.google.api.codegen.grpc.ServiceConfig;
import com.google.api.codegen.samplegen.v1p2.SampleConfigProto;
import com.google.api.codegen.util.ConfigVersionValidator;
import com.google.api.codegen.util.LicenseHeaderUtil;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.ProtoParser;
import com.google.api.tools.framework.model.*;
import com.google.auto.value.AutoValue;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.ImmutableTable;
import com.google.common.collect.Iterables;
import com.google.protobuf.Api;
import com.google.protobuf.DescriptorProtos;
import java.util.*;
import java.util.function.Function;
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

  /** Returns a map from entity names to resource name configs. */
  public abstract ProtoParser getProtoParser();

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

  @Nullable
  public abstract Boolean enableStringFormattingFunctionsOverride();

  /**
   * Returns a table of SampleConfigs. The row key is the full name of the interface and the column
   * key is the name of the method.
   */
  @Nullable
  public abstract ImmutableTable<String, String, ImmutableList<SampleConfig>>
      getSampleConfigTable();

  public abstract Date getGenerationTimestamp();

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
        getProtoParser(),
        getTransportProtocol(),
        getDefaultResourceNameFieldConfigMap(),
        getConfigSchemaVersion(),
        enableStringFormattingFunctionsOverride(),
        getSampleConfigTable(),
        getGenerationTimestamp());
  }

  @Nullable
  public static GapicProductConfig create(
      Model model, ConfigProto configProto, TargetLanguage language) {
    return create(model, configProto, null, null, null, language, null, TransportProtocol.GRPC);
  }

  /**
   * Creates an instance of GapicProductConfig based on ConfigProto, linking up API interface
   * configurations with specified interfaces in interfaceConfigMap. On errors, null will be
   * returned, and diagnostics are reported to the model.
   *
   * @param model The protobuf model for which we are creating a config.
   * @param configProto The parsed set of library config files from input
   * @param sampleConfigProto The parsed set of sample config files from the input
   * @param protoPackage The source proto package, as opposed to imported protos, that we will
   *     generate clients for.
   * @param clientPackage The desired package name for the generated client.
   * @param language The language that this config will be used to generate a client in.
   */
  @Nullable
  public static GapicProductConfig create(
      Model model,
      @Nullable ConfigProto configProto,
      @Nullable SampleConfigProto sampleConfigProto,
      @Nullable String protoPackage,
      @Nullable String clientPackage,
      TargetLanguage language,
      @Nullable ServiceConfig grpcServiceConfig,
      TransportProtocol transportProtocol) {

    final String defaultPackage;
    SymbolTable symbolTable = model.getSymbolTable();

    if (protoPackage != null) {
      // Default to using --package option for value of default package and first API protoFile.
      defaultPackage = protoPackage;
    } else if (configProto != null && configProto.getInterfacesCount() > 0) {
      // Otherwise use configProto to get the proto file containing the first interface listed in
      // the config proto, and use it as the assigned file for generated resource names, and to get
      // the default message namespace.
      ProtoFile file =
          symbolTable.lookupInterface(configProto.getInterfaces(0).getName()).getFile();
      defaultPackage = file.getProto().getPackage();
    } else {
      throw new NullPointerException(
          "configProto and protoPackage cannot both be null. "
              + "If using artman, please add the proto_package field to artman config, "
              + "or switch to bazel.");
    }

    // Consider all proto files in the defaultPackage as well as its sub-packages
    // as source files.
    List<ProtoFile> sourceProtos =
        model
            .getFiles()
            .stream()
            .filter(f -> f.getProto().getPackage().startsWith(defaultPackage))
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

    // Toggle on/off proto annotations parsing.
    ProtoParser protoParser;
    ConfigVersionValidator versionValidator = new ConfigVersionValidator();
    if (versionValidator.isV2Config(configProto)) {
      versionValidator.validateV2Config(configProto);
      protoParser = new ProtoParser(true);

      if (configProto == null) {
        configProto = ConfigProto.getDefaultInstance();
      }
    } else {
      protoParser = new ProtoParser(false);
    }

    DiagCollector diagCollector = model.getDiagReporter().getDiagCollector();

    ProtoFile packageProtoFile = sourceProtos.isEmpty() ? null : sourceProtos.get(0);

    ImmutableMap<String, ResourceNameConfig> resourceNameConfigs;
    ResourceNameMessageConfigs messageConfigs;
    if (protoParser.isProtoAnnotationsEnabled()) {
      Map<String, ResourceDescriptorConfig> descriptorConfigMap =
          protoParser.getResourceDescriptorConfigMap(model.getFiles(), diagCollector);

      List<ResourceReference> fieldsWithResourceRefs =
          sourceProtos
              .stream()
              .flatMap(protoFile -> protoFile.getMessages().stream())
              .flatMap(messageType -> messageType.getFields().stream())
              .filter(protoParser::hasResourceReference)
              .map(protoParser::getResourceReference)
              .collect(Collectors.toList());

      Set<String> configsWithChildTypeReferences =
          fieldsWithResourceRefs
              .stream()
              .map(ResourceReference::getChildType)
              .filter(t -> !Strings.isNullOrEmpty(t))
              .collect(Collectors.toSet());

      Set<String> configsWithTypeReferences =
          fieldsWithResourceRefs
              .stream()
              .map(ResourceReference::getType)
              .filter(t -> !Strings.isNullOrEmpty(t))
              .collect(Collectors.toSet());

      Map<String, DeprecatedCollectionConfigProto> deprecatedPatternResourceMap =
          configProto
              .getInterfacesList()
              .stream()
              .flatMap(i -> i.getDeprecatedCollectionsList().stream())
              .distinct()
              .collect(
                  ImmutableMap.toImmutableMap(
                      DeprecatedCollectionConfigProto::getNamePattern, c -> c));

      // Create a pattern-to-resource map to make looking up parent resources easier.
      Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap =
          ResourceDescriptorConfig.getPatternResourceMap(descriptorConfigMap.values());

      // Create a child-to-parent map to make resolving child_type easier.
      Map<String, List<ResourceDescriptorConfig>> childParentResourceMap =
          ResourceDescriptorConfig.getChildParentResourceMap(
              descriptorConfigMap, patternResourceDescriptorMap);
      resourceNameConfigs =
          createResourceNameConfigsFromAnnotationsAndGapicConfig(
              model,
              diagCollector,
              configProto,
              packageProtoFile,
              language,
              descriptorConfigMap,
              configsWithTypeReferences,
              configsWithChildTypeReferences,
              deprecatedPatternResourceMap,
              patternResourceDescriptorMap,
              childParentResourceMap,
              defaultPackage);

      messageConfigs =
          ResourceNameMessageConfigs.createFromAnnotations(
              diagCollector,
              model.getFiles(),
              resourceNameConfigs,
              protoParser,
              descriptorConfigMap,
              childParentResourceMap);
    } else {
      resourceNameConfigs =
          createResourceNameConfigsFromGapicConfigOnly(
              model, diagCollector, configProto, packageProtoFile, language);
      messageConfigs =
          ResourceNameMessageConfigs.createFromGapicConfigOnly(
              sourceProtos, configProto, defaultPackage);
    }

    if (resourceNameConfigs == null) {
      return null;
    }

    // TransportProtocol transportProtocol = TransportProtocol.GRPC;

    String clientPackageName;
    LanguageSettingsProto settings =
        configProto.getLanguageSettingsMap().get(language.toString().toLowerCase());
    if (settings == null) {
      settings = LanguageSettingsProto.getDefaultInstance();

      if (!Strings.isNullOrEmpty(clientPackage)) {
        clientPackageName = clientPackage;
      } else {
        String basePackageName = Optional.ofNullable(protoPackage).orElse(getPackageName(model));
        clientPackageName =
            LanguageSettingsMerger.getFormattedPackageName(language, basePackageName);
      }
    } else {
      clientPackageName = settings.getPackageName();
    }

    ImmutableMap<String, Interface> protoInterfaces =
        getInterfacesFromProtoFile(diagCollector, sourceProtos, symbolTable);

    ImmutableList<GapicInterfaceInput> interfaceInputs;
    if (protoParser.isProtoAnnotationsEnabled()) {
      interfaceInputs =
          createInterfaceInputsWithAnnotationsAndGapicConfig(
              diagCollector, configProto.getInterfacesList(), protoInterfaces, language);
    } else {
      interfaceInputs =
          createInterfaceInputsWithGapicConfigOnly(
              diagCollector,
              configProto.getInterfacesList(),
              protoInterfaces,
              symbolTable,
              language);
    }
    if (interfaceInputs == null) {
      return null;
    }

    GrpcGapicRetryMapping grpcGapicRetryMapping = null;
    if (grpcServiceConfig != null) {
      grpcGapicRetryMapping = GrpcGapicRetryMapping.create(grpcServiceConfig, protoInterfaces);
    }

    ImmutableMap<String, InterfaceConfig> interfaceConfigMap =
        createInterfaceConfigMap(
            diagCollector,
            interfaceInputs,
            defaultPackage,
            settings,
            messageConfigs,
            resourceNameConfigs,
            language,
            protoParser,
            grpcGapicRetryMapping);

    ImmutableList<String> copyrightLines;
    ImmutableList<String> licenseLines;
    String configSchemaVersion = null;

    LicenseHeaderUtil licenseHeaderUtil = new LicenseHeaderUtil();
    try {
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

    Boolean enableStringFormatFunctionsOverride = null;
    if (configProto.hasEnableStringFormatFunctionsOverride()) {
      enableStringFormatFunctionsOverride =
          configProto.getEnableStringFormatFunctionsOverride().getValue();
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
        protoParser,
        transportProtocol,
        createResponseFieldConfigMap(messageConfigs, resourceNameConfigs),
        configSchemaVersion,
        enableStringFormatFunctionsOverride,
        SampleConfig.createSampleConfigTable(sampleConfigProto, interfaceConfigMap),
        new Date());
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

    LicenseHeaderUtil licenseHeaderUtil = new LicenseHeaderUtil();
    ImmutableList<String> copyrightLines;
    ImmutableList<String> licenseLines;
    try {
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

    Boolean enableStringFormatFunctionsOverride = null;
    if (configProto.getEnableStringFormatFunctionsOverride().isInitialized()) {
      enableStringFormatFunctionsOverride =
          configProto.getEnableStringFormatFunctionsOverride().getValue();
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
        new ProtoParser(false),
        transportProtocol,
        createResponseFieldConfigMap(messageConfigs, resourceNameConfigs),
        configSchemaVersion,
        enableStringFormatFunctionsOverride,
        ImmutableTable.of(),
        new Date());
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
  private static GapicProductConfig createDummyInstance(
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
        new ProtoParser(true),
        // Default to gRPC.
        TransportProtocol.GRPC,
        createResponseFieldConfigMap(messageConfigs, ImmutableMap.of()),
        configSchemaVersion,
        false,
        ImmutableTable.of(),
        new Date());
  }

  /** Return the list of information about clients to be generated. */
  private static ImmutableList<GapicInterfaceInput>
      createInterfaceInputsWithAnnotationsAndGapicConfig(
          DiagCollector diagCollector,
          List<InterfaceConfigProto> interfaceConfigProtosList,
          ImmutableMap<String, Interface> protoInterfaces,
          TargetLanguage language) {
    return createGapicInterfaceInputList(
        diagCollector,
        language,
        protoInterfaces.values(),
        interfaceConfigProtosList
            .stream()
            .collect(Collectors.toMap(InterfaceConfigProto::getName, Function.identity())));
  }

  private static ImmutableList<GapicInterfaceInput> createInterfaceInputsWithGapicConfigOnly(
      DiagCollector diagCollector,
      List<InterfaceConfigProto> interfaceConfigProtosList,
      ImmutableMap<String, Interface> protoInterfaces,
      SymbolTable symbolTable,
      TargetLanguage language) {

    // Maps name of interfaces to found interfaces from proto.
    Map<String, Interface> interfaceMap = new LinkedHashMap<>(protoInterfaces);

    // Maps name of interfaces to found InterfaceConfigs from config yamls.
    Map<String, InterfaceConfigProto> interfaceConfigProtos = new LinkedHashMap<>();

    // Parse GAPIC config for interfaceConfigProtos.
    for (InterfaceConfigProto interfaceConfigProto : interfaceConfigProtosList) {
      Interface apiInterface = symbolTable.lookupInterface(interfaceConfigProto.getName());
      if (apiInterface == null) {
        List<String> interfaces =
            symbolTable
                .getInterfaces()
                .stream()
                .map(ProtoElement::getFullName)
                .collect(Collectors.toList());
        String interfacesString = String.join(",", interfaces);
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "interface not found: %s. Interfaces: [%s]",
                interfaceConfigProto.getName(),
                interfacesString));
        continue;
      }
      if (!apiInterface.isReachable()) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "interface not reachable: %s.",
                interfaceConfigProto.getName()));
        continue;
      }
      interfaceConfigProtos.put(interfaceConfigProto.getName(), interfaceConfigProto);
      interfaceMap.put(interfaceConfigProto.getName(), apiInterface);
    }

    return createGapicInterfaceInputList(
        diagCollector, language, interfaceMap.values(), interfaceConfigProtos);
  }

  private static ImmutableList<GapicInterfaceInput> createGapicInterfaceInputList(
      DiagCollector diagCollector,
      TargetLanguage language,
      Iterable<Interface> interfaceList,
      Map<String, InterfaceConfigProto> interfaceConfigProtos) {

    // Store info about each Interface in a GapicInterfaceInput object.
    ImmutableList.Builder<GapicInterfaceInput> interfaceInputs = ImmutableList.builder();
    for (Interface apiInterface : interfaceList) {
      String serviceFullName = apiInterface.getFullName();
      GapicInterfaceInput.Builder interfaceInput =
          GapicInterfaceInput.newBuilder().setInterface(apiInterface);

      InterfaceConfigProto interfaceConfigProto =
          interfaceConfigProtos.getOrDefault(
              serviceFullName, InterfaceConfigProto.getDefaultInstance());
      interfaceInput.setInterfaceConfigProto(interfaceConfigProto);

      Map<Method, MethodConfigProto> methodsToGenerate;
      methodsToGenerate =
          findMethodsToGenerateWithConfigYaml(
              apiInterface, interfaceConfigProto, language, diagCollector);
      if (methodsToGenerate == null) {
        return null;
      }
      interfaceInput.setMethodsToGenerate(methodsToGenerate);
      interfaceInputs.add(interfaceInput.build());
    }

    return interfaceInputs.build();
  }

  private static ImmutableMap<String, Interface> getInterfacesFromProtoFile(
      DiagCollector diagCollector, List<ProtoFile> sourceProtos, SymbolTable symbolTable) {
    // Maps name of interfaces to found interfaces from proto.
    ImmutableMap.Builder<String, Interface> protoInterfaces = ImmutableMap.builder();

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
    return protoInterfaces.build();
  }

  /** Find the methods that should be generated on the surface when a GAPIC config was given. */
  @Nullable
  private static ImmutableMap<Method, MethodConfigProto> findMethodsToGenerateWithConfigYaml(
      Interface apiInterface,
      InterfaceConfigProto interfaceConfigProto,
      TargetLanguage targetLanguage,
      DiagCollector diagCollector) {

    LinkedHashMap<Method, MethodConfigProto> methodMap = new LinkedHashMap<>();

    // Exclude these proto methods that are marked as DISABLE in the GAPIC config.
    Set<Method> gapicDisabledMethods = new HashSet<>();

    // Get the set of methods defined by the GAPIC config. Only these methods will be generated.
    for (MethodConfigProto methodConfigProto : interfaceConfigProto.getMethodsList()) {
      Interface targetInterface =
          GapicInterfaceConfig.getTargetInterface(
              apiInterface, methodConfigProto.getRerouteToGrpcInterface());
      Method protoMethod = targetInterface.lookupMethod(methodConfigProto.getName());

      if (protoMethod == null) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL, "method not found: %s", methodConfigProto.getName()));
        continue;
      }
      if (methodConfigProto
          .getSurfaceTreatmentsList()
          .stream()
          .anyMatch(
              s ->
                  s.getVisibility().equals(VisibilityProto.DISABLED)
                      && s.getIncludeLanguagesList()
                          .stream()
                          .anyMatch(lang -> lang.equalsIgnoreCase(targetLanguage.name())))) {
        gapicDisabledMethods.add(protoMethod);
        continue;
      }
      methodMap.put(protoMethod, methodConfigProto);
    }

    // Add the proto methods that do not have GAPIC configurations.
    for (Method method : apiInterface.getMethods()) {
      if (methodMap.containsKey(method) || gapicDisabledMethods.contains(method)) continue;
      methodMap.put(method, MethodConfigProto.getDefaultInstance());
    }

    if (diagCollector.getErrorCount() > 0) {
      return null;
    }

    return ImmutableMap.copyOf(methodMap);
  }

  private static ImmutableMap<String, InterfaceConfig> createInterfaceConfigMap(
      DiagCollector diagCollector,
      List<GapicInterfaceInput> interfaceInputs,
      String defaultPackageName,
      LanguageSettingsProto languageSettings,
      ResourceNameMessageConfigs messageConfigs,
      ImmutableMap<String, ResourceNameConfig> resourceNameConfigs,
      TargetLanguage language,
      ProtoParser protoParser,
      GrpcGapicRetryMapping grpcGapicRetryMapping) {
    // Return value; maps interface names to their InterfaceConfig.
    ImmutableMap.Builder<String, InterfaceConfig> interfaceConfigMap = ImmutableMap.builder();

    for (GapicInterfaceInput interfaceInput : interfaceInputs) {

      String serviceFullName = interfaceInput.getServiceFullName();
      String interfaceNameOverride = languageSettings.getInterfaceNamesMap().get(serviceFullName);

      GapicInterfaceConfig interfaceConfig =
          GapicInterfaceConfig.createInterfaceConfig(
              diagCollector,
              language,
              defaultPackageName,
              interfaceInput,
              interfaceNameOverride,
              messageConfigs,
              resourceNameConfigs,
              protoParser,
              grpcGapicRetryMapping);
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
    return createResourceNameConfigsFromGapicConfigOnly(
        null, diagCollector, configProto, null, language);
  }

  /**
   * Create all the ResourceNameOneofConfig from the protofile and GAPIC config. Apply the GAPIC
   * config resourceNames' language-specific overrides. Note the keys in the returned map are
   * unqualified, UpperCamel resource type names.
   */
  @VisibleForTesting
  @Nullable
  static ImmutableMap<String, ResourceNameConfig>
      createResourceNameConfigsFromAnnotationsAndGapicConfig(
          Model model,
          DiagCollector diagCollector,
          ConfigProto configProto,
          @Nullable ProtoFile sampleProtoFile,
          TargetLanguage language,
          Map<String, ResourceDescriptorConfig> resourceDescriptorConfigs,
          Set<String> typesWithTypeReferences,
          Set<String> typesWithChildReferences,
          Map<String, DeprecatedCollectionConfigProto> deprecatedPatternResourceMap,
          Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap,
          Map<String, List<ResourceDescriptorConfig>> childParentResourceMap,
          String defaultPackage) {

    Map<CollectionConfigProto, Interface> allCollectionConfigProtos =
        getAllCollectionConfigProtos(model, configProto);
    ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigsFromGapicConfig =
        createSingleResourceNamesFromGapicConfigOnly(
            diagCollector, allCollectionConfigProtos, sampleProtoFile, language);

    Map<String, ResourceNameConfig> annotationResourceNameConfigs =
        getResourceNameConfigsFromAnnotation(
            diagCollector,
            language,
            resourceDescriptorConfigs,
            typesWithTypeReferences,
            typesWithChildReferences,
            deprecatedPatternResourceMap,
            patternResourceDescriptorMap,
            childParentResourceMap,
            defaultPackage,
            singleResourceNameConfigsFromGapicConfig);

    // Single resource names cannot be supported in a standalone manner for GAPIC v2, because the
    // pattern field has been removed. Therefore, throw an error if a single resource config is
    // found that does not exist in an annotation.
    for (String key : singleResourceNameConfigsFromGapicConfig.keySet()) {
      // Annotations specify resource names in UpperCamelCase, while gapic.yaml does it in
      // lower_snake_case. We need to ensure that both names are in same format before comparing one
      // to another.
      String annotationsStyleKey = Name.from(key).toUpperCamel();
      if (!annotationResourceNameConfigs.containsKey(annotationsStyleKey)) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Found single resource name \"%s\" in GAPIC config that has no corresponding annotation",
                key));
      }
      if (annotationResourceNameConfigs.get(annotationsStyleKey).getResourceNameType()
          != ResourceNameType.SINGLE) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Found single resource name \"%s\" in GAPIC config that had entity name matching a non-single resource annotation: %s",
                key,
                annotationResourceNameConfigs.get(key)));
      }
    }

    // TODO(andrealin): Remove this once explicit fixed resource names are gone-zos.
    ImmutableMap<String, FixedResourceNameConfig> finalFixedResourceNameConfigs =
        createFixedResourceNamesFromGapicConfigOnly(diagCollector, allCollectionConfigProtos);

    // TODO(andrealin): Remove this once ResourceSets are approved.
    ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigsFromGapicConfig =
        createResourceNameOneofConfigs(
            diagCollector,
            configProto.getCollectionOneofsList(),
            singleResourceNameConfigsFromGapicConfig,
            finalFixedResourceNameConfigs,
            sampleProtoFile);
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }

    ImmutableMap.Builder<String, ResourceNameConfig> resourceNameConfigs =
        new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());
    resourceNameConfigs.putAll(annotationResourceNameConfigs);
    resourceNameConfigs.putAll(finalFixedResourceNameConfigs);
    resourceNameConfigs.putAll(resourceNameOneofConfigsFromGapicConfig);
    return resourceNameConfigs.build();
  }

  /**
   * Create all the ResourceNameOneofConfig from the protofile and GAPIC config, and let the GAPIC
   * config resourceNames override the protofile resourceNames in event of clashes.
   */
  @Nullable
  private static ImmutableMap<String, ResourceNameConfig>
      createResourceNameConfigsFromGapicConfigOnly(
          Model model,
          DiagCollector diagCollector,
          ConfigProto configProto,
          @Nullable ProtoFile sampleProtoFile,
          TargetLanguage language) {

    Map<CollectionConfigProto, Interface> allCollectionConfigProtos =
        getAllCollectionConfigProtos(model, configProto);

    ImmutableMap<String, SingleResourceNameConfig> singleResourceNameConfigsFromGapicConfig =
        createSingleResourceNamesFromGapicConfigOnly(
            diagCollector, allCollectionConfigProtos, sampleProtoFile, language);
    ImmutableMap<String, FixedResourceNameConfig> fixedResourceNameConfigsFromGapicConfig =
        createFixedResourceNamesFromGapicConfigOnly(diagCollector, allCollectionConfigProtos);

    validateSingleResourceNameConfigs(singleResourceNameConfigsFromGapicConfig);

    ImmutableMap<String, ResourceNameOneofConfig> resourceNameOneofConfigsFromGapicConfig =
        createResourceNameOneofConfigs(
            diagCollector,
            configProto.getCollectionOneofsList(),
            singleResourceNameConfigsFromGapicConfig,
            fixedResourceNameConfigsFromGapicConfig,
            sampleProtoFile);
    if (diagCollector.getErrorCount() > 0) {
      return null;
    }

    ImmutableMap.Builder<String, ResourceNameConfig> resourceNameConfigs =
        new ImmutableSortedMap.Builder<>(Comparator.naturalOrder());
    resourceNameConfigs.putAll(singleResourceNameConfigsFromGapicConfig);
    resourceNameConfigs.putAll(fixedResourceNameConfigsFromGapicConfig);
    resourceNameConfigs.putAll(resourceNameOneofConfigsFromGapicConfig);
    return resourceNameConfigs.build();
  }

  private static ImmutableMap<String, SingleResourceNameConfig>
      createSingleResourceNamesFromGapicConfigOnly(
          DiagCollector diagCollector,
          Map<CollectionConfigProto, Interface> allCollectionConfigs,
          ProtoFile sampleProtoFile,
          TargetLanguage language) {
    Map<String, SingleResourceNameConfig> singleResourceNameConfigMap = new LinkedHashMap<>();
    for (Map.Entry<CollectionConfigProto, Interface> entry : allCollectionConfigs.entrySet()) {
      CollectionConfigProto collectionConfigProto = entry.getKey();
      Interface anInterface = entry.getValue();
      ProtoFile protoFile = anInterface == null ? sampleProtoFile : anInterface.getFile();
      if (Strings.isNullOrEmpty(collectionConfigProto.getNamePattern())
          || !FixedResourceNameConfig.isFixedResourceNameConfig(
              collectionConfigProto.getNamePattern())) {
        createSingleResourceNameConfigFromGapicConfig(
            diagCollector, collectionConfigProto, singleResourceNameConfigMap, protoFile, language);
      }
    }
    return ImmutableMap.copyOf(singleResourceNameConfigMap);
  }

  private static ImmutableMap<String, FixedResourceNameConfig>
      createFixedResourceNamesFromGapicConfigOnly(
          DiagCollector diagCollector, Map<CollectionConfigProto, Interface> allCollectionConfigs) {
    LinkedHashMap<String, FixedResourceNameConfig> fixedResourceNameConfigMap =
        new LinkedHashMap<>();
    for (Map.Entry<CollectionConfigProto, Interface> entry : allCollectionConfigs.entrySet()) {
      CollectionConfigProto collectionConfigProto = entry.getKey();
      Interface interface_ = entry.getValue();
      ProtoFile protoFile = interface_ == null ? null : interface_.getFile();
      if (!Strings.isNullOrEmpty(collectionConfigProto.getNamePattern())
          && FixedResourceNameConfig.isFixedResourceNameConfig(
              collectionConfigProto.getNamePattern())) {
        FixedResourceNameConfig fixedResourceNameConfig =
            FixedResourceNameConfig.createFixedResourceNameConfig(
                diagCollector,
                collectionConfigProto.getEntityName(),
                collectionConfigProto.getNamePattern(),
                protoFile);
        insertFixedResourceNameConfig(
            diagCollector, fixedResourceNameConfig, "", fixedResourceNameConfigMap);
      }
    }

    return ImmutableMap.copyOf(fixedResourceNameConfigMap);
  }

  private static void validateSingleResourceNameConfigs(
      Map<String, ? extends ResourceNameConfig> resourceNameConfigMap) {
    for (ResourceNameConfig resourceNameConfig : resourceNameConfigMap.values()) {
      if (resourceNameConfig.getResourceNameType().equals(ResourceNameType.SINGLE)) {
        if (((SingleResourceNameConfig) resourceNameConfig).getNameTemplate() == null) {
          throw new IllegalStateException(
              String.format(
                  "Single resource entity '%s' does not have a valid name pattern.",
                  resourceNameConfig.getEntityId()));
        }
      }
    }
  }

  private static void createSingleResourceNameConfigFromGapicConfig(
      DiagCollector diagCollector,
      CollectionConfigProto collectionConfigProto,
      Map<String, SingleResourceNameConfig> singleResourceNameConfigsMap,
      @Nullable ProtoFile file,
      TargetLanguage language) {
    SingleResourceNameConfig singleResourceNameConfig =
        SingleResourceNameConfig.createSingleResourceName(
            diagCollector, collectionConfigProto, file, language);
    insertSingleResourceNameConfig(
        diagCollector, singleResourceNameConfig, "", singleResourceNameConfigsMap);
  }

  // Construct a new SingleResourceNameConfig from the given SingleResourceNameConfig,
  // and add the newly created config as a value to the map param,
  // keyed on the entity_id.
  private static void insertSingleResourceNameConfig(
      DiagCollector diagCollector,
      SingleResourceNameConfig singleResourceNameConfig,
      String prefixForMap,
      Map<String, SingleResourceNameConfig> singleResourceNameConfigsMap) {
    if (singleResourceNameConfig == null) {
      return;
    }
    String configKey = singleResourceNameConfig.getEntityId();
    configKey = StringUtils.prependIfMissing(configKey, prefixForMap);
    SingleResourceNameConfig prevConfig =
        singleResourceNameConfigsMap.put(configKey, singleResourceNameConfig);
    if (prevConfig != null
        && !singleResourceNameConfig.getNamePattern().equals(prevConfig.getNamePattern())) {
      diagCollector.addDiag(
          Diag.error(
              SimpleLocation.TOPLEVEL,
              "Inconsistent collection configs across interfaces. Entity name: "
                  + singleResourceNameConfig.getEntityId()));
    }
  }

  // Construct a new FixedResourceNameConfig from the given Resource, and add the newly
  // created config as a value to the map param, keyed on the package-qualified entity_id.
  private static void insertFixedResourceNameConfig(
      DiagCollector diagCollector,
      FixedResourceNameConfig fixedResourceNameConfig,
      String prefix,
      Map<String, FixedResourceNameConfig> fixedResourceNameConfigsMap) {
    if (fixedResourceNameConfigsMap.containsKey(fixedResourceNameConfig.getEntityId())) {
      FixedResourceNameConfig otherConfig =
          fixedResourceNameConfigsMap.get(fixedResourceNameConfig.getEntityId());
      if (!fixedResourceNameConfig.getFixedValue().equals(otherConfig.getFixedValue())) {
        diagCollector.addDiag(
            Diag.error(
                SimpleLocation.TOPLEVEL,
                "Inconsistent collection configs across interfaces. Entity name: "
                    + fixedResourceNameConfig.getEntityId()));
      }
    } else {
      String fullyQualifiedName = fixedResourceNameConfig.getEntityId();
      fullyQualifiedName = StringUtils.prependIfMissing(fullyQualifiedName, prefix);
      fixedResourceNameConfigsMap.put(fullyQualifiedName, fixedResourceNameConfig);
    }
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
      oneofConfigBuilder.put(oneofConfig.getEntityId(), oneofConfig);
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
          FieldConfigFactory.createMessageFieldConfig(
              messageConfig, resourceNameConfigs, field, ResourceNameTreatment.STATIC_TYPES));
    }
    builder.putAll(map);
    return builder.build();
  }

  /**
   * We should create ResourceNameConfigs only for resources that have one or more associated
   * message in the source protos we are generating GAPICs for, including:
   *
   * <ul>
   *   <li>resources defined at message-level on any message in source protos
   *   <li>resources referenced by any message in source protos through `type`
   *   <li>parent resources of those referenced by any message in source protos through `child_type`
   * </ul>
   */
  private static Map<String, ResourceNameConfig> getResourceNameConfigsFromAnnotation(
      DiagCollector diagCollector,
      TargetLanguage language,
      Map<String, ResourceDescriptorConfig> resourceDescriptorConfigs,
      Set<String> typesWithTypeReferences,
      Set<String> typesWithChildReferences,
      Map<String, DeprecatedCollectionConfigProto> deprecatedPatternResourceMap,
      Map<String, List<ResourceDescriptorConfig>> patternResourceDescriptorMap,
      Map<String, List<ResourceDescriptorConfig>> childParentResourceMap,
      String defaultPackage,
      Map<String, SingleResourceNameConfig> singleResourceNameConfigsFromGapicConfig) {
    HashMap<String, ResourceNameConfig> annotationResourceNameConfigs = new HashMap<>();
    for (ResourceDescriptorConfig resourceDescriptorConfig : resourceDescriptorConfigs.values()) {
      String unifiedResourceType = resourceDescriptorConfig.getUnifiedResourceType();

      if (resourceDescriptorConfig.isDefinedAtMessageLevel()
              && resourceDescriptorConfig
                  .getAssignedProtoFile()
                  .getProto()
                  .getPackage()
                  .equals(defaultPackage)
          || typesWithTypeReferences.contains(unifiedResourceType)) {
        Map<String, ResourceNameConfig> resources =
            resourceDescriptorConfig.buildResourceNameConfigs(
                diagCollector,
                singleResourceNameConfigsFromGapicConfig,
                deprecatedPatternResourceMap,
                language);
        annotationResourceNameConfigs.putAll(resources);
      }

      if (typesWithChildReferences.contains(unifiedResourceType)) {
        List<ResourceDescriptorConfig> parentResources =
            childParentResourceMap.getOrDefault(unifiedResourceType, Collections.emptyList());
        for (ResourceDescriptorConfig parentResource : parentResources) {
          Map<String, ResourceNameConfig> resources =
              parentResource.buildResourceNameConfigs(
                  diagCollector,
                  singleResourceNameConfigsFromGapicConfig,
                  deprecatedPatternResourceMap,
                  language);
          annotationResourceNameConfigs.putAll(resources);
        }
      }
    }
    return annotationResourceNameConfigs;
  }

  /** Returns the GapicInterfaceConfig for the given API interface. */
  public GapicInterfaceConfig getInterfaceConfig(Interface apiInterface) {
    return (GapicInterfaceConfig) getInterfaceConfigMap().get(apiInterface.getFullName());
  }

  /** Returns the InterfaceConfig for the given API interface. */
  @Override
  public InterfaceConfig getInterfaceConfig(InterfaceModel apiInterface) {
    return getInterfaceConfigMap().get(apiInterface.getFullName());
  }

  /** True if contains an InterfaceConfig for the specified API interface */
  @Override
  public boolean hasInterfaceConfig(InterfaceModel apiInterface) {
    return getInterfaceConfigMap().containsKey(apiInterface.getFullName());
  }

  /** Returns the InterfaceConfig for the given API interface. */
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
    if (resourceNameConfig instanceof SingleResourceNameConfig) {
      return (SingleResourceNameConfig) resourceNameConfig;
    }
    if (resourceNameConfig instanceof ResourceNameOneofConfig) {
      ResourceNameOneofConfig oneofConfig = (ResourceNameOneofConfig) resourceNameConfig;
      if (Iterables.size(oneofConfig.getSingleResourceNameConfigs()) > 0) {
        return Iterables.get(oneofConfig.getSingleResourceNameConfigs(), 0);
      }
    }
    return null;
  }

  /** Returns a base package name for an API's client. */
  @Nullable
  public static String getPackageName(Model model) {
    if (model.getServiceConfig().getApisCount() > 0) {
      Api api = model.getServiceConfig().getApis(0);
      Interface apiInterface = model.getSymbolTable().lookupInterface(api.getName());
      if (apiInterface != null) {
        return apiInterface.getFile().getFullName();
      }
    }
    return null;
  }

  public List<LongRunningConfig> getAllLongRunningConfigs() {
    return getInterfaceConfigMap()
        .values()
        .stream()
        .flatMap(i -> i.getMethodConfigs().stream())
        .map(MethodConfig::getLroConfig)
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  private static Map<CollectionConfigProto, Interface> getAllCollectionConfigProtos(
      @Nullable Model model, ConfigProto configProto) {
    Map<CollectionConfigProto, Interface> allCollectionConfigProtos =
        configProto
            .getCollectionsList()
            .stream()
            .collect(
                LinkedHashMap::new, (map, config) -> map.put(config, null), LinkedHashMap::putAll);
    configProto
        .getInterfacesList()
        .forEach(
            i ->
                i.getCollectionsList()
                    .forEach(
                        c ->
                            allCollectionConfigProtos.put(
                                c,
                                model == null
                                    ? null
                                    : model.getSymbolTable().lookupInterface(i.getName()))));
    return allCollectionConfigProtos;
  }
}
