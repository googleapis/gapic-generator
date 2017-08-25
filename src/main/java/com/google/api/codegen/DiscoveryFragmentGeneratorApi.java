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
package com.google.api.codegen;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.codegen.discovery.DiscoveryProvider;
import com.google.api.codegen.discovery.DiscoveryProviderFactory;
import com.google.api.codegen.util.ClassInstantiator;
import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.SimpleDiagCollector;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.TypeLiteral;
import com.google.protobuf.Api;
import com.google.protobuf.Message;
import com.google.protobuf.Method;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/** Main class for the discovery doc fragment generator. */
public class DiscoveryFragmentGeneratorApi {
  public static final Option<String> DISCOVERY_DOC =
      ToolOptions.createOption(
          String.class,
          "discovery_doc",
          "The Discovery doc representing the service description.",
          "");

  public static final Option<String> OUTPUT_FILE =
      ToolOptions.createOption(
          String.class,
          "output_file",
          "The name of the output file or folder to put generated code.",
          "");

  public static final Option<String> OVERRIDE_FILES =
      ToolOptions.createOption(
          String.class,
          "overrides",
          "A comma delimited list of paths to sample config override files.",
          "");

  public static final Option<List<String>> GENERATOR_CONFIG_FILES =
      ToolOptions.createOption(
          new TypeLiteral<List<String>>() {},
          "config_files",
          "The list of YAML configuration files for the fragment generator.",
          ImmutableList.<String>of());

  public static final Option<String> AUTH_INSTRUCTIONS_URL =
      ToolOptions.createOption(
          String.class,
          "auth_instructions",
          "An @-delimited map of language to auth instructions URL: lang:URL@lang:URL@...",
          "");

  public static final Option<String> RUBY_NAMES_FILE =
      ToolOptions.createOption(String.class, "ruby_names_file", "A Ruby names file.", "");

  private final ToolOptions options;
  private final String dataPath;

  /** Constructs a discovery doc fragment generator API based on given options. */
  public DiscoveryFragmentGeneratorApi(ToolOptions options) {
    this.options = options;
    this.dataPath = getDataPath();
  }

  protected void process() throws Exception {
    DiscoveryImporter discovery =
        DiscoveryImporter.parse(
            com.google.common.io.Files.newReader(
                new File(options.get(DISCOVERY_DOC)), Charset.forName("UTF8")));

    // Read the YAML config and convert it to proto.
    List<String> configFileNames = options.get(GENERATOR_CONFIG_FILES);
    if (configFileNames.size() == 0) {
      error(String.format("--%s must be provided", GENERATOR_CONFIG_FILES.name()));
      return;
    }

    ConfigProto configProto = loadConfigFromFiles(configFileNames);
    if (configProto == null) {
      return;
    }

    String[] filenames = options.get(OVERRIDE_FILES).split(",");
    List<JsonNode> overrides = new ArrayList<>();
    for (String filename : filenames) {
      try {
        BufferedReader reader =
            com.google.common.io.Files.newReader(new File(filename), Charset.forName("UTF8"));
        ObjectMapper mapper = new ObjectMapper();
        overrides.add(mapper.readTree(reader));
      } catch (FileNotFoundException e) {
        // Do nothing if the overrides file doesn't exist. Avoiding crashes for
        // this scenario makes parts of the automation around samplegen simpler.
      }
    }

    GeneratorProto generator = configProto.getGenerator();

    String factory = generator.getFactory();
    String id = generator.getId();

    String authInstructions = options.get(AUTH_INSTRUCTIONS_URL);
    ApiaryConfig apiaryConfig = discovery.getConfig();
    apiaryConfig.setAuthInstructionsUrl(parseAuthInstructionsUrl(authInstructions, id));

    String rubyNamesFile = options.get(RUBY_NAMES_FILE);

    DiscoveryProviderFactory providerFactory = createProviderFactory(factory);
    DiscoveryProvider provider =
        providerFactory.create(discovery.getService(), apiaryConfig, overrides, rubyNamesFile, id);

    for (Api api : discovery.getService().getApisList()) {
      for (Method method : api.getMethodsList()) {
        Map<String, Doc> files = provider.generate(method);
        ToolUtil.writeFiles(files, options.get(OUTPUT_FILE));
      }
    }
  }

  /*
   * Parses strings of the format
   * "go:https://www.hello.com@java:https://www.world.com" and returns the value
   * of the key corresponding to the key id.
   */
  private String parseAuthInstructionsUrl(String authInstructionsUrl, String id) {
    if (Strings.isNullOrEmpty(authInstructionsUrl)) {
      return "";
    }
    // Split on '@'
    Iterable<String> it = Splitter.on('@').split(authInstructionsUrl);
    for (String item : it) {
      // Split on only the first ':' and return if there are two values and the
      // language matches.
      String[] pair = item.split(":", 2);
      if (pair[0].equals(id) && pair.length == 2) {
        return pair[1];
      }
    }
    return "";
  }

  private static DiscoveryProviderFactory createProviderFactory(String factory) {
    DiscoveryProviderFactory provider =
        ClassInstantiator.createClass(
            factory,
            DiscoveryProviderFactory.class,
            new Class<?>[] {},
            new Object[] {},
            "generator",
            new ClassInstantiator.ErrorReporter() {
              @Override
              public void error(String message, Object... args) {
                System.err.printf(message, args);
              }
            });
    return provider;
  }

  public void run() throws Exception {
    process();
  }

  private ConfigProto loadConfigFromFiles(List<String> configFileNames) {
    List<File> configFiles = pathsToFiles(configFileNames);
    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(
            ConfigProto.getDescriptor().getFullName(), ConfigProto.getDefaultInstance());
    // Use DiagCollector to collect errors from config read since user errors may arise here
    DiagCollector diagCollector = new SimpleDiagCollector();
    ConfigProto configProto =
        (ConfigProto) MultiYamlReader.read(diagCollector, configFiles, supportedConfigTypes);
    if (diagCollector.getErrorCount() > 0) {
      System.err.println(diagCollector.toString());
      return null;
    } else {
      return configProto;
    }
  }

  private List<File> pathsToFiles(List<String> configFileNames) {
    List<File> files = new ArrayList<>();

    for (String configFileName : configFileNames) {
      File file = findDataFile(configFileName);
      if (file == null) {
        throw new IllegalArgumentException(
            String.format("Cannot find configuration file '%s'.", configFileName));
      }
      files.add(file);
    }

    return files;
  }

  private String getDataPath() {
    List<String> defaults = new ArrayList<>();
    String joined = Joiner.on(File.pathSeparator).join(defaults);
    String option = options.get(ToolOptions.DATA_PATH);
    if (Strings.isNullOrEmpty(joined)) {
      return option;
    } else if (Strings.isNullOrEmpty(option)) {
      return joined;
    } else {
      return option + File.pathSeparator + joined;
    }
  }

  @Nullable
  public File findDataFile(String name) {
    Path file = Paths.get(name);
    if (file.isAbsolute()) {
      return Files.exists(file) ? file.toFile() : null;
    }
    for (String path : Splitter.on(File.pathSeparator).split(dataPath)) {
      file = Paths.get(path, name);
      if (Files.exists(file)) {
        return file.toFile();
      }
    }
    return null;
  }

  private void error(String message, Object... args) {
    System.err.printf(message, args);
  }
}
