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
package com.google.api.codegen.gapic;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.google.api.codegen.ArtifactType;
import com.google.api.tools.framework.snippet.Doc;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.common.collect.Maps;
import java.io.File;
import java.nio.file.FileSystems;
import java.util.Collections;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Code generator unit tests. */
public class GapicGeneratorAppTest {
  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void writeCodeGenOutputAndSetPermissions() throws Exception {
    Map<String, Object> outputFiles = Maps.newHashMap();
    outputFiles.put("tmp.txt", Doc.text("Sample data"));
    outputFiles.put("tmp2.txt", Doc.text("Sample data"));
    outputFiles.put("tmp3", "Sample \"runnable\" data");

    // Verify that files are outputed to a directory.
    String outputDir = tempDir.getRoot().getPath();
    GapicGeneratorApp generator =
        new GapicGeneratorApp(ToolOptions.create(), ArtifactType.LEGACY_GAPIC_AND_PACKAGE);
    generator.writeCodeGenOutput(outputFiles, outputDir);
    generator.setOutputFilesPermissions(Collections.singleton("tmp3"), outputDir);
    assertTrue((new File(outputDir, "tmp.txt")).exists());
    assertTrue((new File(outputDir, "tmp2.txt")).exists());
    assertTrue((new File(outputDir, "tmp3")).exists());
    if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
      assertTrue((new File(outputDir, "tmp3")).canExecute());
    }
    // Verify that files are outputed into a jar file.
    File outputJar = new File(outputDir, "output.jar");
    generator.writeCodeGenOutput(outputFiles, outputJar.getPath());
    generator.setOutputFilesPermissions(Collections.singleton("tmp3"), outputJar.getPath());
    assertTrue(outputJar.exists());
    assertFalse((new File(outputJar.getPath(), "tmp3")).exists());
  }
}
