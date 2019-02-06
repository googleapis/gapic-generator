/* Copyright 2019 Google LLC
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

import static com.google.api.codegen.gapic.GapicGeneratorApp.OUTPUT_FILE;

import autovalue.shaded.com.google.common.common.collect.ImmutableMap;
import com.google.api.codegen.common.GeneratedResult;
import com.google.api.tools.framework.model.Diag;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.SimpleLocation;
import com.google.api.tools.framework.tools.ToolOptions;
import com.google.api.tools.framework.tools.ToolUtil;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/** A class that writes Gapic output to disk. */
public class FileGapicWriter implements GapicWriter {

  private final ToolOptions options;

  private boolean isDone = false;

  // TODO(andrealin): autovalue this
  public FileGapicWriter(ToolOptions toolOptions) {
    this.options = toolOptions;
  }

  @Override
  public boolean isDone() {
    return isDone;
  }

  @Override
  public void writeCodeGenOutput(
      @Nonnull Map<String, GeneratedResult> generatedResults, Model model) throws IOException {
    Map<String, Object> outputFiles =
        generatedResults
            .entrySet()
            .stream()
            .collect(
                Collector.of(
                    ImmutableMap.Builder<String, Object>::new,
                    (b, e) -> b.put(e.getKey(), e.getValue().getBody()),
                    (b1, b2) -> b1.putAll(b2.build()),
                    ImmutableMap.Builder::build));

    String outputPath = options.get(OUTPUT_FILE);
    writeCodeGenOutput(outputFiles, outputPath);

    Set<String> executables =
        generatedResults
            .entrySet()
            .stream()
            .filter(e -> e.getValue().isExecutable())
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());
    setOutputFilesPermissions(executables, outputPath, model);

    isDone = true;
  }

  @VisibleForTesting
  void writeCodeGenOutput(Map<String, Object> outputFiles, String outputPath) throws IOException {
    // TODO: Support zip output.
    if (outputPath.endsWith(".jar") || outputPath.endsWith(".srcjar")) {
      ToolUtil.writeJar(outputFiles, outputPath);
    } else {
      ToolUtil.writeFiles(outputFiles, outputPath);
    }
  }

  @VisibleForTesting
  void setOutputFilesPermissions(Set<String> executables, String outputPath, Model model) {
    if (outputPath.endsWith(".jar")) {
      return;
    }

    for (String executable : executables) {
      File file =
          Strings.isNullOrEmpty(outputPath)
              ? new File(executable)
              : new File(outputPath, executable);
      if (!file.setExecutable(true, false)) {
        warning(
            model,
            "Failed to set output file as executable. Probably running on a non-POSIX system.");
      }
    }
  }

  private void warning(Model model, String message, Object... args) {
    model
        .getDiagReporter()
        .getDiagCollector()
        .addDiag(Diag.warning(SimpleLocation.TOPLEVEL, message, args));
  }
}
