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
package com.google.api.codegen.metadatagen.py;

import com.google.api.codegen.metadatagen.PackageCopier;
import com.google.api.codegen.metadatagen.PackageCopierResult;
import com.google.api.codegen.metadatagen.PackageMetadataGenerator;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

/**
 * A PackageCopier specialized to calculate Python namespace packages and generate __init__.py
 * files.
 */
public class PythonPackageCopier implements PackageCopier {

  /** Copies gRPC source while computing namespace packages and generating __init__.py. */
  private class PythonPackageFileVisitor extends SimpleFileVisitor<Path> {
    ImmutableMap.Builder<String, Doc> docBuilder = new ImmutableMap.Builder<String, Doc>();
    List<String> pythonNamespacePackages = new ArrayList<>();
    Path inputPath;
    Path outputPath;
    String apiVersion;

    /**
     * Constructor.
     *
     * @param inputPath The path to the (unprocessed) gRPC source code.
     * @param apiVersion The major version of the API.
     */
    public PythonPackageFileVisitor(Path inputPath, Path outputPath, String apiVersion) {
      this.inputPath = inputPath;
      this.outputPath = outputPath;
      this.apiVersion = apiVersion;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
      Path destination = outputPath.resolve(inputPath.relativize(file));
      Files.createDirectories(destination.getParent());
      Files.copy(file, destination, StandardCopyOption.REPLACE_EXISTING);
      return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
      // Top-level dir doesn't need an __init__.py
      if (dir.equals(inputPath)) {
        return FileVisitResult.CONTINUE;
      }

      String outFile = inputPath.relativize(dir.resolve("__init__.py")).toString();
      // Version directory gets an empty __init__.py
      if (dir.getFileName().toString().equals(apiVersion)) {
        docBuilder.put(outFile, Doc.text("\n"));

        // All others get become namespace packages
      } else {
        docBuilder.put(
            outFile, Doc.text("__import__('pkg_resources').declare_namespace(__name__)\n"));
        pythonNamespacePackages.add(Joiner.on(".").join(inputPath.relativize(dir).iterator()));
      }
      return FileVisitResult.CONTINUE;
    }

    public List<String> getNamespacePackages() {
      return pythonNamespacePackages;
    }

    public ImmutableMap.Builder<String, Doc> getDocBuilder() {
      return docBuilder;
    }
  }

  @SuppressWarnings("unchecked")
  @Override
  public PackageCopierResult run(ToolOptions options) throws IOException {
    // Copy files from dir into map, and fill in namespace result
    // Run __init__ snippet in each dir that deserves it
    PythonPackageFileVisitor visitor =
        new PythonPackageFileVisitor(
            Paths.get(options.get(PackageMetadataGenerator.INPUT_DIR)),
            Paths.get(options.get(PackageMetadataGenerator.OUTPUT_DIR)),
            options.get(PackageMetadataGenerator.API_VERSION));

    Files.walkFileTree(Paths.get(options.get(PackageMetadataGenerator.INPUT_DIR)), visitor);

    List<String> pythonNamespacePackages = visitor.getNamespacePackages();
    ImmutableMap.Builder<String, Doc> docBuilder = visitor.getDocBuilder();

    return PackageCopierResult.createPython(pythonNamespacePackages, docBuilder.build());
  }
}
