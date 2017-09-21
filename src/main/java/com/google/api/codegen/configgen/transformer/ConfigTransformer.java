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
import com.google.api.codegen.configgen.CollectionPattern;
import com.google.api.codegen.configgen.viewmodel.CommentView;
import com.google.api.codegen.configgen.viewmodel.ConfigFileView;
import com.google.api.codegen.configgen.viewmodel.ConfigView;
import com.google.api.codegen.configgen.viewmodel.InterfaceView;
import com.google.api.codegen.configgen.viewmodel.LanguageSettingView;
import com.google.api.codegen.configgen.viewmodel.LicenseView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.protobuf.Api;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Generates the config view object using a model and output path. */
public class ConfigTransformer {
  private static final String CONFIG_TEMPLATE_FILE = "configgen/gapic_config.snip";
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();
  private static final String CONFIG_COMMENT =
      "Address all the TODOs in this auto-generated config before using it for client generation. "
          + "Remove this paragraph after you closed all the TODOs. The retry_codes_name, "
          + "required_fields, flattening, and timeout properties cannot be precisely decided by "
          + "the tooling and may require some configuration.";

  private final LanguageTransformer languageTransformer = new LanguageTransformer();
  private final RetryTransformer retryTransformer = new RetryTransformer();
  private final CollectionTransformer collectionTransformer = new CollectionTransformer();
  private final MethodTransformer methodTransformer = new MethodTransformer();

  public ViewModel generateConfig(Model model, String outputPath) {
    return ConfigFileView.newBuilder()
        .templateFileName(CONFIG_TEMPLATE_FILE)
        .outputPath(outputPath)
        .config(generateConfigComment(model))
        .build();
  }

  public CommentView<ConfigView> generateConfigComment(Model model) {
    return CommentView.<ConfigView>newBuilder()
        .comment(CONFIG_COMMENT)
        .value(
            ConfigView.newBuilder()
                .type(CONFIG_PROTO_TYPE)
                .languageSettings(generateLanguageSettings(model))
                .license(generateLicense())
                .interfaces(generateInterfaces(model))
                .build())
        .build();
  }

  private List<LanguageSettingView> generateLanguageSettings(Model model) {
    String packageName = getPackageName(model);
    Preconditions.checkNotNull(packageName, "No interface found.");
    return languageTransformer.generateLanguageSettings(packageName);
  }

  private String getPackageName(Model model) {
    if (model.getServiceConfig().getApisCount() <= 0) {
      return null;
    }

    Api api = model.getServiceConfig().getApis(0);
    Interface apiInterface = model.getSymbolTable().lookupInterface(api.getName());
    return apiInterface.getFile().getFullName();
  }

  private LicenseView generateLicense() {
    return LicenseView.newBuilder()
        .copyrightFile(CONFIG_DEFAULT_COPYRIGHT_FILE)
        .licenseFile(CONFIG_DEFAULT_LICENSE_FILE)
        .build();
  }

  private List<InterfaceView> generateInterfaces(Model model) {
    ImmutableList.Builder<InterfaceView> interfaces = ImmutableList.builder();
    for (Api api : model.getServiceConfig().getApisList()) {
      Interface apiInterface = model.getSymbolTable().lookupInterface(api.getName());
      Map<String, String> collectionNameMap = getResourceToEntityNameMap(apiInterface.getMethods());
      InterfaceView.Builder interfaceView = InterfaceView.newBuilder();
      interfaceView.name(apiInterface.getFullName());
      retryTransformer.generateRetryDefinitions(interfaceView);
      interfaceView.collections(collectionTransformer.generateCollections(collectionNameMap));
      interfaceView.methods(methodTransformer.generateMethods(apiInterface, collectionNameMap));
      interfaces.add(interfaceView.build());
    }
    return interfaces.build();
  }

  /**
   * Examines all of the resource paths used by the methods, and returns a map from each unique
   * resource paths to a short name used by the collection configuration.
   */
  private Map<String, String> getResourceToEntityNameMap(List<Method> methods) {
    // Using a map with the string representation of the resource path to avoid duplication
    // of equivalent paths.
    // Using a TreeMap in particular so that the ordering is deterministic
    // (useful for testability).
    Map<String, CollectionPattern> specs = new TreeMap<>();
    for (Method method : methods) {
      for (CollectionPattern collectionPattern :
          CollectionPattern.getCollectionPatternsFromMethod(method)) {
        String resourcePath = collectionPattern.getTemplatizedResourcePath();
        // If there are multiple field segments with the same resource path, the last
        // one will be used, making the output deterministic. Also, the first field path
        // encountered tends to be simply "name" because it is the corresponding create
        // API method for the type.
        specs.put(resourcePath, collectionPattern);
      }
    }

    Set<String> usedNameSet = new HashSet<>();
    ImmutableMap.Builder<String, String> nameMapBuilder = ImmutableMap.builder();
    for (CollectionPattern collectionPattern : specs.values()) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      String entityNameString = collectionPattern.getUniqueName(usedNameSet);
      usedNameSet.add(entityNameString);
      nameMapBuilder.put(resourceNameString, entityNameString);
    }
    return nameMapBuilder.build();
  }
}
