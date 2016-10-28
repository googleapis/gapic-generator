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
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class StandardImportTypeTransformer implements ImportTypeTransformer {
  private enum ImportFileType {
    SERVICE_FILE,
    PROTO_FILE
  }

  @Override
  public List<ImportTypeView> generateImports(Map<String, TypeAlias> imports) {
    List<ImportTypeView> generatedImports = new ArrayList<>();
    for (String key : imports.keySet()) {
      TypeAlias value = imports.get(key);
      generatedImports.add(
          ImportTypeView.newBuilder()
              .fullName(key)
              .nickname(value.getNickname())
              .type(value.getImportType())
              .build());
    }
    return generatedImports;
  }

  public List<ImportTypeView> generateServiceFileImports(SurfaceTransformerContext context) {
    return generateFileImports(context, ImportFileType.SERVICE_FILE);
  }

  public List<ImportTypeView> generateProtoFileImports(SurfaceTransformerContext context) {
    return generateFileImports(context, ImportFileType.PROTO_FILE);
  }

  private List<ImportTypeView> generateFileImports(
      SurfaceTransformerContext context, ImportFileType importFileType) {
    SurfaceNamer namer = context.getNamer();
    Set<String> fullNames = new TreeSet<>();

    fullNames.add(getFileImport(context.getInterface(), namer, importFileType));

    for (Method method : context.getSupportedMethods()) {
      Interface targetInterface = context.asRequestMethodContext(method).getTargetInterface();
      fullNames.add(getFileImport(targetInterface, namer, importFileType));
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

  private String getFileImport(
      Interface service, SurfaceNamer namer, ImportFileType importFileType) {
    return importFileType == ImportFileType.SERVICE_FILE
        ? namer.getServiceFileImportFromService(service)
        : namer.getProtoFileImportFromService(service);
  }
}
