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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.OutputContext;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.SampleImportTransformer;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.Collections;

public class NodeJSSampleImportTransformer implements SampleImportTransformer {

  public ImportSectionView generateImportSection(
      MethodContext context,
      CallingForm form,
      OutputContext outputContext,
      ImportTypeTable initCodeTypeTable,
      Iterable<InitCodeNode> nodes) {
    ImportSectionView.Builder imports = ImportSectionView.newBuilder();
    ImmutableList.Builder<ImportFileView> appImports = ImmutableList.builder();

    boolean shouldImportUtil = outputContext.hasFileOutput();

    boolean shouldImportFs =
        Streams.stream(nodes)
                .anyMatch(node -> node.getLineType() == InitCodeLineType.ReadFileInitLine)
            || outputContext.hasFileOutput();

    if (shouldImportUtil) {
      appImports.add(
          ImportFileView.newBuilder().moduleName("util").types(Collections.emptyList()).build());
    }
    if (shouldImportFs) {
      appImports.add(
          ImportFileView.newBuilder().moduleName("fs").types(Collections.emptyList()).build());
    }
    return imports.appImports(appImports.build()).build();
  }
}
