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

import com.google.api.codegen.ConfigProto;
import com.google.api.tools.framework.aspects.context.ContextConfigAspect;
import com.google.api.tools.framework.aspects.documentation.DocumentationConfigAspect;
import com.google.api.tools.framework.aspects.http.HttpConfigAspect;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.stages.Merged;
import com.google.api.tools.framework.processors.merger.Merger;
import com.google.api.tools.framework.processors.resolver.Resolver;
import com.google.api.tools.framework.tools.ToolDriverBase;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Main class for the config generator. */
public class ConfigGeneratorApi extends ToolDriverBase {

  public static final Option<String> OUTPUT_FILE =
      ToolOptions.createOption(
          String.class, "output_file", "The path of the output file to put generated config.", "");

  private static final String CONFIG_KEY_TYPE = "type";
  private static final String CONFIG_KEY_GENERATE_SAMPLES = "generate_samples";
  private static final String CONFIG_KEY_LANGUAGE_SETTINGS = "language_settings";
  private static final String CONFIG_KEY_INTERFACES = "interfaces";

  private static final String CONFIG_KEY_SERVICE_NAME = "name";
  private static final String CONFIG_KEY_METHOD_NAME = "name";
  private static final String CONFIG_KEY_METHODS = "methods";
  private static final String CONFIG_KEY_COLLECTIONS = "collections";

  private static final String CONFIG_KEY_NAME_PATTERN = "name_pattern";
  private static final String CONFIG_KEY_ENTITY_NAME = "entity_name";

  private static final String CONFIG_PROTO_TYPE = ConfigProto.getDescriptor().getFullName();

  private static final String CONFIG_KEY_TIMEOUT = "timeout_millis";
  private static final int CONFIG_VALUE_DEFAULT_TIMEOUT = 30000;

  /** Constructs a config generator api based on given options. */
  public ConfigGeneratorApi(ToolOptions options) {
    super(options);
  }

  @Override
  protected void registerProcessors() {
    model.registerProcessor(new Resolver());
    model.registerProcessor(new Merger());
  }

  @Override
  protected void registerAspects() {
    model.registerConfigAspect(DocumentationConfigAspect.create(model));
    model.registerConfigAspect(ContextConfigAspect.create(model));
    model.registerConfigAspect(HttpConfigAspect.create(model));
  }

  @Override
  protected void process() throws Exception {
    model.establishStage(Merged.KEY);

    Map<String, Object> output = new LinkedHashMap<String, Object>();
    output.put(CONFIG_KEY_TYPE, CONFIG_PROTO_TYPE);
    output.put(CONFIG_KEY_GENERATE_SAMPLES, true);
    output.put(CONFIG_KEY_LANGUAGE_SETTINGS, generateLanguageSettings());
    output.put(CONFIG_KEY_INTERFACES, generateInterfacesConfig());
    dump(output);
  }

  /** Generates a collection configurations section. */
  private static List<Object> generateCollectionConfigs(Map<String, String> nameMap) {
    List<Object> output = new LinkedList<Object>();
    for (String resourceNameString : nameMap.keySet()) {
      Map<String, Object> collectionMap = new LinkedHashMap<String, Object>();
      collectionMap.put(CONFIG_KEY_NAME_PATTERN, resourceNameString);
      collectionMap.put(CONFIG_KEY_ENTITY_NAME, nameMap.get(resourceNameString));
      output.add(collectionMap);
    }
    return output;
  }

  private List<Object> generateMethodConfigs(
      Interface service, Map<String, String> collectionConfigNameMap) {
    List<MethodConfigGenerator> methodConfigGenerators =
        Arrays.asList(
            new FieldConfigGenerator(),
            new PageStreamingConfigGenerator(),
            new RetryGenerator(),
            new FieldNamePatternConfigGenerator(collectionConfigNameMap),
            new MethodConfigGenerator() {
              @Override
              public Map<String, Object> generate(Method method) {
                return ImmutableMap.of(CONFIG_KEY_TIMEOUT, (Object) CONFIG_VALUE_DEFAULT_TIMEOUT);
              }
            });
    List<Object> methods = new LinkedList<Object>();
    for (Method method : service.getMethods()) {
      Map<String, Object> methodConfig = new LinkedHashMap<String, Object>();
      methodConfig.put(CONFIG_KEY_METHOD_NAME, method.getSimpleName());
      for (MethodConfigGenerator generator : methodConfigGenerators) {
        Map<String, Object> config = generator.generate(method);
        if (config != null) {
          methodConfig.putAll(config);
        }
      }
      methods.add(methodConfig);
    }
    return methods;
  }

  private List<Object> generateInterfacesConfig() {
    List<Object> services = new LinkedList<Object>();
    for (Interface service : model.getSymbolTable().getInterfaces()) {
      Map<String, Object> serviceConfig = new LinkedHashMap<String, Object>();
      Map<String, String> collectionNameMap = getResourceToEntityNameMap(service.getMethods());
      serviceConfig.put(CONFIG_KEY_SERVICE_NAME, service.getFullName());
      serviceConfig.put(CONFIG_KEY_COLLECTIONS, generateCollectionConfigs(collectionNameMap));
      serviceConfig.putAll(RetryGenerator.generateRetryDefinitions());
      serviceConfig.put(CONFIG_KEY_METHODS, generateMethodConfigs(service, collectionNameMap));
      services.add(serviceConfig);
    }
    return services;
  }

  private Map<String, Object> generateLanguageSettings() {
    int index =
        Preconditions.checkPositionIndex(model.getFiles().size() - 1, model.getFiles().size());
    String packageName = model.getFiles().get(index).getFullName();
    return LanguageGenerator.generate(packageName);
  }

  /**
   * Examines all of the resource paths used by the methods, and returns a map from each unique
   * resource paths to a short name used by the collection configuration.
   */
  public static ImmutableMap<String, String> getResourceToEntityNameMap(List<Method> methods) {
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
    ImmutableMap.Builder<String, String> nameMapBuilder = ImmutableMap.<String, String>builder();
    for (CollectionPattern collectionPattern : specs.values()) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      String entityNameString = collectionPattern.getUniqueName(usedNameSet);
      usedNameSet.add(entityNameString);
      nameMapBuilder.put(resourceNameString, entityNameString);
    }
    return nameMapBuilder.build();
  }

  private void dump(Map<String, Object> data) throws IOException {
    DumperOptions dumperOptions = new DumperOptions();
    dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    dumperOptions.setPrettyFlow(false);
    Yaml yaml = new Yaml(dumperOptions);
    String generatedConfig = yaml.dump(data);

    Path path = Paths.get(options.get(OUTPUT_FILE));
    Files.write(generatedConfig.getBytes(), path.toFile());
  }
}
