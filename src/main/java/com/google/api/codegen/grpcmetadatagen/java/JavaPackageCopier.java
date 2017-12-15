/* Copyright 2017 Google LLC
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
package com.google.api.codegen.grpcmetadatagen.java;

import com.google.api.codegen.gapic.StaticFileRunner;
import com.google.api.codegen.grpcmetadatagen.GrpcMetadataGenerator;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

/** Responsible for copying package files and static meta-data files for Java */
public class JavaPackageCopier implements StaticFileRunner {
  private static final String RESOURCE_DIR = "/com/google/api/codegen/metadatagen/java/grpc/";

  private final ImmutableList<String> staticFiles;

  private final String inputDir;
  private final String outputDir;

  private class JavaPackageFileVisitor extends SimpleFileVisitor<Path> {
    Path inputPath;
    Path outputPath;

    public JavaPackageFileVisitor(Path inputPath, Path outputPath) {
      this.inputPath = inputPath;
      this.outputPath = outputPath;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
      Path destination = outputPath.resolve(inputPath.relativize(file));
      Files.createDirectories(destination.getParent());
      Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
      return FileVisitResult.CONTINUE;
    }
  }

  public JavaPackageCopier(List<String> staticFiles, ToolOptions options) {
    this.inputDir = options.get(GrpcMetadataGenerator.INPUT_DIR);
    this.outputDir = options.get(GrpcMetadataGenerator.OUTPUT_DIR);
    this.staticFiles = ImmutableList.copyOf(staticFiles);
  }

  public JavaPackageCopier(List<String> staticFiles, String outputDir) {
    this.inputDir = null;
    this.outputDir = outputDir;
    this.staticFiles = ImmutableList.copyOf(staticFiles);
  }

  public void run() throws IOException {
    if (inputDir != null) {
      // Copy all files in the input dir
      Path inputPath = Paths.get(inputDir);
      Path outputPath = Paths.get(outputDir);
      JavaPackageFileVisitor visitor = new JavaPackageFileVisitor(inputPath, outputPath);
      Files.walkFileTree(inputPath, visitor);
    }

    // Copy static files
    for (String staticFile : staticFiles) {
      Path staticFilePath = Paths.get(staticFile);
      URL input =
          JavaPackageCopier.class.getResource(Paths.get(RESOURCE_DIR, staticFile).toString());
      createDirectoryIfNecessary(staticFilePath, outputDir);

      Path output = Paths.get(outputDir, staticFile);
      Files.copy(
          Paths.get(input.getPath()),
          output,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    }
  }

  private void createDirectoryIfNecessary(Path staticFilePath, String outputDir)
      throws IOException {
    Path destination = Paths.get(outputDir);
    if (staticFilePath.getParent() != null) {
      destination = Paths.get(outputDir, staticFilePath.getParent().toString());
    }
    if (!Files.exists(destination)) {
      Files.createDirectories(destination);
    }
  }
}
