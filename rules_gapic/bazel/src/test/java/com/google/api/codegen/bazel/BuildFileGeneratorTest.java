package com.google.api.codegen.bazel;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;

public class BuildFileGeneratorTest {
  private static final String SRC_DIR =
      Paths.get("rules_gapic", "bazel", "src", "test", "data", "googleapis").toString();

  @Test
  public void testGenerateBuildFiles() throws IOException {

    ArgsParser args = new ArgsParser(new String[] {"--src=" + SRC_DIR, "--dest=" + SRC_DIR});
    FileWriter fw = new FileWriter();
    new BuildFileGenerator().generateBuildFiles(args.createApisVisitor(fw));

    Path fileBodyPathPrefix = Paths.get(SRC_DIR, "google", "example", "library");
    String gapicBuildFilePath =
        Paths.get(fileBodyPathPrefix.toString(), "v1", "BUILD.bazel").toString();
    String rawBuildFilePath = Paths.get(fileBodyPathPrefix.toString(), "BUILD.bazel").toString();

    Assert.assertEquals(2, fw.files.size());
    Assert.assertEquals(
        ApisVisitor.readFile(gapicBuildFilePath + ".baseline"), fw.files.get(gapicBuildFilePath));
    Assert.assertEquals(
        ApisVisitor.readFile(rawBuildFilePath + ".baseline"), fw.files.get(rawBuildFilePath));
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
}
