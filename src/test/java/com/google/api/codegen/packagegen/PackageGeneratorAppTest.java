/* Copyright 2016 Google LLC
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
package com.google.api.codegen.packagegen;

import com.google.api.codegen.common.GeneratedResult;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Map;
import java.util.TreeMap;
import javax.annotation.Nullable;
import org.junit.Test;

public class PackageGeneratorAppTest extends ConfigBaselineTestCase {
  private class OutputCollector extends SimpleFileVisitor<Path> {
    Map<String, GeneratedResult<Doc>> collectedFiles = new TreeMap<>();
    Path testDir;

    OutputCollector(Path testDir) {
      this.testDir = testDir;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
      String filename = testDir.relativize(file).toString();
      Doc doc = Doc.text(new String(Files.readAllBytes(file)));
      collectedFiles.put(filename, GeneratedResult.create(doc, false));
      return FileVisitResult.CONTINUE;
    }

    public Map<String, GeneratedResult<Doc>> getResults() {
      return collectedFiles;
    }
  }

  private String language;
  private String packageConfig;
  private PackagingArtifactType artifactType;

  @Override
  protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  private void test(
      String name, String packageConfig, String language, PackagingArtifactType artifactType)
      throws Exception {
    this.language = language;
    this.packageConfig = packageConfig;
    this.artifactType = artifactType;
    test(name);
  }

  @Override
  @Nullable
  protected Map<String, Doc> run() throws Exception {
    String outFile = tempDir.getRoot().getPath() + File.separator + baselineFileName();
    String metadataConfigPath = getTestDataLocator().findTestData(packageConfig).getPath();

    ToolOptions options = ToolOptions.create();
    options.set(PackageGeneratorApp.OUTPUT_DIR, outFile);
    options.set(
        PackageGeneratorApp.INPUT_DIR, getTestDataLocator().findTestData("fakeprotodir").getPath());
    options.set(PackageGeneratorApp.PACKAGE_CONFIG2_FILE, metadataConfigPath);
    options.set(PackageGeneratorApp.LANGUAGE, language);
    options.set(PackageGeneratorApp.ARTIFACT_TYPE, artifactType);
    URL dependenciesYamlUrl =
        getTestDataLocator()
            .findTestData("com/google/api/codegen/testsrc/frozen_dependencies.yaml");
    Preconditions.checkNotNull(dependenciesYamlUrl);
    Map<String, GeneratedResult<Doc>> generatedDocs =
        new PackageGeneratorApp(options, dependenciesYamlUrl).generate(model);

    if (TargetLanguage.fromString(language) == TargetLanguage.PYTHON) {
      OutputCollector collector = new OutputCollector(Paths.get(outFile));
      Files.walkFileTree(Paths.get(outFile), collector);
      return new ImmutableMap.Builder<String, Doc>()
          .putAll(GeneratedResult.extractBodies(generatedDocs))
          .putAll(GeneratedResult.extractBodies(collector.getResults()))
          .build();
    } else {
      return new ImmutableMap.Builder<String, Doc>()
          .putAll(GeneratedResult.extractBodies(generatedDocs))
          .build();
    }
  }

  // Tests
  // =====
  @Test
  public void java_library() throws Exception {
    test("library", "library_pkg2.yaml", "java", PackagingArtifactType.PROTOBUF);
  }

  @Test
  public void java_grpc_stubs() throws Exception {
    test("library", "library_stubs_pkg2.yaml", "java", PackagingArtifactType.GRPC);
  }

  @Test
  public void java_common_protos() throws Exception {
    test("library", "common_protos_pkg2.yaml", "java", PackagingArtifactType.PROTOBUF);
  }

  @Test
  public void python_library() throws Exception {
    test("library", "library_pkg2.yaml", "python", PackagingArtifactType.GRPC);
  }
}
