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
package com.google.api.codegen.transformer.php;

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

public class PhpImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(TransformationContext context) {
    return generateImportSection(context.getImportTypeTable().getImports());
  }

  @Override
  public ImportSectionView generateImportSection(
      MethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return generateImportSection(context.getTypeTable().getImports());
  }

  /** Package-private for use in PhpGapicSurfaceTestTransformer */
  ImportSectionView generateImportSection(Map<String, TypeAlias> typeImports) {
    ImmutableList.Builder<ImportFileView> appImports = ImmutableList.builder();
    for (Map.Entry<String, TypeAlias> entry : typeImports.entrySet()) {
      String key = entry.getKey();
      TypeAlias alias = entry.getValue();
      // Remove leading backslash because it is not required by PHP use statements
      String fullName = key.startsWith("\\") ? key.substring(1) : key;
      ImportTypeView.Builder imp = ImportTypeView.newBuilder();
      imp.fullName(fullName);
      imp.nickname(alias.getNickname());
      imp.type(alias.getImportType());
      appImports.add(ImportFileView.newBuilder().types(ImmutableList.of(imp.build())).build());
    }
    return ImportSectionView.newBuilder().appImports(appImports.build()).build();
  }
}
