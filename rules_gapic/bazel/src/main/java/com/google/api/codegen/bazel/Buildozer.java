package com.google.api.codegen.bazel;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class Buildozer {
  private static Buildozer instance = null;
  private static File buildozerBinary = null;
  private final List<String> batch = new ArrayList<String>();

  private Buildozer() {
    if (buildozerBinary == null) {
      throw new RuntimeException("Buildozer binary path is not set.");
    }
  }

  // General purpose execute method. Runs buildozer with the given command line
  // arguments
  public List<String> execute(List<String> args, List<String> stdin) throws IOException {
    ArrayList<String> cmdList = new ArrayList<String>(args);
    cmdList.add(0, buildozerBinary.toString());
    ProcessBuilder processBuilder = new ProcessBuilder(cmdList);
    Process process = processBuilder.start();
    try (Writer processStdin =
        new BufferedWriter(new OutputStreamWriter(process.getOutputStream()))) {
      if (stdin != null) {
        for (String line : stdin) {
          processStdin.write(line + "\n");
        }
      }
    }

    List<String> result = new ArrayList<String>();
    try (BufferedReader processStdout =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      String line = null;
      while ((line = processStdout.readLine()) != null) {
        result.add(line);
      }
    }
    return result;
  }

  // Execute buildozer command for the given target
  public List<String> execute(Path bazelBuildFile, String command, String target)
      throws IOException {
    List<String> args = new ArrayList<String>();
    args.add(command);
    args.add(String.format("%s:%s", bazelBuildFile, target));
    return execute(args, null);
  }

  // Get the value of the given attribute of the given target
  public String getAttribute(Path bazelBuildFile, String target, String attribute)
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
  public void setAttribute(Path bazelBuildFile, String target, String attribute, String value)
      throws IOException {
    execute(bazelBuildFile, String.format("set %s \"%s\"", attribute, value), target);
  }

  // Remove the given attribute of the given target. Apply changes immediately.
  public void removeAttribute(Path bazelBuildFile, String target, String attribute)
      throws IOException {
    execute(bazelBuildFile, String.format("remove %s", attribute), target);
  }

  // Add the value to the given list attribute of the given target. Apply changes
  // immediately.
  public void addAttribute(Path bazelBuildFile, String target, String attribute, String value)
      throws IOException {
    execute(bazelBuildFile, String.format("add %s \"%s\"", attribute, value), target);
  }

  // Set the value to the given attribute of the given target.
  // The changes will be applied when the whole batch is committed with .commit().
  public void batchSetAttribute(Path bazelBuildFile, String target, String attribute, String value)
      throws IOException {
    batch.add(
        String.format("set %s \"%s\"|%s:%s", attribute, value, bazelBuildFile.toString(), target));
  }

  // Remove the given attribute of the given target. Apply changes immediately.
  public void batchRemoveAttribute(Path bazelBuildFile, String target, String attribute)
      throws IOException {
    batch.add(String.format("remove %s|%s:%s", attribute, bazelBuildFile.toString(), target));
  }

  // Add the value to the given list attribute of the given target.
  // The changes will be applied when the whole batch is committed with .commit().
  public void batchAddAttribute(Path bazelBuildFile, String target, String attribute, String value)
      throws IOException {
    batch.add(
        String.format("add %s \"%s\"|%s:%s", attribute, value, bazelBuildFile.toString(), target));
  }

  // Make all changes that are waiting in the batch.
  public void commit() throws IOException {
    if (batch.size() == 0) {
      return;
    }
    List<String> args = new ArrayList<>();
    args.add("-f");
    args.add("-");
    execute(args, batch);
    batch.clear();
  }

  // Get a singleton Buildozer instance
  public static synchronized Buildozer getInstance() throws IOException {
    if (instance == null) {
      instance = new Buildozer();
    }

    return instance;
  }

  public static synchronized void setBinaryPath(String path) {
    buildozerBinary = new File(path);
  }
}
