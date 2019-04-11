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

import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.OutputContext;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ImportSectionView;
import java.util.Collections;

/**
 * An implementation of SampleImportTransformer. Subclasses of this class can choose to override
 * `addSampleBodyImports`, `addOutputImports` and `addInitCodeImports` to save language-specific
 * types to the type table in `MethodContext`. Currently used by Java.
 */
public class StandardSampleImportTransformer implements SampleImportTransformer {

  private final ImportSectionTransformer importSectionTransformer;

  public StandardSampleImportTransformer(ImportSectionTransformer importSectionTransformer) {
    this.importSectionTransformer = importSectionTransformer;
  }

  /**
   * Adds all the types to import referenced in the rpc call part of a sample. By default is a
   * no-op.
   */
  protected void addSampleBodyImports(MethodContext context, CallingForm form) {
    return;
  }

  /**
   * Adds all the types to import to the type table in `context` referenced in the output handling
   * part of a sample based on `outputContext`. The output handling does something with the object
   * returned by RPC call, and is usually right above the closed region tag. By default is a no-op.
   */
  protected void addOutputImports(MethodContext context, OutputContext outputContext) {
    return;
  }

  /**
   * Adds all the types to import referenced in the initCode part of sample. The initCode is
   * responsible for setting up the rpc call request object and/or parameters, and is usually right
   * below the open region tag. By default this method copies over all types from initCodeTypeTable.
   */
  protected void addInitCodeImports(
      MethodContext context, ImportTypeTable initCodeTypeTable, Iterable<InitCodeNode> nodes) {

    initCodeTypeTable
        .getImports()
        .values()
        .forEach(t -> context.getTypeTable().getAndSaveNicknameFor(t));
  }

  /** Generates an import section from all types stored in the `importTypeTable` in `context`. */
  protected ImportSectionView generateImportSection(MethodContext context) {
    return importSectionTransformer.generateImportSection(
        context, Collections.<InitCodeNode>emptyList());
  }

  @Override
  public ImportSectionView generateImportSection(
      MethodContext context,
      CallingForm form,
      OutputContext outputContext,
      ImportTypeTable initCodeTypeTable,
      Iterable<InitCodeNode> nodes) {
    addInitCodeImports(context, initCodeTypeTable, nodes);
    addOutputImports(context, outputContext);
    addSampleBodyImports(context, form);
    return generateImportSection(context);
  }
}
