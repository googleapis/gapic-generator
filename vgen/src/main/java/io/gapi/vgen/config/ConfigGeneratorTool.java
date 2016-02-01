package io.gapi.vgen.config;

import com.google.api.tools.framework.tools.ToolOptions;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ConfigGeneratorTool {

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(Option.builder()
        .longOpt("descriptor_set")
        .desc("The descriptor set representing the compiled input protos.")
        .hasArg()
        .argName("DESCRIPTOR-SET")
        .required(true)
        .build());
    options.addOption(Option.builder("o")
        .longOpt("output")
        .desc("The directory in which to output the generated config.")
        .hasArg()
        .argName("OUTPUT-FILE")
        .required(true)
        .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if(cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("ConfigGeneratorTool", options);
    }

    generate(cl.getOptionValue("descriptor_set"),
             cl.getOptionValue("output"));
  }

  private static void generate(String descriptorSet, String outputFile) {
    ToolOptions options = ToolOptions.create();
    options.set(ConfigGeneratorApi.OUTPUT_FILE, outputFile);
    options.set(ToolOptions.DESCRIPTOR_SET, descriptorSet);
    ConfigGeneratorApi configGen = new ConfigGeneratorApi(options);
    configGen.run();
  }
}