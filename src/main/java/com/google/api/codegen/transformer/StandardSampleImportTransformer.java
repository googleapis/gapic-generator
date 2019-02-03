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
import java.util.Collections;
import java.util.List;

public class StandardSampleImportTransformer implements SampleImportTransformer {

  private final ImportSectionTransformer importSectionTransformer;

  public StandardSampleImportTransformer(ImportSectionTransformer importSectionTransformer) {
    this.importSectionTransformer = importSectionTransformer;
  }

  public void addSampleBodyImports(MethodContext context, CallingForm form) {
    // default behavior: no types used in the sample body need to be imported
  }

  public void addOutputImports(MethodContext context, List<OutputView> views) {
    // default behavior: no types used in the output part of a sample need to be imported
  }

  public void addInitCodeImports(
      MethodContext context, ImportTypeTable initCodeTypeTable, Iterable<InitCodeNode> nodes) {
    // by default, copy over all types from initCodeTypeTable
    initCodeTypeTable
        .getImports()
        .values()
        .forEach(t -> context.getTypeTable().getAndSaveNicknameFor(t));
  }

  public ImportSectionView generateImportSection(MethodContext context) {
    return importSectionTransformer.generateImportSection(
        context, Collections.<InitCodeNode>emptyList());
  }
}
