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
package com.google.api.codegen.transformer;

import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportType;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class ImportTypeTransformer {

  public List<ImportTypeView> generateImports(Map<String, TypeAlias> imports) {
    List<ImportTypeView> generatedImports = new ArrayList<>();
    for (String key : imports.keySet()) {
      ImportTypeView.Builder importTypeView =
          ImportTypeView.newBuilder().fullName(key).nickname(imports.get(key).getNickname());
      if (imports.get(key).getParentFullName() != null) {
        importTypeView.type(ImportType.StaticImport);
      }
      generatedImports.add(importTypeView.build());
    }
    return generatedImports;
  }

  public List<ImportTypeView> generateProtoFileImports(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    Set<String> fullNames = new TreeSet<>();

    fullNames.add(namer.getProtoFileImportFromService(context.getInterface()));

    for (Method method : context.getSupportedMethods()) {
      Interface targetInterface = context.asMethodContext(method).getTargetInterface();
      fullNames.add(namer.getProtoFileImportFromService(targetInterface));
    }

    List<ImportTypeView> imports = new ArrayList<>();
    for (String name : fullNames) {
      ImportTypeView.Builder builder = ImportTypeView.newBuilder();
      builder.fullName(name);
      builder.nickname("");
      imports.add(builder.build());
    }

    return imports;
  }
}
