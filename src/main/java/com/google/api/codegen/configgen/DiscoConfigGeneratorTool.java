/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.configgen;

import static com.google.api.codegen.DiscoGapicGeneratorApi.DISCOVERY_DOC_OPTION_NAME;

import com.google.api.tools.framework.tools.ToolOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class DiscoConfigGeneratorTool {

  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(
        Option.builder()
            .longOpt(DISCOVERY_DOC_OPTION_NAME)
            .desc("The filepath of the raw Discovery document.")
            .hasArg()
            .argName("DISCOVERY-FILE")
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

    generate(cl.getOptionValue(DISCOVERY_DOC_OPTION_NAME), cl.getOptionValue("output"));
  }

  private static void generate(String discoveryFilepath, String outputFile) {
    ToolOptions options = ToolOptions.create();
    options.set(DiscoConfigGeneratorApi.OUTPUT_FILE, outputFile);
    options.set(DiscoConfigGeneratorApi.DISCOVERY_DOC, discoveryFilepath);
    DiscoConfigGeneratorApi configGen = new DiscoConfigGeneratorApi(options);
    configGen.run();
  }
}
