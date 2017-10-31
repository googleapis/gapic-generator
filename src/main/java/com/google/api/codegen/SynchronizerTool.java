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
package com.google.api.codegen;

import com.google.api.codegen.sync.Synchronizer;
import com.google.api.tools.framework.tools.ToolOptions;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

/** Implementation of a synchronizer tool that handles code merge. */
public class SynchronizerTool {
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(
        Option.builder()
            .longOpt("source_path")
            .desc("The directory which contains the final code.")
            .hasArg()
            .argName("SOURCE-PATH")
            .required(true)
            .build());
    options.addOption(
        Option.builder()
            .longOpt("generated_path")
            .desc("The directory which contains the generated code.")
            .hasArg()
            .argName("GENERATED-PATH")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("baseline_path")
            .desc("The directory which contains the baseline code.")
            .hasArg()
            .argName("BASELINE-PATH")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("auto_merge")
            .desc("If set, no GUI will be launched if merge can be completed automatically.")
            .argName("AUTO-MERGE")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("auto_resolve")
            .desc("Whether to enable smart conflict resolution")
            .argName("AUTO_RESOLVE")
            .build());
    options.addOption(
        Option.builder()
            .longOpt("ignore_base")
            .desc("If true, the baseline will be ignored and two-way merge will be used.")
            .argName("IGNORE_BASE")
            .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("CodeGeneratorTool", options);
    }

    synchronize(
        cl.getOptionValue("source_path"),
        cl.getOptionValue("generated_path"),
        cl.getOptionValue("baseline_path"),
        cl.hasOption("auto_merge"),
        cl.hasOption("auto_resolve"),
        cl.hasOption("ignore_base"));
  }

  private static void synchronize(
      String sourcePath,
      String generatedPath,
      String baselinePath,
      boolean autoMerge,
      boolean autoResolve,
      boolean ignoreBase) {
    ToolOptions options = ToolOptions.create();
    options.set(Synchronizer.SOURCE_PATH, sourcePath);
    options.set(Synchronizer.GENERATED_PATH, generatedPath);
    options.set(Synchronizer.BASELINE_PATH, baselinePath);
    options.set(Synchronizer.AUTO_MERGE, autoMerge);
    options.set(Synchronizer.AUTO_RESOLUTION, autoResolve);
    options.set(Synchronizer.IGNORE_BASE, ignoreBase);

    Synchronizer synchronizer = new Synchronizer(options);
    synchronizer.run();
  }
}
