package com.google.api.codegen.bazel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Buildozer {
  private static Buildozer instance = null;
  private File tempdir = null;
  private File buildozerBinary = null;
  private List<String> batch = null;

  private Buildozer() throws IOException {
    tempdir = Files.createTempDirectory("build_file_generator_").toFile();
    final String resourcePath = "/rules_gapic/bazel/buildozer.bin";
    final InputStream inputStream = (getClass().getResourceAsStream(resourcePath));
    buildozerBinary = new File(tempdir, "buildozer.bin");
    Files.copy(inputStream, buildozerBinary.getAbsoluteFile().toPath());
    buildozerBinary.setExecutable(true, false);
    buildozerBinary.deleteOnExit();
    tempdir.deleteOnExit();
    batch = new ArrayList<String>();
  }

  // General purpose execute method. Runs buildozer with the given command line
  // arguments
  public List<String> execute(final String[] args, final String[] stdin) throws IOException {
    final ArrayList<String> cmdList = new ArrayList<String>(Arrays.asList(args));
    cmdList.add(0, buildozerBinary.toString());
    final ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
    final Process process = processBuilder.start();
    final BufferedWriter processStdin =
        new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
    final BufferedReader processStdout =
        new BufferedReader(new InputStreamReader(process.getInputStream()));

    if (stdin != null) {
      for (String line : stdin) {
        processStdin.write(line + "\n");
      }
    }
    processStdin.close();

    final List<String> result = new ArrayList<String>();
    String line = null;
    while ((line = processStdout.readLine()) != null) {
      result.add(line);
    }

    return result;
  }

  // Execute buildozer command for the given target
  public List<String> execute(final Path bazelBuildFile, final String command, String target)
      throws IOException {
    if (!target.startsWith(":")) {
      target = ":" + target;
    }
    return execute(new String[] {command, String.format("%s%s", bazelBuildFile, target)}, null);
  }

  // Get the value of the given attribute of the given target
  public String getAttribute(final Path bazelBuildFile, String target, final String attribute)
      throws IOException {
    List<String> executeResult;
    try {
      executeResult = execute(bazelBuildFile, String.format("print %s", attribute), target);
      String value = executeResult.get(0);
      if (value.equals("(missing)")) {
        return null;
      }
      return value;
    } catch (IndexOutOfBoundsException ignored) {
      return null;
    }
  }

  // Set the value to the given attribute of the given target. Apply changes
  // immediately.
  public void setAttribute(
      final Path bazelBuildFile, final String target, final String attribute, final String value)
      throws IOException {
    execute(bazelBuildFile, String.format("set %s \"%s\"", attribute, value), target);
  }

  // Remove the given attribute of the given target. Apply changes immediately.
  public void removeAttribute(
      final Path bazelBuildFile, final String target, final String attribute) throws IOException {
    execute(bazelBuildFile, String.format("remove %s", attribute), target);
  }

  // Add the value to the given list attribute of the given target. Apply changes
  // immediately.
  public void addAttribute(
      final Path bazelBuildFile, final String target, final String attribute, final String value)
      throws IOException {
    execute(bazelBuildFile, String.format("add %s \"%s\"", attribute, value), target);
  }

  // Set the value to the given attribute of the given target.
  // The changes will be applied when the whole batch is committed with .commit().
  public void batchSetAttribute(
      final Path bazelBuildFile, String target, final String attribute, final String value)
      throws IOException {
    if (!target.startsWith(":")) {
      target = ":" + target;
    }
    batch.add(
        String.format("set %s \"%s\"|%s%s", attribute, value, bazelBuildFile.toString(), target));
  }

  // Remove the given attribute of the given target. Apply changes immediately.
  public void batchRemoveAttribute(final Path bazelBuildFile, String target, final String attribute)
      throws IOException {
    if (!target.startsWith(":")) {
      target = ":" + target;
    }
    batch.add(String.format("remove %s|%s%s", attribute, bazelBuildFile.toString(), target));
  }

  // Add the value to the given list attribute of the given target.
  // The changes will be applied when the whole batch is committed with .commit().
  public void batchAddAttribute(
      final Path bazelBuildFile, String target, final String attribute, final String value)
      throws IOException {
    if (!target.startsWith(":")) {
      target = ":" + target;
    }
    batch.add(
        String.format("add %s \"%s\"|%s%s", attribute, value, bazelBuildFile.toString(), target));
  }

  // Make all changes that are waiting in the batch.
  public void commit() throws IOException {
    if (batch.size() == 0) {
      return;
    }
    String[] stdin = new String[batch.size()];
    batch.toArray(stdin);
    batch.clear();
    execute(new String[] {"-f", "-"}, stdin);
  }

  // Get a singleton Buildozer instance
  public static Buildozer getInstance() throws IOException {
    if (instance == null) {
      instance = new Buildozer();
    }

    return instance;
  }
}
