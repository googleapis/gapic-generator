/* Copyright 2017 Google Inc
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
import com.google.common.collect.*;
import java.util.*;

/** Generates the config view object using a model and output path. */
public class DiscoConfigTransformer {
  private static final String CONFIG_TEMPLATE_FILE = "configgen/gapic_config.snip";
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();

  private final LanguageTransformer languageTransformer = new LanguageTransformer();
  private final RetryTransformer retryTransformer = new RetryTransformer();
  private final CollectionTransformer collectionTransformer = new CollectionTransformer();
  private final MethodTransformer methodTransformer =
      new MethodTransformer(new DiscoveryMethodTransformer());

  public ViewModel generateConfig(Document model, String outputPath) {
    ImmutableSetMultimap.Builder<String, String> resourceToNamePatternMapBuilder =
        ImmutableSetMultimap.builder();
    ImmutableMap.Builder<Method, String> methodToNamePatternMapBuilder = ImmutableMap.builder();

    for (Method method : model.methods()) {
      String namePattern = DiscoGapicNamer.getCanonicalPath(method);
      String simpleResourceName =
          DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
      resourceToNamePatternMapBuilder.put(simpleResourceName, namePattern);
      methodToNamePatternMapBuilder.put(method, namePattern);
    }

    // Map of base resource identifiers to all canonical name patterns that use that identifier.
    ImmutableSetMultimap<String, String> resourceToNamePatternMap =
        resourceToNamePatternMapBuilder.build();
    ImmutableMap<Method, String> methodToNamePatternMap = methodToNamePatternMapBuilder.build();

    return ConfigView.newBuilder()
        .templateFileName(CONFIG_TEMPLATE_FILE)
        .outputPath(outputPath)
        .type(CONFIG_PROTO_TYPE)
        .languageSettings(generateLanguageSettings(model))
        .license(generateLicense())
        .interfaces(generateInterfaces(model, resourceToNamePatternMap, methodToNamePatternMap))
        .resourceNameGeneration(generateResourceNameGenerations(model, resourceToNamePatternMap))
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
      ImmutableSetMultimap<String, String> resourceToNamePatternMap,
      ImmutableMap<Method, String> methodToNamePatternMap) {
    ImmutableList.Builder<InterfaceView> interfaces = ImmutableList.builder();
    for (String resource : model.resources().keySet()) {
      List<Method> interfaceMethods = model.resources().get(resource);

      Map<String, String> collectionNameMap =
          getResourceToEntityNameMap(
              resource, interfaceMethods, resourceToNamePatternMap, methodToNamePatternMap);
      InterfaceView.Builder interfaceView = InterfaceView.newBuilder();

      String ownerName = model.ownerDomain().split("\\.")[0];
      String resourceName = Name.anyCamel(resource).toUpperCamel();
      interfaceView.name(
          String.format("%s.%s.%s.%s", ownerName, model.name(), model.version(), resourceName));

      retryTransformer.generateRetryDefinitions(
          interfaceView,
          ImmutableList.of("SC_SERVICE_UNAVAILABLE", "SC_GATEWAY_TIMEOUT"),
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
   * Get the resource name for a method. Qualifies the resource name if it clashes with another
   * resource with the same name but different canonical path.
   */
  private String getResourceIdentifier(
      Method method, String parentName, SetMultimap<String, String> resourceToNamePatternMap) {
    String resourceName = DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
    if (resourceToNamePatternMap.get(resourceName).size() == 1) {
      return resourceName;
    } else {
      // Qualify resource name to avoid naming clashes with other methods with same name pattern.
      return DiscoGapicNamer.getQualifiedResourceIdentifier(method, parentName).toLowerCamel();
    }
  }

  /**
   * Examines all of the resource paths used by the methods, and returns a map from each unique
   * canonical resource path to a resource identifier (a short name used by the collection
   * configuration). Each resource path is merely a string describing the fields in the entity, and
   * the resource path might not be the same as the RPC endpoint URI. The resource identifier is
   * globally unique within each API. Many methods may use the same resource collection. The
   * resource identifier will be qualified (with the name of a parent resource) iff there are two
   * resource identifiers with different canonical resource paths.
   */
  private Map<String, String> getResourceToEntityNameMap(
      String parentResource,
      List<Method> interfaceMethods,
      SetMultimap<String, String> resourceToNamePatternMap,
      Map<Method, String> methodToNamePatternMap) {
    Map<String, String> resourceNameMap = new TreeMap<>();

    for (Method method : interfaceMethods) {
      resourceNameMap.put(
          methodToNamePatternMap.get(method),
          getResourceIdentifier(method, parentResource, resourceToNamePatternMap));
    }
    return ImmutableMap.copyOf(resourceNameMap);
  }

  private List<ResourceNameGenerationView> generateResourceNameGenerations(
      Document model, SetMultimap<String, String> resourceToNamePatternMap) {
    ImmutableList.Builder<ResourceNameGenerationView> resourceNames = ImmutableList.builder();
    for (Map.Entry<String, List<Method>> resource : model.resources().entrySet()) {
      for (Method method : resource.getValue()) {
        if (!Strings.isNullOrEmpty(method.path())) {
          ResourceNameGenerationView.Builder view = ResourceNameGenerationView.newBuilder();
          view.messageName(DiscoGapicNamer.getRequestName(method).toUpperCamel());
          String parameterName =
              DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
          String resourceName =
              getResourceIdentifier(method, resource.getKey(), resourceToNamePatternMap);
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
