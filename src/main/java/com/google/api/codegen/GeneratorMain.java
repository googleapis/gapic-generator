/* Copyright 2018 Google LLC
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

import com.google.api.codegen.configgen.DiscoConfigGeneratorApp;
import com.google.api.codegen.configgen.GapicConfigGeneratorApp;
import com.google.api.codegen.discogapic.DiscoGapicGeneratorApp;
import com.google.api.codegen.gapic.GapicGeneratorApp;
import com.google.api.codegen.packagegen.PackageGeneratorApp;
import com.google.api.codegen.packagegen.PackagingArtifactType;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.List;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

// Example usage: (assuming environment variable BASE is the base directory of the project
// containing the YAMLs, descriptor set, and output)
//
//     GeneratorMain LEGACY_GAPIC_AND_PACKAGE \
//        --descriptor_set=$BASE/src/main/generated/_descriptors/bigtable.desc \
//        --service_yaml=$BASE/src/main/configs/bigtabletableadmin.yaml \
//        --gapic_yaml=$BASE/src/main/configs/bigtable_table_gapic.yaml \
//        --output=$BASE
public class GeneratorMain {
  private static final Option DESCRIPTOR_SET_OPTION =
      Option.builder()
          .longOpt("descriptor_set")
          .desc("The descriptor set representing the compiled input protos.")
          .hasArg()
          .argName("DESCRIPTOR-SET")
          .required(true)
          .build();
  private static final Option TARGET_API_PROTO_PACKAGE =
      Option.builder()
          .longOpt("package")
          .desc(
              "The proto package designating the files actually intended for output. "
                  + "This option is required if a GAPIC config is not given.")
          .hasArg()
          .argName("PACKAGE")
          .required(false)
          .build();
  private static final Option SERVICE_YAML_OPTION =
      Option.builder()
          .longOpt("service_yaml")
          .desc("The service YAML configuration file or files.")
          .hasArg()
          .argName("SERVICE-YAML")
          .required(true)
          .build();
  private static final Option SERVICE_YAML_NONREQUIRED_OPTION =
      Option.builder()
          .longOpt("service_yaml")
          .desc("The service YAML configuration file or files.")
          .hasArg()
          .argName("SERVICE-YAML")
          .required(false)
          .build();
  private static final Option LANGUAGE_OPTION =
      Option.builder("l")
          .longOpt("language")
          .desc("The target programming language for generated output.")
          .hasArg()
          .argName("LANGUAGE")
          .required(true)
          .build();
  private static final Option LANGUAGE_NONREQUIRED_OPTION =
      Option.builder("l")
          .longOpt("language")
          .desc("The target programming language for generated output.")
          .hasArg()
          .argName("LANGUAGE")
          .required(false)
          .build();
  private static final Option OUTPUT_OPTION =
      Option.builder("o")
          .longOpt("output")
          .desc("The destination file or directory for the generated files.")
          .hasArg()
          .argName("OUTPUT")
          .required(true)
          .build();
  private static final Option GAPIC_YAML_OPTION =
      Option.builder()
          .longOpt("gapic_yaml")
          .desc("The GAPIC YAML configuration file or files.")
          .hasArg()
          .argName("GAPIC-YAML")
          .required(true)
          .build();
  private static final Option GAPIC_YAML_NONREQUIRED_OPTION =
      Option.builder()
          .longOpt("gapic_yaml")
          .desc(
              "The GAPIC YAML configuration file or files. This is required only if "
                  + "the --package option is not specified.")
          .hasArg()
          .argName("GAPIC-YAML")
          .required(false)
          .build();
  private static final Option PACKAGE_YAML2_OPTION =
      Option.builder("c2")
          .longOpt("package_yaml2")
          .desc("The packaging YAML configuration file.")
          .hasArg()
          .argName("PACKAGE-YAML2")
          .required(false)
          .build();
  private static final Option DISCOVERY_DOC_OPTION =
      Option.builder()
          .longOpt("discovery_doc")
          .desc("The filepath of the raw Discovery document.")
          .hasArg()
          .argName("DISCOVERY-DOC")
          .required(true)
          .build();

  public static void printAvailableCommands() {
    System.err.println("  Available artifact types:");
    for (ArtifactType artifactType : ArtifactType.values()) {
      System.err.println("    " + artifactType);
    }
  }

  public static void main(String[] args) throws Exception {
    if (args.length == 0) {
      System.err.println("No artifact type given!");
      printAvailableCommands();
      System.exit(1);
      return;
    }
    String command = args[0].toUpperCase();

    ArtifactType artifactType;
    try {
      artifactType = ArtifactType.valueOf(command);
    } catch (Exception e) {
      System.err.println("Unrecognized artifact type: '" + command.toLowerCase() + "'");
      printAvailableCommands();
      System.exit(1);
      return;
    }

    switch (artifactType) {
      case GAPIC_CONFIG:
        gapicConfigGeneratorMain(args);
        break;
      case GAPIC_CODE:
        gapicGeneratorMain(artifactType, args);
        break;
      case GAPIC_PACKAGE:
        gapicGeneratorMain(artifactType, args);
        break;
      case LEGACY_GAPIC_AND_PACKAGE:
        gapicGeneratorMain(artifactType, args);
        break;
      case DISCOGAPIC_CONFIG:
        discoGapicConfigGeneratorMain(args);
        break;
      case DISCOGAPIC_CODE:
        discoGapicMain(artifactType, args);
        break;
      case LEGACY_DISCOGAPIC_AND_PACKAGE:
        discoGapicMain(artifactType, args);
        break;
      case LEGACY_GRPC_PACKAGE:
        packageGeneratorMain(args);
        break;
      default:
        System.err.println(
            "ArtifactType '"
                + artifactType
                + "' present in enum but not supported on command line - programmer error?");
        System.exit(1);
    }
  }

  public static void gapicConfigGeneratorMain(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(DESCRIPTOR_SET_OPTION);
    options.addOption(SERVICE_YAML_OPTION);
    options.addOption(OUTPUT_OPTION);

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("ConfigGeneratorTool", options);
    }

    ToolOptions toolOptions = ToolOptions.create();
    toolOptions.set(
        GapicConfigGeneratorApp.OUTPUT_FILE, cl.getOptionValue(OUTPUT_OPTION.getLongOpt()));
    toolOptions.set(
        ToolOptions.DESCRIPTOR_SET, cl.getOptionValue(DESCRIPTOR_SET_OPTION.getLongOpt()));
    toolOptions.set(
        ToolOptions.CONFIG_FILES,
        Lists.newArrayList(cl.getOptionValues(SERVICE_YAML_OPTION.getLongOpt())));
    GapicConfigGeneratorApp configGen = new GapicConfigGeneratorApp(toolOptions);
    int exitCode = configGen.run();
    System.exit(exitCode);
  }

  public static void gapicGeneratorMain(ArtifactType artifactType, String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(DESCRIPTOR_SET_OPTION);
    options.addOption(SERVICE_YAML_NONREQUIRED_OPTION);
    // TODO make required after artman passes this in
    options.addOption(LANGUAGE_NONREQUIRED_OPTION);
    options.addOption(GAPIC_YAML_NONREQUIRED_OPTION);
    options.addOption(PACKAGE_YAML2_OPTION);
    options.addOption(TARGET_API_PROTO_PACKAGE);
    options.addOption(OUTPUT_OPTION);
    Option enabledArtifactsOption =
        Option.builder()
            .longOpt("enabled_artifacts")
            .desc(
                "Optional. Artifacts enabled for the generator. "
                    + "Currently supports 'surface' and 'test'.")
            .hasArg()
            .argName("ENABLED_ARTIFACTS")
            .required(false)
            .build();
    options.addOption(enabledArtifactsOption);

    Option devSamplesOption =
        Option.builder()
            .longOpt("dev_samples")
            .desc("Whether to generate samples in non-production-ready languages.")
            .argName("DEV_SAMPLES")
            .required(false)
            .build();
    options.addOption(devSamplesOption);

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("GapicGeneratorTool", options);
    }

    ToolOptions toolOptions = ToolOptions.create();
    toolOptions.set(
        ToolOptions.DESCRIPTOR_SET, cl.getOptionValue(DESCRIPTOR_SET_OPTION.getLongOpt()));

    // TODO(andrealin): Write system tests to ensure at least one option given.
    checkAtLeastOneOption(cl, SERVICE_YAML_NONREQUIRED_OPTION, TARGET_API_PROTO_PACKAGE);
    checkAtLeastOneOption(cl, GAPIC_YAML_NONREQUIRED_OPTION, TARGET_API_PROTO_PACKAGE);

    toolOptions.set(
        GapicGeneratorApp.PROTO_PACKAGE, cl.getOptionValue(TARGET_API_PROTO_PACKAGE.getLongOpt()));
    toolOptions.set(
        GapicGeneratorApp.LANGUAGE, cl.getOptionValue(LANGUAGE_NONREQUIRED_OPTION.getLongOpt()));
    toolOptions.set(
        GapicGeneratorApp.OUTPUT_FILE, cl.getOptionValue(OUTPUT_OPTION.getLongOpt(), ""));
    toolOptions.set(
        GapicGeneratorApp.PACKAGE_CONFIG2_FILE,
        cl.getOptionValue(PACKAGE_YAML2_OPTION.getLongOpt()));

    checkFile(toolOptions.get(ToolOptions.DESCRIPTOR_SET));

    if (cl.getOptionValues(SERVICE_YAML_NONREQUIRED_OPTION.getLongOpt()) != null) {
      toolOptions.set(
          ToolOptions.CONFIG_FILES,
          Lists.newArrayList(cl.getOptionValues(SERVICE_YAML_NONREQUIRED_OPTION.getLongOpt())));
      checkFiles(toolOptions.get(ToolOptions.CONFIG_FILES));
    }
    if (cl.getOptionValues(GAPIC_YAML_NONREQUIRED_OPTION.getLongOpt()) != null) {
      toolOptions.set(
          GapicGeneratorApp.GENERATOR_CONFIG_FILES,
          Lists.newArrayList(cl.getOptionValues(GAPIC_YAML_NONREQUIRED_OPTION.getLongOpt())));
      checkFiles(toolOptions.get(GapicGeneratorApp.GENERATOR_CONFIG_FILES));
    }
    if (!Strings.isNullOrEmpty(toolOptions.get(GapicGeneratorApp.PACKAGE_CONFIG2_FILE))) {
      checkFile(toolOptions.get(GapicGeneratorApp.PACKAGE_CONFIG2_FILE));
    }

    if (cl.getOptionValues(enabledArtifactsOption.getLongOpt()) != null) {
      toolOptions.set(
          GapicGeneratorApp.ENABLED_ARTIFACTS,
          Lists.newArrayList(cl.getOptionValues(enabledArtifactsOption.getLongOpt())));
    }

    toolOptions.set(GapicGeneratorApp.DEV_SAMPLES, cl.hasOption(devSamplesOption.getLongOpt()));

    GapicGeneratorApp codeGen = new GapicGeneratorApp(toolOptions, artifactType);
    int exitCode = codeGen.run();
    System.exit(exitCode);
  }

  public static void packageGeneratorMain(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(DESCRIPTOR_SET_OPTION);
    options.addOption(TARGET_API_PROTO_PACKAGE);
    options.addOption(SERVICE_YAML_NONREQUIRED_OPTION);
    options.addOption(LANGUAGE_OPTION);
    Option inputOption =
        Option.builder("i")
            .longOpt("input")
            .desc("The input directory containing the gRPC package.")
            .hasArg()
            .argName("INPUT-DIR")
            .required(true)
            .build();
    options.addOption(inputOption);
    options.addOption(OUTPUT_OPTION);
    options.addOption(PACKAGE_YAML2_OPTION);
    Option artifactTypeOption =
        Option.builder()
            .longOpt("artifact_type")
            .desc(
                "Optional. Artifacts enabled for the generator. Currently supports "
                    + "'GRPC' and 'PROTOBUF' and is ignored for all languages except Java")
            .hasArg()
            .argName("ARTIFACT-TYPE")
            .required(false)
            .build();
    options.addOption(artifactTypeOption);

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("PackageGeneratorTool", options);
    }

    ToolOptions toolOptions = ToolOptions.create();

    toolOptions.set(PackageGeneratorApp.LANGUAGE, cl.getOptionValue(LANGUAGE_OPTION.getLongOpt()));
    toolOptions.set(PackageGeneratorApp.INPUT_DIR, cl.getOptionValue(inputOption.getLongOpt()));
    toolOptions.set(PackageGeneratorApp.OUTPUT_DIR, cl.getOptionValue(OUTPUT_OPTION.getLongOpt()));
    toolOptions.set(
        ToolOptions.DESCRIPTOR_SET, cl.getOptionValue(DESCRIPTOR_SET_OPTION.getLongOpt()));
    toolOptions.set(
        PackageGeneratorApp.PROTO_PACKAGE,
        cl.getOptionValue(TARGET_API_PROTO_PACKAGE.getLongOpt()));
    if (cl.getOptionValues(SERVICE_YAML_NONREQUIRED_OPTION.getLongOpt()) != null) {
      toolOptions.set(
          ToolOptions.CONFIG_FILES,
          Lists.newArrayList(cl.getOptionValues(SERVICE_YAML_NONREQUIRED_OPTION.getLongOpt())));
    }
    toolOptions.set(
        PackageGeneratorApp.PACKAGE_CONFIG2_FILE,
        cl.getOptionValue(PACKAGE_YAML2_OPTION.getLongOpt()));
    toolOptions.set(PackageGeneratorApp.LANGUAGE, cl.getOptionValue(LANGUAGE_OPTION.getLongOpt()));

    if (cl.getOptionValue(artifactTypeOption.getLongOpt()) != null) {
      toolOptions.set(
          PackageGeneratorApp.ARTIFACT_TYPE,
          PackagingArtifactType.of(cl.getOptionValue(artifactTypeOption.getLongOpt())));
    }

    PackageGeneratorApp generator = new PackageGeneratorApp(toolOptions);
    int exitCode = generator.run();
    System.exit(exitCode);
  }

  public static void discoGapicConfigGeneratorMain(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(DISCOVERY_DOC_OPTION);
    options.addOption(OUTPUT_OPTION);

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("ConfigGeneratorTool", options);
    }

    ToolOptions toolOptions = ToolOptions.create();
    toolOptions.set(
        DiscoConfigGeneratorApp.OUTPUT_FILE, cl.getOptionValue(OUTPUT_OPTION.getLongOpt()));
    toolOptions.set(
        DiscoConfigGeneratorApp.DISCOVERY_DOC,
        cl.getOptionValue(DISCOVERY_DOC_OPTION.getLongOpt()));
    DiscoConfigGeneratorApp configGen = new DiscoConfigGeneratorApp(toolOptions);
    int exitCode = configGen.run();
    System.exit(exitCode);
  }

  public static void discoGapicMain(ArtifactType artifactType, String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    // TODO make required after artman passes this in
    options.addOption(LANGUAGE_NONREQUIRED_OPTION);
    options.addOption(DISCOVERY_DOC_OPTION);
    // TODO (andrealin): Make Gapic YAML optional
    options.addOption(GAPIC_YAML_OPTION);
    options.addOption(PACKAGE_YAML2_OPTION);
    options.addOption(OUTPUT_OPTION);
    Option enabledArtifactsOption =
        Option.builder()
            .longOpt("enabled_artifacts")
            .desc(
                "Optional. Artifacts enabled for the generator. "
                    + "Currently supports 'surface' and 'test'.")
            .hasArg()
            .argName("ENABLED_ARTIFACTS")
            .required(false)
            .build();
    options.addOption(enabledArtifactsOption);

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formatter = new HelpFormatter();
      formatter.printHelp("DiscoGapicGeneratorTool", options);
    }

    ToolOptions toolOptions = ToolOptions.create();
    toolOptions.set(
        DiscoGapicGeneratorApp.LANGUAGE,
        cl.getOptionValue(LANGUAGE_NONREQUIRED_OPTION.getLongOpt()));
    toolOptions.set(
        DiscoGapicGeneratorApp.DISCOVERY_DOC, cl.getOptionValue(DISCOVERY_DOC_OPTION.getLongOpt()));
    toolOptions.set(
        GapicGeneratorApp.OUTPUT_FILE, cl.getOptionValue(OUTPUT_OPTION.getLongOpt(), ""));
    toolOptions.set(
        GapicGeneratorApp.GENERATOR_CONFIG_FILES,
        Lists.newArrayList(cl.getOptionValues(GAPIC_YAML_OPTION.getLongOpt())));
    toolOptions.set(
        GapicGeneratorApp.PACKAGE_CONFIG2_FILE,
        cl.getOptionValue(PACKAGE_YAML2_OPTION.getLongOpt()));

    if (cl.getOptionValues(enabledArtifactsOption.getLongOpt()) != null) {
      toolOptions.set(
          GapicGeneratorApp.ENABLED_ARTIFACTS,
          Lists.newArrayList(cl.getOptionValues(enabledArtifactsOption.getLongOpt())));
    }
    DiscoGapicGeneratorApp codeGen = new DiscoGapicGeneratorApp(toolOptions, artifactType);
    int exitCode = codeGen.run();
    System.exit(exitCode);
  }

  private static void checkFiles(List<String> files) {
    for (String filePath : files) {
      checkFile(filePath);
    }
  }

  private static void checkFile(String filePath) {
    if (!new File(filePath).exists()) {
      throw new IllegalArgumentException("File not found: " + filePath);
    }
  }

  // Throws an exception if neither option was given.
  private static void checkAtLeastOneOption(CommandLine cl, Option option1, Option option2) {
    if (cl.getOptionValues(option1.getLongOpt()) == null
        && cl.getOptionValues(option2.getLongOpt()) == null) {
      throw new IllegalArgumentException(
          String.format(
              "At least one of --%s and/or --%s must be given.",
              option1.getLongOpt(), option2.getLongOpt()));
    }
  }
}
