package io.gapi.vgen;

import com.google.api.tools.framework.tools.ToolOptions;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import io.gapi.vsync.Synchronizer;

/**
 * Implementation of a synchronizer tool that handles code merge.
 */
public class SynchronizerTool {
  public static void main(String[] args) throws Exception {
    Options options = new Options();
    options.addOption("h", "help", false, "show usage");
    options.addOption(Option.builder()
        .longOpt("source_path")
        .desc("The directory which contains the final code.")
        .hasArg()
        .argName("SOURCE-PATH")
        .required(true)
        .build());
    options.addOption(Option.builder()
        .longOpt("generated_path")
        .desc("The directory which contains the generated code.")
        .hasArg()
        .argName("GENERATED-PATH")
        .build());
    options.addOption(Option.builder()
        .longOpt("baseline_path")
        .desc("The directory which contains the baseline code.")
        .hasArg()
        .argName("BASELINE-PATH")
        .build());
    options.addOption(Option.builder()
        .longOpt("auto_merge")
        .desc("If set, no GUI will be launched if merge can be completed automatically.")
        .argName("AUTO-MERGE")
        .build());
    options.addOption(Option.builder()
        .longOpt("auto_resolve")
        .desc("Whether to enable smart conflict resolution")
        .argName("AUTO_RESOLVE")
        .build());
    options.addOption(Option.builder()
        .longOpt("ignore_base")
        .desc("If true, the baseline will be ignored and two-way merge will be used.")
        .argName("IGNORE_BASE")
        .build());

    CommandLine cl = (new DefaultParser()).parse(options, args);
    if (cl.hasOption("help")) {
      HelpFormatter formater = new HelpFormatter();
      formater.printHelp("CodeGeneratorTool", options);
    }

    synchronize(cl.getOptionValue("source_path"),
        cl.getOptionValue("generated_path"),
        cl.getOptionValue("baseline_path"),
        cl.hasOption("auto_merge"),
        cl.hasOption("auto_resolve"),
        cl.hasOption("ignore_base"));
  }

  private static void synchronize(
      String sourcePath, String generatedPath, String baselinePath,
      boolean autoMerge, boolean autoResolve, boolean ignoreBase) {
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


