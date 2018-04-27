/* Copyright 2016 Google LLC
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
import java.io.File;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

// Example usage: (assuming environment variable BASE is the base directory of the project
// containing the YAMLs, descriptor set, and output)
//
//     CodeGeneratorTool --descriptor_set=$BASE/src/main/generated/_descriptors/bigtable.desc \
//        --service_yaml=$BASE/src/main/configs/bigtabletableadmin.yaml \
//        --gapic_yaml=$BASE/src/main/configs/bigtable_table_gapic.yaml \
//        --output=$BASE
public class CodeGeneratorTool {
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
            .desc("The package metadata YAML configuration file.")
            .hasArg()
            .argName("PACKAGE-YAML")
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

    int exitCode =
        generate(
            cl.getOptionValue("descriptor_set"),
            cl.getOptionValues("service_yaml"),
            cl.getOptionValues("gapic_yaml"),
            cl.getOptionValue("package_yaml"),
            cl.getOptionValue("output", ""),
            cl.getOptionValues("enabled_artifacts"));
    System.exit(exitCode);
  }

  private static int generate(
      String descriptorSet,
      String[] configs,
      String[] generatorConfigs,
      String packageConfig,
      String outputDirectory,
      String[] enabledArtifacts) {
    checkFile(descriptorSet);
    checkFiles(configs);
    checkFiles(generatorConfigs);
    checkFile(packageConfig);

    ToolOptions options = ToolOptions.create();
    options.set(ToolOptions.DESCRIPTOR_SET, descriptorSet);
    options.set(ToolOptions.CONFIG_FILES, Lists.newArrayList(configs));
    options.set(CodeGeneratorApi.OUTPUT_FILE, outputDirectory);
    options.set(CodeGeneratorApi.GENERATOR_CONFIG_FILES, Lists.newArrayList(generatorConfigs));
    options.set(CodeGeneratorApi.PACKAGE_CONFIG_FILE, packageConfig);

    if (enabledArtifacts != null) {
      options.set(CodeGeneratorApi.ENABLED_ARTIFACTS, Lists.newArrayList(enabledArtifacts));
    }
    CodeGeneratorApi codeGen = new CodeGeneratorApi(options);

    return codeGen.run();
  }

  private static void checkFiles(String[] files) {
    for (String filePath : files) {
      checkFile(filePath);
    }
  }

  private static void checkFile(String filePath) {
    if (!new File(filePath).exists()) {
      throw new IllegalArgumentException("File not found: " + filePath);
    }
  }
}
