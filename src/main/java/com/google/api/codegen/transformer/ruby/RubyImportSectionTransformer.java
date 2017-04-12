/* Copyright 2017 Google Inc
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

import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class RubyImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(InterfaceContext context) {
    // TODO support non-Gapic inputs
    GapicInterfaceContext gapicContext = (GapicInterfaceContext) context;
    Set<String> importFilenames = generateImportFilenames(gapicContext);
    ImportSectionView.Builder importSection = ImportSectionView.newBuilder();
    importSection.standardImports(generateStandardImports());
    importSection.externalImports(generateExternalImports(gapicContext));
    importSection.appImports(generateAppImports(gapicContext, importFilenames));
    importSection.serviceImports(generateServiceImports(gapicContext, importFilenames));
    return importSection.build();
  }

  @Override
  public ImportSectionView generateImportSection(
      GapicMethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return new StandardImportSectionTransformer().generateImportSection(context, specItemNodes);
  }

  private List<ImportFileView> generateStandardImports() {
    return ImmutableList.of(createImport("json"), createImport("pathname"));
  }

  private List<ImportFileView> generateExternalImports(GapicInterfaceContext context) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    imports.add(createImport("google/gax"));

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      imports.add(createImport("google/gax/operation"));
      imports.add(createImport("google/longrunning/operations_client"));
    }

    return imports.build();
  }

  private List<ImportFileView> generateAppImports(
      GapicInterfaceContext context, Set<String> filenames) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    for (String filename : filenames) {
      imports.add(createImport(context.getNamer().getProtoFileImportName(filename)));
    }
    return imports.build();
  }

  private List<ImportFileView> generateServiceImports(
      GapicInterfaceContext context, Set<String> filenames) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    imports.add(createImport("google/gax/grpc"));
    for (String filename : filenames) {
      imports.add(createImport(context.getNamer().getServiceFileImportName(filename)));
    }
    return imports.build();
  }

  private Set<String> generateImportFilenames(GapicInterfaceContext context) {
    Set<String> filenames = new TreeSet<>();
    filenames.add(context.getInterface().getFile().getSimpleName());
    for (Method method : context.getSupportedMethods()) {
      Interface targetInterface = context.asRequestMethodContext(method).getTargetInterface();
      filenames.add(targetInterface.getFile().getSimpleName());
    }
    return filenames;
  }

  private ImportFileView createImport(String name) {
    ImportFileView.Builder fileImport = ImportFileView.newBuilder();
    fileImport.moduleName(name);
    fileImport.types(ImmutableList.<ImportTypeView>of());
    return fileImport.build();
  }
}
