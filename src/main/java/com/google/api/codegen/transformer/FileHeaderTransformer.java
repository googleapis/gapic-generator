/* Copyright 2016 Google LLC
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

import com.google.api.codegen.GeneratorVersionProvider;
import com.google.api.codegen.config.ProductConfig;
import com.google.api.codegen.viewmodel.FileHeaderView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.common.collect.ImmutableList;

public class FileHeaderTransformer {

  private final ImportSectionTransformer importSectionTransformer;

  public FileHeaderTransformer(ImportSectionTransformer importSectionTransformer) {
    this.importSectionTransformer = importSectionTransformer;
  }

  public FileHeaderView generateFileHeader(TransformationContext context) {
    return generateFileHeader(
        context.getProductConfig(),
        importSectionTransformer.generateImportSection(context),
        context.getNamer());
  }

  public FileHeaderView generateFileHeader(
      ProductConfig productConfig, ImportSectionView importSectionView, SurfaceNamer namer) {
    return generateFileHeader(productConfig, importSectionView, namer, namer.getApiModules());
  }

  public FileHeaderView generateFileHeader(
      ProductConfig productConfig,
      ImportSectionView importSection,
      SurfaceNamer namer,
      ImmutableList<String> apiModules) {
    FileHeaderView.Builder fileHeader = FileHeaderView.newBuilder();

    fileHeader.copyrightLines(productConfig.getCopyrightLines());
    fileHeader.licenseLines(productConfig.getLicenseLines());
    fileHeader.packageName(namer.getPackageName());
    fileHeader.examplePackageName(namer.getExamplePackageName());
    fileHeader.localPackageName(namer.getLocalPackageName());
    fileHeader.localExamplePackageName(namer.getLocalExamplePackageName());
    fileHeader.importSection(importSection);
    fileHeader.version(namer.getApiWrapperModuleVersion());
    fileHeader.generatorVersion(GeneratorVersionProvider.getGeneratorVersion());
    fileHeader.modules(apiModules);

    return fileHeader.build();
  }
}
