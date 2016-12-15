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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.codegen.metadatagen.py.PythonPackageCopier;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/** Tool to generate package metadata. */
public class GrpcMetadataGeneratorTool {

  private static final Map<String, List<String>> SNIPPETS =
      new ImmutableMap.Builder<String, List<String>>()
          .put(
              "python",
              Lists.newArrayList(
                  "LICENSE.snip",
                  "py/setup.py.snip",
                  "py/README.rst.snip",
                  "py/PUBLISHING.rst.snip",
                  "py/MANIFEST.in.snip"))
          .build();

  private static final Map<String, GrpcPackageCopier> COPIERS =
      new ImmutableMap.Builder<String, GrpcPackageCopier>()
          .put("python", new PythonPackageCopier())
          .build();

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
        Option.builder("d")
            .longOpt("dependencies_config")
            .desc("The dependencies configuration file.")
            .hasArg()
            .argName("DEPENDENCIES-FILE")
            .required(true)
            .build());
    options.addOption(
        Option.builder("c")
            .longOpt("defaults_config")
            .desc("The defaults configuration file.")
            .hasArg()
            .argName("DEFAULTS-FILE")
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
        Option.builder("s")
            .longOpt("short_name")
            .desc("The short name for the API.")
            .hasArg()
            .argName("SHORT-NAME")
            .required(true)
            .build());
    options.addOption(
        Option.builder("p")
            .longOpt("package_name")
            .desc(
                "The base name of the package. Defaults to \"google-cloud-{short_name}-{version}\"")
            .hasArg()
            .argName("PACKAGE-NAME")
            .build());
    options.addOption(
        Option.builder("g")
            .longOpt("googleapis_path")
            .desc("The path to the API protos under googleapis.")
            .hasArg()
            .argName("GOOGLEAPIS-PATH")
            .required(true)
            .build());
    options.addOption(
        Option.builder("v")
            .longOpt("version")
            .desc("The major version of the API.")
            .hasArg()
            .argName("VERSION")
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
        cl.getOptionValue("dependencies_config"),
        cl.getOptionValue("defaults_config"),
        cl.getOptionValue("short_name"),
        cl.getOptionValue("package_name"),
        cl.getOptionValue("googleapis_path"),
        cl.getOptionValue("version"));
  }

  private static void generate(
      String descriptorSet,
      String[] apiConfigs,
      String inputDir,
      String outputDir,
      String language,
      String dependenciesConfig,
      String defaultsConfig,
      String shortName,
      String packageName,
      String googleapisPath,
      String version) {
    ToolOptions options = ToolOptions.create();
    options.set(GrpcMetadataGenerator.INPUT_DIR, inputDir);
    options.set(GrpcMetadataGenerator.OUTPUT_DIR, outputDir);
    options.set(ToolOptions.DESCRIPTOR_SET, descriptorSet);
    options.set(ToolOptions.CONFIG_FILES, Lists.newArrayList(apiConfigs));
    options.set(GrpcMetadataGenerator.DEPENDENCIES_FILE, dependenciesConfig);
    options.set(GrpcMetadataGenerator.API_DEFAULTS_FILE, defaultsConfig);
    options.set(GrpcMetadataGenerator.SHORT_API_NAME, shortName);
    options.set(GrpcMetadataGenerator.API_PATH, googleapisPath);
    options.set(GrpcMetadataGenerator.API_VERSION, version);
    if (Strings.isNullOrEmpty(packageName)) {
      options.set(GrpcMetadataGenerator.PACKAGE_NAME, "google-cloud-" + shortName + "-" + version);
    } else {
      options.set(GrpcMetadataGenerator.PACKAGE_NAME, packageName);
    }
    GrpcMetadataGenerator generator =
        new GrpcMetadataGenerator(options, getSnippets(language), getCopier(language));
    generator.run();
  }

  // Public for visibility to tests.
  public static List<String> getSnippets(String language) {
    return getForLanguage(SNIPPETS, language);
  }

  // Public for visibility to tests.
  public static GrpcPackageCopier getCopier(String language) {
    return getForLanguage(COPIERS, language);
  }

  private static <T> T getForLanguage(Map<String, T> map, String language) {
    T value = map.get(language);
    if (value == null) {
      throw new IllegalArgumentException(
          "The target language \"" + language + "\" is not supported");
    }
    return value;
  }
}
