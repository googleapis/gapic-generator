/* Copyright 2016 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportTypeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GoImportTransformer implements ImportTypeTransformer {
  /**
   * Similar to StandardImportTypeTransformer.generateImports, but specific to Go since Go imports
   * differently than other languages.
   *
   * <p>Specifically, {@code fullName} and {@code nickname} of each {@code ImportTypeView} is not
   * the names of a type, but the names of an imported package.
   */
  @Override
  public List<ImportTypeView> generateImports(Map<String, TypeAlias> imports) {
    List<ImportTypeView> standard = new ArrayList<>(imports.size());
    List<ImportTypeView> thirdParty = new ArrayList<>(imports.size());

    for (Map.Entry<String, TypeAlias> imp : imports.entrySet()) {
      String importPath = imp.getKey();
      String packageRename = imp.getValue().getNickname();
      List<ImportTypeView> target = isStandardImport(importPath) ? standard : thirdParty;
      target.add(
          ImportTypeView.newBuilder()
              .fullName('"' + importPath + '"')
              .nickname(packageRename)
              .build());
    }

    List<ImportTypeView> merge = new ArrayList<>(standard);
    if (!standard.isEmpty() && !thirdParty.isEmpty()) {
      merge.add(ImportTypeView.newBuilder().fullName("").nickname("").build());
    }
    merge.addAll(thirdParty);
    return merge;
  }

  private static boolean isStandardImport(String importPath) {
    // TODO(pongad): Some packages in standard library have slashes,
    // we might have to special case them.
    if (importPath.equals("net/http")) {
      return true;
    }
    return !importPath.contains("/");
  }
}
