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
package com.google.api.codegen.grpcmetadatagen;

import com.google.api.tools.framework.model.testing.ConfigBaselineTestCase;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.IOException;
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

public class PackageMetadataGeneratorTest extends ConfigBaselineTestCase {

  private class OutputCollector extends SimpleFileVisitor<Path> {
    Map<String, Doc> collectedFiles = new TreeMap<>();
    Path testDir;

    OutputCollector(Path testDir) {
      this.testDir = testDir;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attr) throws IOException {
      collectedFiles.put(
          testDir.relativize(file).toString(), Doc.text(new String(Files.readAllBytes(file))));
      return FileVisitResult.CONTINUE;
    }

    public Map<String, Doc> getResults() {
      return collectedFiles;
    }
  }

  private String language;

  @Override
  protected boolean suppressDiagnosis() {
    // Suppress linter warnings
    return true;
  }

  private void test(String name, String language) throws Exception {
    this.language = language;
    test(name);
  }

  @Override
  @Nullable
  protected Object run() throws Exception {
    String outFile = tempDir.getRoot().getPath() + File.separator + baselineFileName();
    String metadataConfigPath = getTestDataLocator().findTestData("library_pkg.yaml").getPath();

    ToolOptions options = ToolOptions.create();
    options.set(GrpcMetadataGenerator.OUTPUT_DIR, outFile);
    options.set(
        GrpcMetadataGenerator.INPUT_DIR,
        getTestDataLocator().findTestData("fakeprotodir").getPath());
    options.set(GrpcMetadataGenerator.METADATA_CONFIG_FILE, metadataConfigPath);
    options.set(GrpcMetadataGenerator.LANGUAGE, language);
    Map<String, Doc> generatedDocs = new GrpcMetadataGenerator(options).generateDocs(model);
    OutputCollector collector = new OutputCollector(Paths.get(outFile));
    Files.walkFileTree(Paths.get(outFile), collector);
    return new ImmutableMap.Builder<String, Doc>()
        .putAll(generatedDocs)
        .putAll(collector.getResults())
        .build();
  }

  // Tests
  // =====

  @Test
  public void python_library() throws Exception {
    test("library", "python");
  }
}
