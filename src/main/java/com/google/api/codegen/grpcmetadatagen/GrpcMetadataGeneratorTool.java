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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.codegen.TargetLanguage;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.Lists;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/** Tool to generate package metadata. */
public class GrpcMetadataGeneratorTool {
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
            .required(false)
            .build());
    options.addOption(
        Option.builder("i")
            .longOpt("input")
            .desc("The input directory containing the gRPC package.")
            .hasArg()
            .argName("INPUT-DIR")
            .required(true)
            .build());
    options.addOption(
        Option.builder("o")
            .longOpt("output")
            .desc("The directory in which to output the generated config.")
            .hasArg()
            .argName("OUTPUT-DIR")
            .required(true)
            .build());
    options.addOption(
        Option.builder("l")
            .longOpt("language")
            .desc("The language for which to generate package metadata.")
            .hasArg()
            .argName("LANGUAGE")
            .required(true)
            .build());
    options.addOption(
        Option.builder("c")
            .longOpt("metadata_config")
            .desc("The YAML file configuring the package metadata.")
            .hasArg()
            .argName("METADATA-CONFIG")
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
        cl.getOptionValue("input"),
        cl.getOptionValue("output"),
        cl.getOptionValue("language"),
        cl.getOptionValue("metadata_config"));
  }

  private static void generate(
      String descriptorSet,
      String[] configs,
      String inputDir,
      String outputDir,
      String languageString,
      String metadataConfig) {
    TargetLanguage language = TargetLanguage.fromString(languageString);
    ToolOptions options = ToolOptions.create();
    options.set(GrpcMetadataGenerator.INPUT_DIR, inputDir);
    options.set(GrpcMetadataGenerator.OUTPUT_DIR, outputDir);
    options.set(ToolOptions.DESCRIPTOR_SET, descriptorSet);
    if (configs != null) {
      options.set(ToolOptions.CONFIG_FILES, Lists.newArrayList(configs));
    }
    options.set(GrpcMetadataGenerator.METADATA_CONFIG_FILE, metadataConfig);
    options.set(GrpcMetadataGenerator.LANGUAGE, languageString);
    GrpcMetadataGenerator generator = new GrpcMetadataGenerator(options);
    generator.run();
  }
}
