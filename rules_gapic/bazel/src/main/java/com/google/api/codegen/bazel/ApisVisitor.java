package com.google.api.codegen.bazel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;

class ApisVisitor extends SimpleFileVisitor<Path> {
  @FunctionalInterface
  interface FileWriter {
    void write(Path dest, String fileBody) throws IOException;
  }

  private Map<String, ApiVersionedDir> bazelApiVerPackages = new TreeMap<>();
  private Map<String, ApiDir> bazelApiPackages = new TreeMap<>();
  private final BazelBuildFileTemplate gapicApiTempl;
  private final BazelBuildFileTemplate rootApiTempl;
  private final BazelBuildFileTemplate rawApiTempl;
  private final Path srcDir;
  private final Path destDir;
  private boolean writerMode;
  private final FileWriter fileWriter;

  ApisVisitor(
      Path srcDir,
      Path destDir,
      String gapicApiTempl,
      String rootApiTempl,
      String rawApiTempl,
      FileWriter fileWriter) {
    this.gapicApiTempl = new BazelBuildFileTemplate(gapicApiTempl);
    this.rootApiTempl = new BazelBuildFileTemplate(rootApiTempl);
    this.rawApiTempl = new BazelBuildFileTemplate(rawApiTempl);
    this.srcDir = srcDir.normalize();
    this.destDir = destDir.normalize();
    this.writerMode = false;
    this.fileWriter =
        (fileWriter != null)
            ? fileWriter
            : (dest, fileBody) -> Files.write(dest, fileBody.getBytes(StandardCharsets.UTF_8));
  }

  public Path getSrcDir() {
    return srcDir;
  }

  @Override
  public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
    if (writerMode) {
      return FileVisitResult.CONTINUE;
    }
    System.out.println("Scan Directory: " + dir.toString());

    String dirStr = dir.toString();

    ApiVersionedDir bp = new ApiVersionedDir();
    bazelApiVerPackages.put(dirStr, bp);
    ApiDir bap = new ApiDir();
    bazelApiPackages.put(dirStr, bap);

    int parentDirIndex = dirStr.lastIndexOf(File.separator);
    String parentDirStr = dirStr.substring(0, parentDirIndex);
    bp.setParent(bazelApiPackages.get(parentDirStr));

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
    if (writerMode) {
      return FileVisitResult.CONTINUE;
    }

    System.out.println("    Read File: " + file.toString());

    String packageLocation = file.getParent().toString();
    ApiVersionedDir bp = bazelApiVerPackages.get(packageLocation);
    ApiDir bap = bazelApiPackages.get(packageLocation);

    if (bp == null || bap == null) {
      return FileVisitResult.CONTINUE;
    }

    String fileName = file.getFileName().toString();
    if (fileName.endsWith(".yaml")) {
      if (!fileName.endsWith(".legacy.yaml")) {
        String fileBody = readFile(file);
        bp.parseYamlFile(fileName, fileBody);
        bap.parseYamlFile(fileName, fileBody);
      }
    } else if (fileName.endsWith(".proto")) {
      bp.parseProtoFile(fileName, readFile(file));
    } else if (fileName.endsWith(".bazel")) {
      // Consider merging BUILD.bazel files if it becomes necessary (i.e. people will be doing many
      // valuable manual edits in their BUILD.bazel files). This will complicate the whole logic
      // so not doing it for now, hoping it will not be required.
    } else if (fileName.endsWith(".json")) {
      bp.parseJsonFile(fileName, readFile(file));
    }

    return FileVisitResult.CONTINUE;
  }

  @Override
  public FileVisitResult postVisitDirectory(Path dir, IOException e) throws IOException {
    if (!writerMode) {
      return FileVisitResult.CONTINUE;
    }

    String dirStr = dir.toString();
    ApiVersionedDir bp = bazelApiVerPackages.get(dirStr);
    BazelBuildFileTemplate template = null;
    String tmplType = "";
    if (bp.getProtoPackage() != null) {
      if (bp.getGapicYamlPath() != null) {
        bp.injectFieldsFromTopLevel();
        template = this.gapicApiTempl;
        tmplType = "GAPIC_VERSIONED";
      } else if (!bp.getLangProtoPackages().isEmpty()) {
        template = this.rawApiTempl;
        tmplType = "RAW";
      }
    } else if (bp.getServiceYamlPath() != null) {
      template = this.rootApiTempl;
      tmplType = "API_ROOT";
    }

    if (template == null) {
      return FileVisitResult.CONTINUE;
    }

    String rootDirStr = srcDir.toString();
    String outDirPath = destDir.toString() + dirStr.substring(rootDirStr.length());
    File outDir = new File(outDirPath);

    if (!outDir.exists()) {
      if (!outDir.mkdirs()) {
        System.out.println("WARNING: Could not create directory: " + outDir.toString());
        return FileVisitResult.CONTINUE;
      }
    }

    System.out.println(
        "Write File [" + tmplType + "]: " + outDir.toString() + File.separator + "BUILD.bazel");
    try {
      BazelBuildFileView bpv = new BazelBuildFileView(bp);
      fileWriter.write(Paths.get(outDir.toString(), "BUILD.bazel"), template.expand(bpv));
    } catch (RuntimeException ex) {
      ex.printStackTrace();
    }

    return FileVisitResult.CONTINUE;
  }

  public static String readFile(String path) throws IOException {
    return new String(Files.readAllBytes(Paths.get(path).normalize()), StandardCharsets.UTF_8);
  }

  private static String readFile(Path path) throws IOException {
    return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
  }

  public void setWriterMode(boolean writerMode) {
    this.writerMode = writerMode;
  }
}
