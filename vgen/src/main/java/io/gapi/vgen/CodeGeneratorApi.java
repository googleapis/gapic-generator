package io.gapi.vgen;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import com.google.inject.TypeLiteral;
import com.google.protobuf.Message;

import io.gapi.fx.aspects.context.ContextConfigAspect;
import io.gapi.fx.aspects.documentation.DocumentationConfigAspect;
import io.gapi.fx.aspects.http.HttpConfigAspect;
import io.gapi.fx.aspects.naming.NamingConfigAspect;
import io.gapi.fx.aspects.system.SystemConfigAspect;
import io.gapi.fx.aspects.versioning.VersionConfigAspect;
import io.gapi.fx.aspects.visibility.VisibilityConfigAspect;
import io.gapi.fx.model.Diag;
import io.gapi.fx.model.Interface;
import io.gapi.fx.model.SimpleLocation;
import io.gapi.fx.processors.linter.Linter;
import io.gapi.fx.processors.merger.Merger;
import io.gapi.fx.processors.normalizer.Normalizer;
import io.gapi.fx.processors.resolver.Resolver;
import io.gapi.fx.tools.ToolBase;
import io.gapi.fx.tools.ToolOptions;
import io.gapi.fx.tools.ToolOptions.Option;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Main class for the code generator.
 */
public class CodeGeneratorApi extends ToolBase {

  public static final Option<String> OUTPUT_FILE = ToolOptions.createOption(
      String.class,
      "output_file",
      "The name of the output file or folder to put generated code.",
      "");

  public static final Option<List<String>> GENERATOR_CONFIG_FILES = ToolOptions.createOption(
      new TypeLiteral<List<String>>(){},
      "config_files",
      "The list of Yaml configuration files for the code generator.",
      ImmutableList.<String>of());

  /**
   * Constructs a code generator api based on given options.
   */
  public CodeGeneratorApi(ToolOptions options) {
    super(options);
  }

  @Override
  protected void registerProcessors() {
    model.registerProcessor(new Resolver());
    model.registerProcessor(new Merger());
    model.registerProcessor(new Normalizer());
    model.registerProcessor(new Linter());
  }

  @Override
  protected void registerAspects() {
    model.registerConfigAspect(DocumentationConfigAspect.create(model));
    model.registerConfigAspect(ContextConfigAspect.create(model));
    model.registerConfigAspect(HttpConfigAspect.create(model));
    model.registerConfigAspect(VisibilityConfigAspect.create(model));
    model.registerConfigAspect(VersionConfigAspect.create(model));
    model.registerConfigAspect(NamingConfigAspect.create(model));
    model.registerConfigAspect(SystemConfigAspect.create(model));
  }

  @Override
  protected void process() throws Exception {

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

    CodeGenerator generator =
        new CodeGenerator.Builder()
            .setConfigProto(configProto)
            .setModel(model)
            .build();
    if (generator == null) {
      return;
    }

    Multimap<Interface, GeneratedResult> docs = ArrayListMultimap.create();
    for (String snippetInputName : configProto.getSnippetFilesList()) {
      SnippetDescriptor snippetDescriptor =
          new SnippetDescriptor(snippetInputName);
      Map<Interface, GeneratedResult> code = generator.generate(snippetDescriptor);
      if (code == null) {
        continue;
      }
      for (Map.Entry<Interface, GeneratedResult> entry : code.entrySet()) {
        docs.put(entry.getKey(), entry.getValue());
      }
    }
    generator.outputCode(options.get(OUTPUT_FILE), docs, configProto.getArchive());
  }

  private ConfigProto loadConfigFromFiles(List<String> configFileNames) {
    List<File> configFiles = pathsToFiles(configFileNames);
    if (model.getErrorCount() > 0) {
      return null;
    }
    ImmutableMap<String, Message> supportedConfigTypes =
        ImmutableMap.<String, Message>of(ConfigProto.getDescriptor().getFullName(),
            ConfigProto.getDefaultInstance());
    ConfigProto configProto =
        (ConfigProto) MultiYamlReader.read(model, configFiles, supportedConfigTypes);
    return configProto;
  }

  private List<File> pathsToFiles(List<String> configFileNames) {
    List<File> files = new ArrayList<>();

    for (String configFileName : configFileNames) {
      File file = model.findDataFile(configFileName);
      if (file == null) {
        error("Cannot find configuration file '%s'.", configFileName);
        continue;
      }
      files.add(file);
    }

    return files;
  }

  private void error(String message, Object... args) {
    model.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
  }
}
