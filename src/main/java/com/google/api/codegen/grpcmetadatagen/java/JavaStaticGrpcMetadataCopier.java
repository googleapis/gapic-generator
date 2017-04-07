/* Copyright 2017 Google Inc
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
package com.google.api.codegen.grpcmetadatagen.java;

import com.google.api.codegen.grpcmetadatagen.GrpcMetadataGenerator;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

/** Responsible for copying static grpc meta-data files for Java */
public class JavaStaticGrpcMetadataCopier {
  private static final String RESOURCE_DIR = "/com/google/api/codegen/metadatagen/java/grpc/";
  private static final ImmutableList<String> GRPC_STATIC_FILES =
      ImmutableList.of(
          "gradlew",
          "gradle/wrapper/gradle-wrapper.jar",
          "gradle/wrapper/gradle-wrapper.properties",
          "gradlew.bat",
          "PUBLISHING.md",
          "templates/apidocs_index.html.template");

  private final String outputDir;

  public JavaStaticGrpcMetadataCopier(ToolOptions options) {
    this.outputDir = options.get(GrpcMetadataGenerator.OUTPUT_DIR);
  }

  public ImmutableMap<String, Doc> run() throws IOException {
    ImmutableMap.Builder<String, Doc> docBuilder = new ImmutableMap.Builder<String, Doc>();
    for (String staticFile : GRPC_STATIC_FILES) {
      Path staticFilePath = Paths.get(staticFile);
      URL input =
          JavaStaticGrpcMetadataCopier.class
              .getResource(Paths.get(RESOURCE_DIR, staticFile).toString());
      createDirectoryIfNecessary(staticFilePath, outputDir);

      Path output = Paths.get(outputDir, staticFile);
      Files.copy(
          Paths.get(input.getPath()),
          output,
          StandardCopyOption.REPLACE_EXISTING,
          StandardCopyOption.COPY_ATTRIBUTES);
    }
    return docBuilder.build();
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
