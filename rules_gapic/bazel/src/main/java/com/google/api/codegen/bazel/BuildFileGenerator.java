package com.google.api.codegen.bazel;

import java.io.IOException;
import java.nio.file.Files;

// To run from bazel and overwrite existing BUILD.bazel files:
//    bazel run //rules_gapic/bazel:build_file_generator -- \
//        --src=rules_gapic/bazel/src/test/data/googleapis
//
// To compile and copy resources manually from the repository root directory (no bazel needed):
//    javac -d . rules_gapic/bazel/src/main/java/com/google/api/codegen/bazel/*.java
//    cp rules_gapic/bazel/src/main/java/com/google/api/codegen/bazel/*.mustache
// Then to run manually:
//    java -cp . com.google.api.codegen.bazel.BuildFileGenerator \
//        --src=rules_gapic/bazel/src/test/data/googleapis
public class BuildFileGenerator {
  public static void main(String[] args) throws IOException {
    BuildFileGenerator bfg = new BuildFileGenerator();
    ApisVisitor visitor =
        new ArgsParser(args).createApisVisitor(null, System.getenv("BUILD_WORKSPACE_DIRECTORY"));
    bfg.generateBuildFiles(visitor);
  }

  void generateBuildFiles(ApisVisitor visitor) throws IOException {
    System.out.println("\n\n========== READING INPUT DIRECTORY ==========");
    Files.walkFileTree(visitor.getSrcDir(), visitor);
    visitor.setWriterMode(true);
    System.out.println("\n\n========== WRITING GENERATED FILES ==========");
    Files.walkFileTree(visitor.getSrcDir(), visitor);

    System.out.println("\nBUILD.bazel file generation completed successfully\n");
  }
}
