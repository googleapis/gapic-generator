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
package com.google.api.codegen.transformer;

import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.util.ImportType;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.common.collect.ImmutableList;
import java.util.Map;

public class StandardImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(TransformationContext context, String className) {
    return generateImportSection(context.getImportTypeTable().getImports(), className);
  }

  @Override
  public ImportSectionView generateImportSection(
      MethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return generateImportSection(context.getTypeTable().getImports(), null);
  }

  public ImportSectionView generateImportSection(
      Map<String, TypeAlias> typeImports, String className) {
    ImmutableList.Builder<ImportFileView> appImports = ImmutableList.builder();
    for (TypeAlias alias : typeImports.values()) {
      if (excludeAppImport(alias, className)) {
        continue;
      }
      ImportTypeView.Builder imp = ImportTypeView.newBuilder();
      imp.fullName(alias.getFullName());
      imp.nickname(alias.getNickname());
      imp.type(alias.getImportType());

      appImports.add(ImportFileView.newBuilder().types(ImmutableList.of(imp.build())).build());
    }
    return ImportSectionView.newBuilder().appImports(appImports.build()).build();
  }

  private boolean excludeAppImport(TypeAlias alias, String className) {
    String parentName = alias.getParentFullName();
    if (className == null
        || alias.getImportType() != ImportType.StaticImport
        || parentName == null
        || !parentName.endsWith(className)) {
      return false;
    }

    if (parentName.length() > className.length()) {
      char packageSeparator = parentName.charAt(parentName.length() - className.length() - 1);
      if (Character.isLetterOrDigit(packageSeparator)) {
        return false;
      }
    }
    return true;
  }
}
