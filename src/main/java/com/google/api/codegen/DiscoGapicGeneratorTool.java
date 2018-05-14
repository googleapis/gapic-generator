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
package com.google.api.codegen;

import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.Lists;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

// Example usage: (assuming environment variable BASE is the base directory of the project
// containing the YAMLs, descriptor set, and output)

// DiscoGapicGeneratorTool \
//   --gapic_yaml=$BASE/src/main/resources/com/google/api/codegen/java/java_discogapic.yaml \
//   --gapic_yaml=$BASE/src/main/resources/com/google/api/codegen/testdata/compute_gapic.yaml \
//   --discovery_doc=$BASE/src/test/java/com/google/api/codegen/testdata/discoveries/compute.v1.json \
//   --output=output
public class DiscoGapicGeneratorTool {
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
    // TODO add this option back
    //    options.addOption(
    //        Option.builder()
    //            .longOpt("service_yaml")
    //            .desc("The service YAML configuration file or files.")
    //            .hasArg()
    //            .argName("SERVICE-YAML")
    //            .required(true)
    //            .build());
    options.addOption(
        Option.builder()
            .longOpt("gapic_yaml")
            .desc("The GAPIC YAML configuration file or files.")
            .hasArg()
            .argName("GAPIC-YAML")
            .required(true)
            .build());
    options.addOption(
        Option.builder()
            .longOpt("package_yaml")
            .desc(
                "The package metadata YAML configuration file (deprecated in favor of package_yaml2).")
            .hasArg()
            .argName("PACKAGE-YAML")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("package_yaml2")
            .desc("The packaging YAML configuration file.")
            .hasArg()
            .argName("PACKAGE-YAML2")
            .build());
    options.addOption(
        Option.builder("o")
            .longOpt("output")
            .desc("The directory in which to output the generated client library.")
            .hasArg()
            .argName("OUTPUT-DIRECTORY")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("enabled_artifacts")
            .desc(
                "Optional. Artifacts enabled for the generator. "
                    + "Currently supports 'surface' and 'test'.")
            .hasArg()
            .argName("ENABLED_ARTIFACTS")
            .required(false)
            .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("CodeGeneratorTool", options);
    }

    try {
      generate(
          cl.getOptionValue("discovery_doc"),
          //          cl.getOptionValues("service_yaml"),
          cl.getOptionValues("gapic_yaml"),
          cl.getOptionValue("package_yaml"),
          cl.getOptionValue("package_yaml2"),
          cl.getOptionValue("output", ""),
          cl.getOptionValues("enabled_artifacts"));
    } catch (Exception e) {
      e.printStackTrace(System.err);
      System.exit(1);
    }
  }

  private static void generate(
      String discoveryDoc,
      String[] generatorConfigs,
      String packageConfig,
      String packageConfig2,
      String outputDirectory,
      String[] enabledArtifacts)
      throws Exception {
    ToolOptions options = ToolOptions.create();
    options.set(DiscoGapicGeneratorApi.DISCOVERY_DOC, discoveryDoc);
    options.set(CodeGeneratorApi.OUTPUT_FILE, outputDirectory);
    options.set(CodeGeneratorApi.GENERATOR_CONFIG_FILES, Lists.newArrayList(generatorConfigs));
    options.set(CodeGeneratorApi.PACKAGE_CONFIG_FILE, packageConfig);
    options.set(CodeGeneratorApi.PACKAGE_CONFIG2_FILE, packageConfig2);

    if (enabledArtifacts != null) {
      options.set(CodeGeneratorApi.ENABLED_ARTIFACTS, Lists.newArrayList(enabledArtifacts));
    }
    DiscoGapicGeneratorApi codeGen = new DiscoGapicGeneratorApi(options);
    codeGen.run();
  }
}
