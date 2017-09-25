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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Generates the config view object using a model and output path. */
public class DiscoConfigTransformer {
  private static final String CONFIG_TEMPLATE_FILE = "configgen/gapic_config.snip";
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();
  private static final Pattern UNBRACKETED_PATH_SEGMENTS_PATTERN =
      Pattern.compile("\\}/((?:[a-zA-Z]+/){2,})\\{");

  private final LanguageTransformer languageTransformer = new LanguageTransformer();
  private final RetryTransformer retryTransformer = new RetryTransformer();
  private final CollectionTransformer collectionTransformer = new CollectionTransformer();
  private final MethodTransformer methodTransformer =
      new MethodTransformer(new DiscoveryMethodTransformer());

  public ViewModel generateConfig(Document model, String outputPath) {
    return ConfigView.newBuilder()
        .templateFileName(CONFIG_TEMPLATE_FILE)
        .outputPath(outputPath)
        .type(CONFIG_PROTO_TYPE)
        .languageSettings(generateLanguageSettings(model))
        .license(generateLicense())
        .interfaces(generateInterfaces(model))
        .resourceNameGeneration(generateResourceNameGenerations(model))
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

  private List<InterfaceView> generateInterfaces(Document model) {
    ImmutableList.Builder<InterfaceView> interfaces = ImmutableList.builder();
    for (Map.Entry<String, List<Method>> resource : model.resources().entrySet()) {
      Map<String, String> collectionNameMap =
          getResourceToEntityNameMap(resource.getKey(), resource.getValue());
      InterfaceView.Builder interfaceView = InterfaceView.newBuilder();

      String ownerName = model.ownerDomain().split("\\.")[0];
      String resourceName = Name.anyCamel(resource.getKey()).toUpperCamel();
      interfaceView.name(
          String.format("%s.%s.%s.%s", ownerName, model.name(), model.version(), resourceName));

      retryTransformer.generateRetryDefinitions(
          interfaceView,
          ImmutableList.of("SC_SERVICE_UNAVAILABLE", "SC_GATEWAY_TIMEOUT"),
          ImmutableList.<String>of());
      interfaceView.collections(collectionTransformer.generateCollections(collectionNameMap));
      interfaceView.methods(
          methodTransformer.generateMethods(
              new DiscoInterfaceModel(resource.getKey(), model), collectionNameMap));
      interfaces.add(interfaceView.build());
    }
    return interfaces.build();
  }

  /**
   * Examines all of the resource paths used by the methods, and returns a map from each unique
   * resource paths to a short name used by the collection configuration. Each resource path is
   * merely a string describing the fields in the entity, and the resource path may not be the same
   * as the RPC endpoint URI.
   */
  private Map<String, String> getResourceToEntityNameMap(
      String parentResource, List<Method> methods) {
    Map<String, String> resourceNameMap = new TreeMap<>();
    for (Method method : methods) {
      String namePattern = method.flatPath();
      // Escape the first character of the pattern if necessary.
      namePattern = namePattern.charAt(0) == '{' ? "\\".concat(namePattern) : namePattern;
      // Remove any trailing non-bracketed substring
      if (!namePattern.endsWith("}") && namePattern.contains("}")) {
        namePattern = namePattern.substring(0, namePattern.lastIndexOf('}') + 1);
      }
      // For each sequence of consecutive non-bracketed path segments,
      // replace those segments with the last one in the sequence.
      Matcher m = UNBRACKETED_PATH_SEGMENTS_PATTERN.matcher(namePattern);
      if (m.find()) {
        StringBuffer sb = new StringBuffer();
        for (int i = 1; i <= m.groupCount(); i++) {
          String multipleSegment = m.group(i);
          String[] segmentPieces = multipleSegment.split("/");
          Name segment = Name.anyCamel(segmentPieces[segmentPieces.length - 1]);
          m.appendReplacement(sb, String.format("}/%s/{", segment.toLowerCamel()));
        }
        namePattern = m.appendTail(sb).toString();
      }
      resourceNameMap.put(
          namePattern,
          DiscoGapicNamer.getQualifiedResourceIdentifier(method, parentResource).toLowerCamel());
    }
    return ImmutableMap.copyOf(resourceNameMap);
  }

  private List<ResourceNameGenerationView> generateResourceNameGenerations(Document model) {
    ImmutableList.Builder<ResourceNameGenerationView> resourceNames = ImmutableList.builder();
    for (Map.Entry<String, List<Method>> resource : model.resources().entrySet()) {
      for (Method method : resource.getValue()) {
        if (!Strings.isNullOrEmpty(method.path())) {
          ResourceNameGenerationView.Builder view = ResourceNameGenerationView.newBuilder();
          view.messageName(DiscoGapicNamer.getRequestName(method).toUpperCamel());

          String parameterName =
              DiscoGapicNamer.getResourceIdentifier(method.flatPath()).toLowerCamel();
          String qualifiedResourceName =
              DiscoGapicNamer.getQualifiedResourceIdentifier(method, resource.getKey())
                  .toLowerCamel();

          Map<String, String> fieldEntityMap = new HashMap<>();
          fieldEntityMap.put(parameterName, qualifiedResourceName);
          view.fieldEntities(fieldEntityMap);

          resourceNames.add(view.build());
        }
      }
    }
    return resourceNames.build();
  }
}
