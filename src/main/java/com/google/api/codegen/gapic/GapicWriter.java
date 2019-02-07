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

import com.google.api.codegen.common.GeneratedResult;
import com.google.api.tools.framework.model.DiagCollector;
import java.io.IOException;
import java.util.Map;
import javax.annotation.Nonnull;

/** Interface for classes that write out generator output. */
public interface GapicWriter {

  /** Returns whether the writer has finished writing. */
  boolean isDone();

  /** Write out the generator output. */
  void writeCodeGenOutput(
      @Nonnull Map<String, GeneratedResult<?>> outputFiles, DiagCollector diagCollector)
      throws IOException;
}
