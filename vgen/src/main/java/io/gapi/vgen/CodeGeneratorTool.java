package io.gapi.vgen;

import com.google.api.tools.framework.tools.ToolOptions;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.ArrayList;
import java.util.List;

// Example usage: (assuming environment variable BASE is the base directory of the project
// containing the yamls, descriptor set, and output)
//
//     CodeGeneratorTool --descriptor_set=$BASE/src/main/generated/_descriptors/bigtable.desc \
//        --service_yaml=$BASE/src/main/configs/bigtabletableadmin.yaml \
//        --veneer_yaml=$BASE/src/main/configs/bigtable_table_veneer.yaml \
//        --output=$BASE
public class CodeGeneratorTool {
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
    options.addOption(Option.builder()
        .longOpt("service_yaml")
        .desc("The service yaml configuration file or files.")
        .hasArg()
        .argName("SERVICE-YAML")
        .required(true)
        .build());
    options.addOption(Option.builder()
        .longOpt("veneer_yaml")
        .desc("The Veneer yaml configuration file or files.")
        .hasArg()
        .argName("VENEER-YAML")
        .required(true)
        .build());
    options.addOption(Option.builder("o")
        .longOpt("output")
        .desc("The directory in which to output the generated Veneer.")
        .hasArg()
        .argName("OUTPUT-DIRECTORY")
        .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if(cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("CodeGeneratorTool", options);
    }

    int exitCode = generate(cl.getOptionValue("descriptor_set"),
                            cl.getOptionValues("service_yaml"),
                            cl.getOptionValues("veneer_yaml"),
                            cl.getOptionValue("output", ""));
    System.exit(exitCode);
  }

  private static int generate(String descriptorSet, String[] apiConfigs,
      String[] generatorConfigs, String outputDirectory) {

    ToolOptions options = ToolOptions.create();
    options.set(ToolOptions.DESCRIPTOR_SET, descriptorSet);
    List<String> configs = new ArrayList<String>();
    for (String config : apiConfigs) {
      configs.add(config);
    }
    options.set(ToolOptions.CONFIG_FILES, configs);
    options.set(CodeGeneratorApi.OUTPUT_FILE, outputDirectory);
    List<String> genConfigs = new ArrayList<String>();
    for (String genConfig : generatorConfigs) {
      genConfigs.add(genConfig);
    }
    options.set(CodeGeneratorApi.GENERATOR_CONFIG_FILES, genConfigs);
    CodeGeneratorApi codeGen = new CodeGeneratorApi(options);
    return codeGen.run();
  }
}


