package io.gapi.vgen;

import com.google.api.tools.framework.tools.ToolOptions;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.Arrays;

// Example usage: (assuming environment variable BASE is the base directory of the project
// containing the yaml config, discovery doc, and output)
//
//     DiscoveryFragmentGeneratorTool --discovery_doc=$BASE/<service>.json \
//        --veneer_yaml=$BASE/<service>_veneer.yaml \
//        --output=$BASE
public class DiscoveryFragmentGeneratorTool {
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(Option.builder()
        .longOpt("discovery_doc")
        .desc("The Discovery doc representing the service description.")
        .hasArg()
        .argName("DISCOVERY-DOC")
        .required(true)
        .build());
    options.addOption(Option.builder()
        .longOpt("veneer_yaml")
        .desc("The Veneer YAML configuration file or files.")
        .hasArg()
        .argName("VENEER-YAML")
        .required(true)
        .build());
    options.addOption(Option.builder("o")
        .longOpt("output")
        .desc("The directory in which to output the generated fragments.")
        .hasArg()
        .argName("OUTPUT-DIRECTORY")
        .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("CodeGeneratorTool", options);
    }

    generate(cl.getOptionValue("discovery_doc"),
             cl.getOptionValues("veneer_yaml"),
             cl.getOptionValue("output", ""));
  }

  @SuppressWarnings("unchecked")
  private static void generate(String discoveryDoc,
      String[] generatorConfigs, String outputDirectory) throws Exception {

    ToolOptions options = ToolOptions.create();
    options.set(DiscoveryFragmentGeneratorApi.DISCOVERY_DOC, discoveryDoc);
    options.set(DiscoveryFragmentGeneratorApi.OUTPUT_FILE, outputDirectory);
    options.set(DiscoveryFragmentGeneratorApi.GENERATOR_CONFIG_FILES, Arrays.asList(generatorConfigs));
    DiscoveryFragmentGeneratorApi generator = new DiscoveryFragmentGeneratorApi(options);
    generator.run();
  }
}
