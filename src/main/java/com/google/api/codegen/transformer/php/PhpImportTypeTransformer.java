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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportTypeView;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PhpImportTypeTransformer implements ImportTypeTransformer {
  @Override
  public List<ImportTypeView> generateImports(Map<String, TypeAlias> imports) {
    List<ImportTypeView> generatedImports = new ArrayList<>();
    for (String key : imports.keySet()) {
      // Remove leading backslash because it is not required by PHP use statements
      String fullName = key.startsWith("\\") ? key.substring(1) : key;
      TypeAlias value = imports.get(key);
      generatedImports.add(
          ImportTypeView.newBuilder()
              .fullName(fullName)
              .nickname(value.getNickname())
              .type(value.getImportType())
              .build());
    }
    return generatedImports;
  }
}
