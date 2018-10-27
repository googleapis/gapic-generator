/* Copyright 2018 Google LLC
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

import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.OutputTransformer;
import com.google.api.codegen.util.ImportType;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.codegen.viewmodel.PrintArgView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import java.util.Collections;
import java.util.List;

public class PythonSampleOutputImportTransformer
    implements OutputTransformer.OutputImportTransformer {

  @Override
  public ImmutableList<ImportFileView> generateOutputImports(
      MethodContext context, List<OutputView> outputViews) {
    ImmutableSet.Builder<ImportFileView> imports = ImmutableSet.builder();
    addImports(imports, context, outputViews);
    return imports.build().asList();
  }

  private static void addImports(
      ImmutableSet.Builder<ImportFileView> imports,
      MethodContext context,
      List<OutputView> outputViews) {
    for (OutputView view : outputViews) {
      addImports(imports, context, view);
    }
  }

  private static void addImports(
      ImmutableSet.Builder<ImportFileView> imports, MethodContext context, OutputView outputView) {
    if (outputView.kind() == OutputView.Kind.LOOP) {
      OutputView.LoopView loopView = (OutputView.LoopView) outputView;
      addImports(imports, context, loopView.body());
    } else if (outputView.kind() == OutputView.Kind.PRINT) {
      addEnumImports(imports, context, (OutputView.PrintView) outputView);
    }
  }

  private static void addEnumImports(
      ImmutableSet.Builder<ImportFileView> imports,
      MethodContext context,
      OutputView.PrintView view) {
    for (PrintArgView arg : view.args()) {
      for (PrintArgView.ArgSegmentView segment : arg.segments())
        if (segment instanceof PrintArgView.VariableSegmentView) {
          TypeModel type = ((PrintArgView.VariableSegmentView) segment).variable().type();
          if (type != null && type.isEnum()) {
            ImportTypeView importTypeView =
                ImportTypeView.newBuilder()
                    .fullName("enums")
                    .type(ImportType.SimpleImport)
                    .nickname("")
                    .build();
            imports.add(
                ImportFileView.newBuilder()
                    .moduleName(context.getNamer().getVersionedDirectoryNamespace())
                    .types(Collections.singletonList(importTypeView))
                    .build());
          }
        }
    }
  }
}
