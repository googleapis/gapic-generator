/* Copyright 2016 Google LLC
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
package com.google.api.codegen;

import static org.junit.Assert.assertTrue;

import com.google.api.tools.framework.snippet.Doc;
import com.google.common.collect.Maps;
import java.io.File;
import java.util.Map;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/** Code generator unit tests. */
public class CodeGeneratorApiTest {
  @Rule public TemporaryFolder tempDir = new TemporaryFolder();

  @Test
  public void writeCodeGenOutput() throws Exception {
    Map<String, Doc> outputFiles = Maps.newHashMap();
    outputFiles.put("tmp.txt", Doc.text("Sample data"));
    outputFiles.put("tmp2.txt", Doc.text("Sample data"));

    // Verify that files are outputed to a directory.
    String outputDir = tempDir.getRoot().getPath();
    CodeGeneratorApi.writeCodeGenOutput(outputFiles, outputDir);
    assertTrue((new File(outputDir, "tmp.txt")).exists());
    assertTrue((new File(outputDir, "tmp2.txt")).exists());

    // Verify that files are outputed into a jar file.
    File outputJar = new File(outputDir, "output.jar");
    CodeGeneratorApi.writeCodeGenOutput(outputFiles, outputJar.getPath());
    assertTrue(outputJar.exists());
  }
}
