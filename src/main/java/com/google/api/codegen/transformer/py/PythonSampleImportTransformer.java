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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.OutputContext;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SampleImportTransformer;
import com.google.api.codegen.util.ImportType;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.Collections;
import java.util.List;

public class PythonSampleImportTransformer implements SampleImportTransformer {

  private final ImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();

  public ImportSectionView generateImportSection(
      MethodContext context,
      CallingForm form,
      OutputContext outputContext,
      ImportTypeTable initCodeTypeTable,
      Iterable<InitCodeNode> nodes) {
    ImportSectionView.Builder imports = ImportSectionView.newBuilder();
    ImmutableList.Builder<ImportFileView> appImports = ImmutableList.builder();

    // First get all the imports generated from InitCodeTransformer
    List<ImportFileView> initCodeAppImports =
        importSectionTransformer.generateImportSection(context, nodes).appImports();
    appImports.addAll(initCodeAppImports);

    // Then add more imports that other parts of the sample need
    // For now we only need to special-case `enums`
    boolean addEnumImports =
        outputContext.stringFormattedVariableTypes().stream().anyMatch(TypeModel::isEnum)
            || Streams.stream(nodes).map(InitCodeNode::getType).anyMatch(TypeModel::isEnum);
    if (addEnumImports) {
      ImportTypeView importTypeView =
          ImportTypeView.newBuilder()
              .fullName("enums")
              .type(ImportType.SimpleImport)
              .nickname("")
              .build();
      ImportFileView enumImportView =
          ImportFileView.newBuilder()
              .moduleName(context.getNamer().getVersionedDirectoryNamespace())
              .types(Collections.singletonList(importTypeView))
              .build();
      if (!initCodeAppImports.contains(enumImportView)) {
        appImports.add(enumImportView);
      }
    }
    return imports.appImports(appImports.build()).build();
  }
}
