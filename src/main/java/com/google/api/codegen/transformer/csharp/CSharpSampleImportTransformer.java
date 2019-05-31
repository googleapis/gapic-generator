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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StandardSampleImportTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.common.collect.Streams;

public class CSharpSampleImportTransformer extends StandardSampleImportTransformer {

  public CSharpSampleImportTransformer() {
    super(new StandardImportSectionTransformer());
  }

  @Override
  protected void addSampleBodyImports(MethodContext context, CallingForm form) {
    ImportTypeTable typeTable = context.getTypeTable();
    MethodModel method = context.getMethodModel();
    SurfaceNamer namer = context.getNamer();
    if (namer.usesAsyncAwaitPattern(form)) {
      typeTable.saveNicknameFor("System.Threading.Tasks.Task");
    }
    switch (form) {
      case RequestPaged:
      case FlattenedPaged:
      case RequestPagedAll:
      case FlattenedPagedAll:
        typeTable.saveNicknameFor("Google.Api.Gax.PagedEnumerable");
        break;
      case RequestAsyncPaged:
      case FlattenedAsyncPaged:
      case RequestAsyncPagedAll:
      case FlattenedAsyncPagedAll:
        typeTable.saveNicknameFor("Google.Api.Gax.PagedAsyncEnumerable");
        break;
      case RequestPagedPageSize:
      case FlattenedPagedPageSize:
        typeTable.saveNicknameFor("Google.Api.Gax.Page");
        break;
    }
  }

  @Override
  protected void addInitCodeImports(
      MethodContext context, ImportTypeTable initCodeTypeTable, Iterable<InitCodeNode> nodes) {
    ImportTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor(
        context.getNamer().getFullyQualifiedApiWrapperClassName(context.getInterfaceConfig()));
    Streams.stream(nodes).map(InitCodeNode::getType).forEach(typeTable::getAndSaveNicknameFor);
    if (Streams.stream(nodes).anyMatch(n -> n.getLineType() == InitCodeLineType.ReadFileInitLine)) {
      typeTable.saveNicknameFor("System.IO.File");
    }
    if (Streams.stream(nodes).anyMatch(n -> n.getLineType() == InitCodeLineType.ListInitLine)) {
      typeTable.saveNicknameFor("System.Collections.Generic.IEnumerable");
    }
    if (Streams.stream(nodes).anyMatch(n -> n.getLineType() == InitCodeLineType.MapInitLine)) {
      typeTable.saveNicknameFor("System.Collections.Generic.IDictionary");
    }
  }
}
