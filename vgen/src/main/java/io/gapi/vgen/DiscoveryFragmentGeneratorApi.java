package io.gapi.vgen;

import com.google.api.tools.framework.model.DiagCollector;
import com.google.api.tools.framework.model.testing.SimpleDiag;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolOptions.Option;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.TypeLiteral;
import com.google.protobuf.Message;
import com.google.protobuf.Method;

import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

/**
 * Main class for the discovery doc fragment generator.
 */
public class DiscoveryFragmentGeneratorApi {
  public static final Option<String> DISCOVERY_DOC = ToolOptions.createOption(
      String.class,
      "discovery_doc",
      "The discovery doc representing the service description.",
      "");

  public static final Option<String> OUTPUT_FILE = ToolOptions.createOption(
      String.class,
      "output_file",
      "The name of the output file or folder to put generated code.",
      "");

  public static final Option<List<String>> GENERATOR_CONFIG_FILES = ToolOptions.createOption(
      new TypeLiteral<List<String>>(){},
      "config_files",
      "The list of Yaml configuration files for the fragment generator.",
      ImmutableList.<String>of());

  protected final ToolOptions options;
  private final String dataPath;

  /**
   * Constructs a discovery doc fragment generator API based on given options.
   */
  public DiscoveryFragmentGeneratorApi(ToolOptions options) {
    this.options = options;
    this.dataPath = getDataPath();
  }

  protected void process() throws Exception {

    DiscoveryImporter discovery =
        DiscoveryImporter.parse(
            com.google.common.io.Files.newReader(
                new File(options.get(DISCOVERY_DOC)), Charset.forName("UTF8")));

    // Read the yaml config and convert it to proto.
    List<String> configFileNames = options.get(GENERATOR_CONFIG_FILES);
    if (configFileNames.size() == 0) {
      error(String.format("--%s must be provided", GENERATOR_CONFIG_FILES.name()));
      return;
    }

    ConfigProto configProto = loadConfigFromFiles(configFileNames);
    if (configProto == null) {
      return;
    }

    DiscoveryFragmentGenerator generator =
        (DiscoveryFragmentGenerator) new DiscoveryGenerator.Builder()
            .setConfigProto(configProto)
            .setDiscovery(discovery)
            .build();
    if (generator == null) {
      return;
    }

    Multimap<Method, GeneratedResult> docs = ArrayListMultimap.create();
    for (String snippetInputName : configProto.getFragmentFilesList()) {
      SnippetDescriptor snippetDescriptor =
          new SnippetDescriptor(snippetInputName);
      Map<Method, GeneratedResult> code = generator.generateFragments(snippetDescriptor);
      if (code == null) {
        continue;
      }
      for (Map.Entry<Method, GeneratedResult> entry : code.entrySet()) {
        docs.put(entry.getKey(), entry.getValue());
      }
    }
    generator.outputFragments(options.get(OUTPUT_FILE), docs, configProto.getArchive());
  }

  public void run() throws Exception {
    process();
  }

  private ConfigProto loadConfigFromFiles(List<String> configFileNames) {
    List<File> configFiles = pathsToFiles(configFileNames);
    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(ConfigProto.getDescriptor().getFullName(),
            ConfigProto.getDefaultInstance());
    // Use DiagCollector to collect errors from config read since user errors may arise here
    DiagCollector diagCollector = new SimpleDiag();
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
        error("Cannot find configuration file '%s'.", configFileName);
        continue;
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
