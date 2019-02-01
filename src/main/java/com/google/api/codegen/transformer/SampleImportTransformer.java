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
package com.google.api.codegen.transformer;

import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.OutputView;
import java.util.List;

/** Generates an ImportSection for standalone samples. */
public interface SampleImportTransformer {

  /** Adds all the types to import referenced in the rpc call part of a sample. */
  void addSampleBodyImports(MethodContext context, CallingForm form);

  /**
   * Adds all the types to import referenced in the output handling part of a sample. The output
   * handling does something with the object returned by RPC call, and is usually right above the
   * closed region tag.
   */
  void addOutputImports(MethodContext context, List<OutputView> views);

  /**
   * Adds all the types to import referenced in the initCode part of sample. The initCode is
   * responsible for setting up the rpc call request object and/or parameters, and is usually right
   * below the open region tag.
   */
  void addInitCodeImports(
      MethodContext context, ImportTypeTable initCodeTypeTable, Iterable<InitCodeNode> nodes);

  ImportSectionView generateImportSection(MethodContext context);
}
