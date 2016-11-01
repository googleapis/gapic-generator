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

import com.google.api.tools.framework.tools.ToolOptions;
import java.util.Arrays;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

// Example usage: (assuming environment variable BASE is the base directory of the project
// containing the YAML config, discovery doc, and output)
//
//     DiscoveryFragmentGeneratorTool --discovery_doc=$BASE/<service>.json \
//        --gapic_yaml=$BASE/<service>_gapic.yaml \
//        --output=$BASE
public class DiscoveryFragmentGeneratorTool {
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(
        Option.builder()
            .longOpt("discovery_doc")
            .desc("The Discovery doc representing the service description.")
            .hasArg()
            .argName("DISCOVERY-DOC")
            .required(true)
            .build());
    options.addOption(
        Option.builder()
            .longOpt("overrides")
            .desc("The path to the sample config overrides file")
            .hasArg()
            .argName("OVERRIDES")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("gapic_yaml")
            .desc("The GAPIC YAML configuration file or files.")
            .hasArg()
            .argName("GAPIC-YAML")
            .required(true)
            .build());
    options.addOption(
        Option.builder("o")
            .longOpt("output")
            .desc("The directory in which to output the generated fragments.")
            .hasArg()
            .argName("OUTPUT-DIRECTORY")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("auth_instructions")
            .desc("An @-delimited map of language to auth instructions URL: lang:URL@lang:URL@...")
            .hasArg()
            .argName("AUTH-INSTRUCTIONS")
            .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("CodeGeneratorTool", options);
    }

    generate(
        cl.getOptionValue("discovery_doc"),
        cl.getOptionValues("gapic_yaml"),
        cl.getOptionValue("overrides", ""),
        cl.getOptionValue("output", ""),
        cl.getOptionValue("auth_instructions", ""));
  }

  private static void generate(
      String discoveryDoc,
      String[] generatorConfigs,
      String overridesFile,
      String outputDirectory,
      String authInstructions)
      throws Exception {

    ToolOptions options = ToolOptions.create();
    options.set(DiscoveryFragmentGeneratorApi.DISCOVERY_DOC, discoveryDoc);
    options.set(
        DiscoveryFragmentGeneratorApi.GENERATOR_CONFIG_FILES, Arrays.asList(generatorConfigs));
    options.set(DiscoveryFragmentGeneratorApi.OVERRIDES_FILE, overridesFile);
    options.set(DiscoveryFragmentGeneratorApi.OUTPUT_FILE, outputDirectory);
    options.set(DiscoveryFragmentGeneratorApi.AUTH_INSTRUCTIONS_URL, authInstructions);
    DiscoveryFragmentGeneratorApi generator = new DiscoveryFragmentGeneratorApi(options);
    generator.run();
  }
}
