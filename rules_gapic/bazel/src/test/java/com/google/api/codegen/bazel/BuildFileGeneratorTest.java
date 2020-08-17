package com.google.api.codegen.bazel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class BuildFileGeneratorTest {
  private static final String SRC_DIR = Paths.get("googleapis").toString();
  private static final String PATH_PREFIX =
      Paths.get("rules_gapic", "bazel", "src", "test", "data").toString();

  @Test
  public void testGenerateBuildFiles() throws IOException {
    String buildozerPath = getBuildozerPath();
    ArgsParser args =
        new ArgsParser(new String[] {"--buildozer=" + buildozerPath, "--src=" + SRC_DIR});
    FileWriter fw = new FileWriter();
    new BuildFileGenerator().generateBuildFiles(args.createApisVisitor(fw, PATH_PREFIX));

    Path fileBodyPathPrefix = Paths.get(PATH_PREFIX, SRC_DIR, "google", "example", "library");
    String gapicBuildFilePath =
        Paths.get(fileBodyPathPrefix.toString(), "v1", "BUILD.bazel").toString();
    String rawBuildFilePath = Paths.get(fileBodyPathPrefix.toString(), "BUILD.bazel").toString();

    Assert.assertEquals(2, fw.files.size());
    Assert.assertEquals(
        ApisVisitor.readFile(gapicBuildFilePath + ".baseline"), fw.files.get(gapicBuildFilePath));
    Assert.assertEquals(
        ApisVisitor.readFile(rawBuildFilePath + ".baseline"), fw.files.get(rawBuildFilePath));
  }

  @Test
  public void testRegeneration() throws IOException, InterruptedException {
    // In this test we run the generator twice, changing the generated
    // google/example/library/v1/BUILD.bazel
    // after the first run, and verifying that some changed values are preserved
    // (and some are not).
    Path tempDirPath = getTemporaryDirectory();

    // I'm lazy, so let's just "cp -r" stuff.
    Path fixturesPath = Paths.get(PATH_PREFIX, SRC_DIR);
    new ProcessBuilder(new String[] {"cp", "-r", fixturesPath.toString(), tempDirPath.toString()})
        .start()
        .waitFor();

    String buildozerPath = getBuildozerPath();
    Path copiedGoogleapis = Paths.get(tempDirPath.toString(), "googleapis");
    ArgsParser args =
        new ArgsParser(new String[] {"--buildozer=" + buildozerPath, "--src=" + copiedGoogleapis});
    new BuildFileGenerator()
        .generateBuildFiles(args.createApisVisitor(null, tempDirPath.toString()));

    Path fileBodyPathPrefix =
        Paths.get(copiedGoogleapis.toString(), "google", "example", "library");
    Path gapicBuildFilePath = Paths.get(fileBodyPathPrefix.toString(), "v1", "BUILD.bazel");
    String rawBuildFilePath = Paths.get(fileBodyPathPrefix.toString(), "BUILD.bazel").toString();

    Assert.assertEquals(
        ApisVisitor.readFile(gapicBuildFilePath.toString() + ".baseline"),
        ApisVisitor.readFile(gapicBuildFilePath.toString()));
    Assert.assertEquals(
        ApisVisitor.readFile(rawBuildFilePath + ".baseline"),
        ApisVisitor.readFile(rawBuildFilePath));

    // Now change some values in google/example/library/v1/BUILD.bazel
    Buildozer.setBinaryPath(buildozerPath);
    Buildozer buildozer = Buildozer.getInstance();
    // The following values should be preserved:
    buildozer.batchSetAttribute(
        gapicBuildFilePath, "library_nodejs_gapic", "package_name", "@google-cloud/library");
    buildozer.batchRemoveAttribute(
        gapicBuildFilePath, "library_nodejs_gapic", "extra_protoc_parameters");
    buildozer.batchAddAttribute(
        gapicBuildFilePath, "library_nodejs_gapic", "extra_protoc_parameters", "param1");
    buildozer.batchAddAttribute(
        gapicBuildFilePath, "library_nodejs_gapic", "extra_protoc_parameters", "param2");
    buildozer.batchSetAttribute(
        gapicBuildFilePath,
        "google-cloud-example-library-v1-csharp",
        "name",
        "renamed_csharp_rule");
    buildozer.batchSetAttribute(
        gapicBuildFilePath, "google-cloud-example-library-v1-java", "name", "renamed_java_rule");

    // The following values should NOT be preserved:
    buildozer.batchSetAttribute(
        gapicBuildFilePath,
        "library_nodejs_gapic",
        "grpc_service_config",
        "fake_grpc_service_config");

    buildozer.commit();

    // Run the generator again
    new BuildFileGenerator()
        .generateBuildFiles(args.createApisVisitor(null, tempDirPath.toString()));

    // Check that values are preserved
    Assert.assertEquals(
        "@google-cloud/library",
        buildozer.getAttribute(gapicBuildFilePath, "library_nodejs_gapic", "package_name"));
    Assert.assertEquals(
        "[param1 param2]",
        buildozer.getAttribute(
            gapicBuildFilePath, "library_nodejs_gapic", "extra_protoc_parameters"));
    Assert.assertEquals(
        "renamed_csharp_rule",
        buildozer.getAttribute(gapicBuildFilePath, "%csharp_gapic_assembly_pkg", "name"));
    Assert.assertEquals(
        "renamed_java_rule",
        buildozer.getAttribute(gapicBuildFilePath, "%java_gapic_assembly_gradle_pkg", "name"));
    // Check that grpc_service_config value is not preserved:
    Assert.assertEquals(
        "library_example_grpc_service_config.json",
        buildozer.getAttribute(gapicBuildFilePath, "library_nodejs_gapic", "grpc_service_config"));

    // Now run with overwrite and verify it actually ignores all the changes
    ArgsParser argsOverwrite =
        new ArgsParser(
            new String[] {
              "--overwrite", "--buildozer=" + buildozerPath, "--src=" + copiedGoogleapis
            });
    new BuildFileGenerator()
        .generateBuildFiles(argsOverwrite.createApisVisitor(null, tempDirPath.toString()));
    Assert.assertEquals(
        ApisVisitor.readFile(gapicBuildFilePath.toString() + ".baseline"),
        ApisVisitor.readFile(gapicBuildFilePath.toString()));
    Assert.assertEquals(
        ApisVisitor.readFile(rawBuildFilePath + ".baseline"),
        ApisVisitor.readFile(rawBuildFilePath));
  }

  @Test
  public void testBuildozer() throws IOException {
    Path tempDirPath = getTemporaryDirectory();
    Path templateFile = Paths.get(PATH_PREFIX, "buildozer", "BUILD.bazel.template");
    Path buildBazel = Paths.get(tempDirPath.toString(), "BUILD.bazel");
    Files.copy(templateFile, buildBazel);

    String buildozerPath = getBuildozerPath();
    Buildozer.setBinaryPath(buildozerPath);
    Buildozer buildozer = Buildozer.getInstance();

    // Get some attributes
    Assert.assertEquals("rule1", buildozer.getAttribute(buildBazel, "rule1", "name"));
    Assert.assertEquals("attr_value", buildozer.getAttribute(buildBazel, "rule1", "attr"));
    Assert.assertEquals(
        "[value1 value2]", buildozer.getAttribute(buildBazel, "rule2", "list_attr"));

    // Set some attributes and get the result
    buildozer.setAttribute(buildBazel, "rule1", "attr", "new_attr_value");
    buildozer.addAttribute(buildBazel, "rule2", "list_attr", "value3");
    Assert.assertEquals("new_attr_value", buildozer.getAttribute(buildBazel, "rule1", "attr"));
    Assert.assertEquals(
        "[value1 value2 value3]", buildozer.getAttribute(buildBazel, "rule2", "list_attr"));

    // Remove attribute
    Assert.assertEquals("remove_a", buildozer.getAttribute(buildBazel, "rule1", "to_be_removed_a"));
    buildozer.removeAttribute(buildBazel, "rule1", "to_be_removed_a");
    Assert.assertEquals(null, buildozer.getAttribute(buildBazel, "rule1", "to_be_removed_a"));

    // Test batch operations
    buildozer.batchSetAttribute(buildBazel, "rule1", "attr", "new_batch_attr_value");
    buildozer.batchAddAttribute(buildBazel, "rule2", "list_attr", "value4");
    buildozer.batchRemoveAttribute(buildBazel, "rule1", "to_be_removed_b");
    // before commit: old values
    Assert.assertEquals("new_attr_value", buildozer.getAttribute(buildBazel, "rule1", "attr"));
    Assert.assertEquals(
        "[value1 value2 value3]", buildozer.getAttribute(buildBazel, "rule2", "list_attr"));
    Assert.assertEquals("remove_b", buildozer.getAttribute(buildBazel, "rule1", "to_be_removed_b"));
    buildozer.commit();
    // after commit: new values
    Assert.assertEquals(
        "new_batch_attr_value", buildozer.getAttribute(buildBazel, "rule1", "attr"));
    Assert.assertEquals(
        "[value1 value2 value3 value4]", buildozer.getAttribute(buildBazel, "rule2", "list_attr"));
    Assert.assertEquals(null, buildozer.getAttribute(buildBazel, "rule1", "to_be_removed_b"));
  }

  // Not using mocking libraries to keep this tool as simple as possible (currently it does not use
  // any dependencies). Tests depend only on JUnit.
  private static class FileWriter implements ApisVisitor.FileWriter {
    private final Map<String, String> files = new HashMap<>();

    @Override
    public void write(Path dest, String fileBody) throws IOException {
      files.put(dest.toString(), fileBody);
    }
  }

  // Get a path to some temporary directory.
  // If we are run by "bazel test", we expect to have TEST_TMPDIR defined by bazel
  // and we're free to use it. Otherwise, just get some temporary directory.
  private static Path getTemporaryDirectory() throws IOException {
    String bazelTempDir = System.getenv().get("TEST_TMPDIR");
    Path tempDirPath;
    if (bazelTempDir == null) {
      tempDirPath = Files.createTempDirectory("build_file_generator_test_");
    } else {
      tempDirPath = Paths.get(bazelTempDir);
    }
    return tempDirPath;
  }

  private static String getBuildozerPath() {
    String path = System.getProperty("buildozer");
    if (path == null) {
      throw new RuntimeException("Use -Dbuildozer=/path/to/buildozer for testing.");
    }
    return path;
  }
}
