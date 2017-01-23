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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class RubyImportSectionTransformer implements ImportSectionTransformer {

  @Override
  public ImportSectionView generateImportSection(SurfaceTransformerContext context) {
    Set<String> importFilenames = generateImportFilenames(context);
    ImportSectionView.Builder importSection = ImportSectionView.newBuilder();
    importSection.standardImports(generateStandardImports());
    importSection.externalImports(generateExternalImports(context));
    importSection.appImports(generateAppImports(importFilenames));
    importSection.serviceImports(generateServiceImports(importFilenames));
    return importSection.build();
  }

  private List<ImportFileView> generateStandardImports() {
    return ImmutableList.of(createImport("json"), createImport("pathname"));
  }

  private List<ImportFileView> generateExternalImports(SurfaceTransformerContext context) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    imports.add(createImport("google/gax"));

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      imports.add(createImport("google/gax/operation"));
      imports.add(createImport("google/longrunning/operations_api"));
    }

    return imports.build();
  }

  private List<ImportFileView> generateAppImports(Set<String> filenames) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    for (String filename : filenames) {
      imports.add(createImport(filename.replace(".proto", "_pb")));
    }
    return imports.build();
  }

  private List<ImportFileView> generateServiceImports(Set<String> filenames) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    imports.add(createImport("google/gax/grpc"));
    for (String filename : filenames) {
      imports.add(createImport(filename.replace(".proto", "_services_pb")));
    }
    return imports.build();
  }

  private Set<String> generateImportFilenames(SurfaceTransformerContext context) {
    Set<String> filenames = new TreeSet<>();
    filenames.add(context.getInterface().getFile().getSimpleName());
    for (Method method : context.getSupportedMethods()) {
      Interface targetInterface = context.asRequestMethodContext(method).getTargetInterface();
      filenames.add(targetInterface.getFile().getSimpleName());
    }
    return filenames;
  }

  private ImportFileView createImport(String name) {
    return ImportFileView.newBuilder().attributeName(name).build();
  }
}
