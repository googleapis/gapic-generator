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
import com.google.api.codegen.config.TransportProtocol;
import com.google.api.codegen.configgen.viewmodel.ConfigView;
import com.google.api.codegen.configgen.viewmodel.InterfaceView;
import com.google.api.codegen.configgen.viewmodel.LanguageSettingView;
import com.google.api.codegen.configgen.viewmodel.LicenseView;
import com.google.api.codegen.discogapic.transformer.DiscoGapicNamer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.discovery.Method;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Generates the config view object using a model and output path. */
public class DiscoConfigTransformer {
  private static final String CONFIG_TEMPLATE_FILE = "configgen/gapic_config.snip";
  private static final String CONFIG_DEFAULT_COPYRIGHT_FILE = "copyright-google.txt";
  private static final String CONFIG_DEFAULT_LICENSE_FILE = "license-header-apache-2.0.txt";
  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();

  private final LanguageTransformer languageTransformer = new LanguageTransformer();
  private final RetryTransformer retryTransformer = new RetryTransformer();
  private final CollectionTransformer collectionTransformer = new CollectionTransformer();
  private final MethodTransformer methodTransformer = new MethodTransformer();

  public ViewModel generateConfig(Document model, String outputPath) {
    return ConfigView.newBuilder()
        .templateFileName(CONFIG_TEMPLATE_FILE)
        .outputPath(outputPath)
        .type(CONFIG_PROTO_TYPE)
        .languageSettings(generateLanguageSettings(model))
        .license(generateLicense())
        .interfaces(generateInterfaces(model))
        .build();
  }

  private List<LanguageSettingView> generateLanguageSettings(Document model) {
    String packageName = getPackageName(model);
    Preconditions.checkNotNull(packageName, "No interface found.");
    return languageTransformer.generateLanguageSettings(packageName);
  }

  private String getPackageName(Document model) {
    //    // TODO(andrealin): what about non-java?
    //    return String.format("com.google.cloud.%s.%s", model.name(), model.version());
    //    Api api = model.getServiceConfig().getApis(0);
    //    Interface apiInterface = model.getSymbolTable().lookupInterface(api.getName());
    return model.name();
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
      Map<String, String> collectionNameMap = getResourceToEntityNameMap(resource.getValue());
      InterfaceView.Builder interfaceView = InterfaceView.newBuilder();

      String ownerName = model.ownerDomain().split("\\.", 1)[0];
      interfaceView.name(String.format("%s.%s.%s", ownerName, resource.getKey(), model.version()));

      retryTransformer.generateRetryDefinitions(interfaceView, TransportProtocol.HTTP);
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
   * resource paths to a short name used by the collection configuration.
   */
  private Map<String, String> getResourceToEntityNameMap(List<Method> methods) {
    Map<String, String> resourceNameMap = new TreeMap<>();
    for (Method method : methods) {
      resourceNameMap.put(
          method.flatPath(), DiscoGapicNamer.getResourceIdentifier(method).toLowerCamel());
    }
    return ImmutableMap.copyOf(resourceNameMap);
  }
}
