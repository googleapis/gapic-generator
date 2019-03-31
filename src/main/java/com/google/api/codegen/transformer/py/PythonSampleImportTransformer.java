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

import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.py.PythonImportSectionTransformer;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.OutputView;
import java.util.Collections;
import java.util.List;

public class PythonSampleImportTransformer implements SampleImportTransformer {

  private final ImportSectionTransformer importSectionTransformer = new PythonImportSectionTransformer();
  private final OutputTransformer.OutputImportTransformer OutputImportTransformer = new PythonSampleOutputImportTransformer();

  public ImportSectionView generateImportSection(
      MethodContext context,
      CallingForm form,
      OutputContext outputContext,
      ImportTypeTable initCodeTypeTable,
      Iterable<InitCodeNode> nodes) {
    ImportSectionView.Builder imports = ImportSectionView.newBuilder();
    ImmutableList<ImportFileView> appImports = ImmutableList.builder();
    appImports.addAll(importSectionTransformer.generateImportSection(initCodeTypeTable, nodes).appImports());
    boolean addEnumImports = outputContext.stringFormattedVariableTypes().stream().anyMatch(TypeModel::isEnum);
    if (addEnumImports) {
      ImportTypeView importTypeView =
          ImportTypeView.newBuilder()
              .fullName("enums")
              .type(ImportType.SimpleImport)
              .nickname("")
              .build();
      appImports.add(
          ImportFileView.newBuilder()
              .moduleName(context.getNamer().getVersionedDirectoryNamespace())
              .types(Collections.singletonList(importTypeView))
              .build());
    }
    return imports.appImports(appImports.build()).build();
  } 
}
