/* Copyright 2016 Google LLC
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
package com.google.api.codegen.configgen;

import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.Lists;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class ConfigGeneratorTool {

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(
        Option.builder()
            .longOpt("descriptor_set")
            .desc("The descriptor set representing the compiled input protos.")
            .hasArg()
            .argName("DESCRIPTOR-SET")
            .required(true)
            .build());
    options.addOption(
        Option.builder()
            .longOpt("service_yaml")
            .desc("The service YAML configuration file or files.")
            .hasArg()
            .argName("SERVICE-YAML")
            .required(true)
            .build());
    options.addOption(
        Option.builder("o")
            .longOpt("output")
            .desc("The directory in which to output the generated config.")
            .hasArg()
            .argName("OUTPUT-FILE")
            .required(true)
            .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("ConfigGeneratorTool", options);
    }

    generate(
        cl.getOptionValue("descriptor_set"),
        cl.getOptionValues("service_yaml"),
        cl.getOptionValue("output"));
  }

  private static void generate(String descriptorSet, String[] configs, String outputFile) {
    ToolOptions options = ToolOptions.create();
    options.set(ConfigGeneratorApi.OUTPUT_FILE, outputFile);
    options.set(ToolOptions.DESCRIPTOR_SET, descriptorSet);
    options.set(ToolOptions.CONFIG_FILES, Lists.newArrayList(configs));
    ConfigGeneratorApi configGen = new ConfigGeneratorApi(options);
    configGen.run();
  }
}
