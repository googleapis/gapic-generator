package io.gapi.vgen;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
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
import io.gapi.fx.yaml.YamlReader;

import java.io.File;
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

  public static final Option<String> GENERATOR_CONFIG_FILE = ToolOptions.createOption(
      String.class,
      "config_file",
      "Specifies a config yaml file for the code generator.",
      "");

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
    String configFileName = options.get(GENERATOR_CONFIG_FILE);
    if (configFileName.isEmpty()) {
      error("--config_file must be provided");
      return;
    }
    File file = model.findDataFile(configFileName);
    if (file == null) {
      error("Cannot find configuration file '%s'.", configFileName);
      return;
    }
    Message configMessage = YamlReader.read(model, file,
        ImmutableMap.<String, Message>of(
            Config.getDescriptor().getFullName(), Config.getDefaultInstance()));
    if (configMessage == null) {
      // errors reported by yaml reader
      return;
    }

    Config config = (Config) configMessage;
    // Create and call the code generator.
    CodeGenerator generator = CodeGenerator.create(model, config);
    if (generator == null) {
      // errors reported by generator
      return;
    }

    Multimap<Interface, GeneratedResult> docs = ArrayListMultimap.create();
    for (String snippetInputName : config.getSnippetInputNamesList()) {
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
    generator.outputCode(options.get(OUTPUT_FILE), docs);
  }

  private void error(String message, Object... args) {
    model.addDiag(Diag.error(SimpleLocation.TOPLEVEL, message, args));
  }

}
