/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.transformer;

import com.google.api.codegen.ConfigProto;
import com.google.api.codegen.config.DiscoInterfaceModel;
import com.google.api.codegen.configgen.viewmodel.ConfigView;
import com.google.api.codegen.configgen.viewmodel.InterfaceView;
import com.google.api.codegen.configgen.viewmodel.LanguageSettingView;
import com.google.api.codegen.configgen.viewmodel.LicenseView;
import com.google.api.codegen.configgen.viewmodel.ResourceNameGenerationView;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Generates the config view object using a model and output path. */
public class DiscoConfigTransformer {
  private static final String CONFIG_TEMPLATE_FILE = "configgen/gapic_config.snip";
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();
  private static final String DEFAULT_CONFIG_SCHEMA_VERSION = "1.0.0";

  private final LanguageTransformer languageTransformer = new LanguageTransformer();
  private final RetryTransformer retryTransformer = new RetryTransformer();
  private final CollectionTransformer collectionTransformer = new CollectionTransformer();
  private final MethodTransformer methodTransformer =
      new MethodTransformer(new DiscoveryMethodTransformer());

  public ViewModel generateConfig(Document model, String outputPath) {
    Map<Method, Name> methodToResourceName = new HashMap<>();
    ImmutableMap.Builder<Method, String> methodToNamePattern = ImmutableMap.builder();

    // Maps visited simple resource names to canonical name patterns. Used to check for collisions in simple resource names.
    Map<Name, String> simpleResourceToFirstPatternMap = new HashMap<>();
    for (Method method : model.methods()) {
      String namePattern = DiscoGapicNamer.getCanonicalPath(method);
      methodToNamePattern.put(method, namePattern);

      Name simpleResourceName = DiscoGapicNamer.getResourceIdentifier(method.flatPath());
      String collisionPattern = simpleResourceToFirstPatternMap.get(simpleResourceName);
      if (collisionPattern != null && !collisionPattern.equals(namePattern)) {
        // Collision with another path template with the same simple resource name; qualify this resource name.
        Name qualifiedResourceName = DiscoGapicNamer.getQualifiedResourceIdentifier(namePattern);
        methodToResourceName.put(method, qualifiedResourceName);
      } else {
        simpleResourceToFirstPatternMap.put(simpleResourceName, namePattern);
        methodToResourceName.put(method, simpleResourceName);
      }
    }

    // Map of base resource identifiers to all canonical name patterns that use that identifier.
    ImmutableMap<Method, Name> methodToResourceNames =
        ImmutableMap.<Method, Name>builder().putAll(methodToResourceName).build();

    return ConfigView.newBuilder()
        .templateFileName(CONFIG_TEMPLATE_FILE)
        .outputPath(outputPath)
        .type(CONFIG_PROTO_TYPE)
        .configSchemaVersion(DEFAULT_CONFIG_SCHEMA_VERSION)
        .languageSettings(generateLanguageSettings(model))
        .license(generateLicense())
        .interfaces(generateInterfaces(model, methodToNamePattern.build(), methodToResourceNames))
        .resourceNameGeneration(generateResourceNameGenerations(model, methodToResourceNames))
        .build();
  }

  private List<LanguageSettingView> generateLanguageSettings(Document model) {
    String packageName = getPackageName(model);
    Preconditions.checkNotNull(packageName, "No interface found.");
    return languageTransformer.generateLanguageSettings(packageName);
  }

  private String getPackageName(Document model) {
    String reverseDomain =
        Joiner.on(".").join(Lists.reverse(Arrays.asList(model.ownerDomain().split("\\."))));
    return String.format("%s.%s.%s", reverseDomain, model.name(), model.version());
  }

  private LicenseView generateLicense() {
    return LicenseView.newBuilder()
        .copyrightFile(CONFIG_DEFAULT_COPYRIGHT_FILE)
        .licenseFile(CONFIG_DEFAULT_LICENSE_FILE)
        .build();
  }

  private List<InterfaceView> generateInterfaces(
      Document model,
      Map<Method, String> methodToNamePatterns,
      Map<Method, Name> methodToResourceNames) {
    ImmutableList.Builder<InterfaceView> interfaces = ImmutableList.builder();
    for (String resource : model.resources().keySet()) {
      List<Method> interfaceMethods = model.resources().get(resource);

      Map<String, String> collectionNameMap =
          getResourceToEntityNameMap(interfaceMethods, methodToNamePatterns, methodToResourceNames);
      InterfaceView.Builder interfaceView = InterfaceView.newBuilder();

      String ownerName = model.ownerDomain().split("\\.")[0];
      String resourceName = Name.anyCamel(resource).toUpperCamel();
      interfaceView.name(
          String.format("%s.%s.%s.%s", ownerName, model.name(), model.version(), resourceName));

      retryTransformer.generateRetryDefinitions(
          interfaceView,
          ImmutableList.of("UNAVAILABLE", "DEADLINE_EXCEEDED"),
          ImmutableList.<String>of());
      interfaceView.collections(collectionTransformer.generateCollections(collectionNameMap));
      interfaceView.methods(
          methodTransformer.generateMethods(
              new DiscoInterfaceModel(resource, model), collectionNameMap));
      interfaces.add(interfaceView.build());
    }
    return interfaces.build();
  }

  /**
   * Examines all of the resource paths used by the methods, and returns a map from each unique
   * canonical resource path to a resource identifier (a short name used by the collection
   * configuration). Each resource path is merely a string describing the fields in the entity, and
   * the resource path might not be the same as the RPC endpoint URI. The resource identifier is
   * globally unique within each API. Many methods may use the same resource collection. The
   * resource identifier will be full qualified iff there are two or more resource identifiers with
   * different canonical resource paths.
   */
  private Map<String, String> getResourceToEntityNameMap(
      List<Method> interfaceMethods,
      Map<Method, String> methodToNamePatterns,
      Map<Method, Name> methodToResourceNames) {
    Map<String, String> resourceNameMap = new TreeMap<>();

    for (Method method : interfaceMethods) {
      resourceNameMap.put(
          methodToNamePatterns.get(method), methodToResourceNames.get(method).toLowerCamel());
    }
    return ImmutableMap.copyOf(resourceNameMap);
  }

  private List<ResourceNameGenerationView> generateResourceNameGenerations(
      Document model, Map<Method, Name> methodToResourceNameAndPatternMap) {
    ImmutableList.Builder<ResourceNameGenerationView> resourceNames = ImmutableList.builder();
    for (Map.Entry<String, List<Method>> resource : model.resources().entrySet()) {
      for (Method method : resource.getValue()) {
        if (!Strings.isNullOrEmpty(method.path())) {
          ResourceNameGenerationView.Builder view = ResourceNameGenerationView.newBuilder();
          view.messageName(DiscoGapicNamer.getRequestName(method).toUpperCamel());
          String parameterName =
              DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
          String resourceName = methodToResourceNameAndPatternMap.get(method).toLowerCamel();
          Map<String, String> fieldEntityMap = new HashMap<>();
          fieldEntityMap.put(parameterName, resourceName);
          view.fieldEntities(fieldEntityMap);

          resourceNames.add(view.build());
        }
      }
    }
    return resourceNames.build();
  }
}
