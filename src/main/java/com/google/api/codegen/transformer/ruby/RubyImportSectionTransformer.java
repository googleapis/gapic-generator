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

import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.ruby.RubyUtil;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.Interface;
import com.google.common.collect.ImmutableList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class RubyImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(TransformationContext transformationContext) {
    // TODO support non-Gapic inputs
    GapicInterfaceContext context = (GapicInterfaceContext) transformationContext;
    Set<String> importFilenames = generateImportFilenames(context);
    ImportSectionView.Builder importSection = ImportSectionView.newBuilder();
    importSection.standardImports(generateStandardImports());
    importSection.externalImports(generateExternalImports(context));
    importSection.appImports(generateAppImports(context, importFilenames));
    importSection.serviceImports(generateServiceImports(context, importFilenames));
    return importSection.build();
  }

  @Override
  public ImportSectionView generateImportSection(
      MethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return new StandardImportSectionTransformer().generateImportSection(context, specItemNodes);
  }

  public ImportSectionView generateTestImportSection(GapicInterfaceContext context) {
    List<ImportFileView> none = ImmutableList.of();
    ImportSectionView.Builder importSection = ImportSectionView.newBuilder();
    importSection.standardImports(generateTestStandardImports());
    importSection.externalImports(generateTestExternalImports(context));
    importSection.appImports(generateTestAppImports(context));
    importSection.serviceImports(none);
    return importSection.build();
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
    if (!RubyUtil.isLongrunning(context.getProductConfig().getPackageName())) {
      imports.add(createImport(context.getNamer().getCredentialsClassImportName()));
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
    for (MethodModel method : context.getSupportedMethods()) {
      Interface targetInterface =
          context.asRequestMethodContext(method).getTargetInterface().getInterface();
      filenames.add(targetInterface.getFile().getSimpleName());
    }
    return filenames;
  }

  private List<ImportFileView> generateTestStandardImports() {
    return ImmutableList.of(createImport("minitest/autorun"), createImport("minitest/spec"));
  }

  private List<ImportFileView> generateTestExternalImports(GapicInterfaceContext context) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    if (RubyUtil.isLongrunning(context.getNamer().getPackageName())) {
      imports.add(createImport("googleauth"));
    }
    imports.add(createImport("google/gax"));
    return imports.build();
  }

  private List<ImportFileView> generateTestAppImports(GapicInterfaceContext context) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    SurfaceNamer namer = context.getNamer();
    if (RubyUtil.hasMajorVersion(context.getProductConfig().getPackageName())) {
      imports.add(createImport(namer.getTopLevelIndexFileImportName()));
    } else {
      imports.add(createImport(namer.getVersionIndexFileImportName()));
    }
    // Import the client class directly so the client class is in scope for the static class methods
    // used in the in the init code such as the path methods. This is not necessary for method
    // samples since the client is initialized before the init code, and the initialization
    // requires the client class file.
    imports.add(createImport(namer.getServiceFileName(context.getInterfaceConfig())));
    for (String filename : generateImportFilenames(context)) {
      imports.add(createImport(namer.getServiceFileImportName(filename)));
    }
    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      imports.add(createImport("google/longrunning/operations_pb"));
    }
    return imports.build();
  }

  private ImportFileView createImport(String name) {
    ImportFileView.Builder fileImport = ImportFileView.newBuilder();
    fileImport.moduleName(name);
    fileImport.types(ImmutableList.<ImportTypeView>of());
    return fileImport.build();
  }

  public ImportSectionView generateSmokeTestImportSection(GapicInterfaceContext context) {
    List<ImportFileView> none = ImmutableList.of();
    ImportSectionView.Builder importSection = ImportSectionView.newBuilder();
    importSection.standardImports(generateTestStandardImports());
    importSection.externalImports(none);
    importSection.appImports(generateSmokeTestAppImports(context));
    importSection.serviceImports(none);
    return importSection.build();
  }

  private List<ImportFileView> generateSmokeTestAppImports(GapicInterfaceContext context) {
    return ImmutableList.of(createImport(context.getNamer().getTopLevelIndexFileImportName()));
  }
}
