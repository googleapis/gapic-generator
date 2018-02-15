/* Copyright 2017 Google LLC
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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.common.collect.ImmutableList;
import java.util.Map;

public class GoImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(TransformationContext context, String className) {
    return generateImportSection(context.getImportTypeTable().getImports());
  }

  @Override
  public ImportSectionView generateImportSection(
      MethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return generateImportSection(context.getTypeTable().getImports());
  }

  public ImportSectionView generateImportSection(Map<String, TypeAlias> typeImports) {
    ImmutableList.Builder<ImportFileView> standardImports = ImmutableList.builder();
    ImmutableList.Builder<ImportFileView> externalImports = ImmutableList.builder();
    for (TypeAlias alias : typeImports.values()) {
      String importPath = alias.getFullName();
      ImportTypeView.Builder imp = ImportTypeView.newBuilder();
      imp.fullName('"' + importPath + '"');
      imp.nickname(alias.getNickname());
      imp.type(alias.getImportType());
      ImmutableList.Builder<ImportFileView> target =
          isStandardImport(importPath) ? standardImports : externalImports;
      target.add(ImportFileView.newBuilder().types(ImmutableList.of(imp.build())).build());
    }
    return ImportSectionView.newBuilder()
        .standardImports(standardImports.build())
        .externalImports(externalImports.build())
        .build();
  }

  private boolean isStandardImport(String importPath) {
    // TODO(pongad): Some packages in standard library have slashes,
    // we might have to special case them.
    if (importPath.equals("net/http")) {
      return true;
    }
    return !importPath.contains("/");
  }
}
