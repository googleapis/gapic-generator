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

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

/**
 * A GapicProvider performs file generation (code, static resources, etc.) using a proto-based Model
 * for a particular language.
 */
public interface GapicProvider {
  /**
   * Returns the file names that this provider will use for generation. They can be static files,
   * snippet templates or any other type of files, depending on the actual provider implementation.
   */
  Collection<String> getInputFileNames();

  /** Returns a set of relative file paths to generated files, which must be set executable. */
  Collection<String> getOutputExecutableNames();

  /** Runs code generation and returns a map from relative file paths to generated files. */
  Map<String, ?> generate() throws IOException;
}
