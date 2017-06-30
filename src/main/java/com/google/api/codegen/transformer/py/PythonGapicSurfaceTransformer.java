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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.VersionIndexRequireView;
import com.google.api.codegen.viewmodel.metadata.VersionIndexView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;

public class PythonGapicSurfaceTransformer implements ModelToViewTransformer {

  private static final String TYPES_TEMPLEATE_FILE = "py/types.snip";
  private static final String VERSIONED_INIT_TEMPLATE_FILE =
      "py/versioned_directory__init__.py.snip";

  private static final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private static final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);

  private final PackageMetadataConfig packageConfig;

  public PythonGapicSurfaceTransformer(PackageMetadataConfig packageConfig) {
    this.packageConfig = packageConfig;
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.addAll(generateVersionedDirectoryViews(model, productConfig));
    return views.build();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(TYPES_TEMPLEATE_FILE, VERSIONED_INIT_TEMPLATE_FILE);
  }

  private List<ViewModel> generateVersionedDirectoryViews(
      Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.add(generateTypesView(model, productConfig));
    views.add(generateVersionedInitView(model, productConfig));
    return views.build();
  }

  private ViewModel generateTypesView(Model model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());
    ImportSectionView imports =
        importSectionTransformer.generateTypesImportSection(model, productConfig, namer);
    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    return VersionIndexView.newBuilder()
        .templateFileName(TYPES_TEMPLEATE_FILE)
        .outputPath(typesOutputFile(model, namer))
        .requireViews(ImmutableList.<VersionIndexRequireView>of())
        .apiVersion(namer.getApiWrapperModuleVersion())
        .namespace(namer.getVersionedDirectoryNamespace(apiInterfaces.iterator().next()))
        .packageVersion(packageConfig.generatedPackageVersionBound(TargetLanguage.PYTHON).lower())
        .fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, imports, namer))
        .build();
  }

  private ViewModel generateVersionedInitView(Model model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());
    boolean packageHasEnums = false;
    for (TypeRef type : model.getSymbolTable().getDeclaredTypes()) {
      if (type.isEnum() && type.getEnumType().isReachable()) {
        packageHasEnums = true;
        break;
      }
    }
    ImportSectionView imports =
        importSectionTransformer.generateVersionedInitImportSection(
            model, productConfig, packageConfig, namer, packageHasEnums);
    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    return VersionIndexView.newBuilder()
        .templateFileName(VERSIONED_INIT_TEMPLATE_FILE)
        .outputPath(versionedInitOutputFile(model, namer))
        .requireViews(versionedInitRequireViews(model, productConfig, namer))
        .apiVersion(namer.getApiWrapperModuleVersion())
        .namespace(namer.getVersionedDirectoryNamespace(apiInterfaces.iterator().next()))
        .packageVersion(packageConfig.generatedPackageVersionBound(TargetLanguage.PYTHON).lower())
        .fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, imports, namer))
        .packageHasEnums(packageHasEnums)
        .build();
  }

  private List<VersionIndexRequireView> versionedInitRequireViews(
      Model model, GapicProductConfig productConfig, SurfaceNamer namer) {
    ImmutableList.Builder<VersionIndexRequireView> views = ImmutableList.builder();
    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    ModelTypeTable typeTable = emptyTypeTable(productConfig);
    for (Interface apiInterface : apiInterfaces) {
      views.add(
          VersionIndexRequireView.newBuilder()
              .clientName(namer.getAndSaveNicknameForGrpcClientTypeName(typeTable, apiInterface))
              .localName(
                  namer.getApiWrapperClassName(productConfig.getInterfaceConfig(apiInterface)))
              .fileName(namer.getNotImplementedString("VersionIndexRequireView.fileName"))
              .helpersClassName(namer.getHelpersClassName(packageConfig.shortName()))
              .build());
    }
    return views.build();
  }

  private ModelTypeTable emptyTypeTable(GapicProductConfig productConfig) {
    return new ModelTypeTable(
        new PythonTypeTable(productConfig.getPackageName()),
        new PythonModelTypeNameConverter(productConfig.getPackageName()));
  }

  private String typesOutputFile(Model model, SurfaceNamer namer) {
    return versionedDirectoryPath(model, namer) + File.separator + "types.py";
  }

  private String versionedInitOutputFile(Model model, SurfaceNamer namer) {
    return versionedDirectoryPath(model, namer) + File.separator + "__init__.py";
  }

  private String versionedDirectoryPath(Model model, SurfaceNamer namer) {
    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    String namespace = namer.getVersionedDirectoryNamespace(apiInterfaces.iterator().next());
    return namespace.replace(".", File.separator);
  }
}
