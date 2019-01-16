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

public class SampleImportTransformer {

  private final ImportSectionTransformer importSectionTransformer;

  public SampleImportTransformer(ImportSectionTransformer importSectionTransformer) {
    this.importSectionTransformer = importSectionTransformer;
  }

  protected void addSampleBodyImports(MethodContext context, CallingForm form) {}

  protected void addOutputImports(MethodContext context, List<OutputView> views) {}

  protected void addInitCodeImports(
      MethodContext context, ImportTypeTable initCodeTypeTable, Iterable<InitCodeNode> nodes) {
    initCodeTypeTable
        .getImports()
        .values()
        .forEach(t -> context.getTypeTable().getAndSaveNicknameFor(t));
  }

  public ImportSectionView toImportSectionView(
      MethodContext context,
      CallingForm form,
      List<OutputView> views,
      ImportTypeTable initCodeTypeTable,
      Iterable<InitCodeNode> nodes) {
    addSampleBodyImports(context, form);
    addOutputImports(context, views);
    addInitCodeImports(context, initCodeTypeTable, nodes);
    return importSectionTransformer.generateImportSection(context, nodes);
  }
}
