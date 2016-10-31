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

import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.FileHeaderView;
import java.util.Map;

public class FileHeaderTransformer {

  private final ImportTypeTransformer importTypeTransformer;

  public FileHeaderTransformer(ImportTypeTransformer importTypeTransformer) {
    this.importTypeTransformer = importTypeTransformer;
  }

  public FileHeaderView generateFileHeader(SurfaceTransformerContext context) {
    return generateFileHeader(
        context.getApiConfig(), context.getTypeTable().getImports(), context.getNamer());
  }

  public FileHeaderView generateFileHeader(
      ApiConfig apiConfig, Map<String, TypeAlias> imports, SurfaceNamer namer) {
    FileHeaderView.Builder fileHeader = FileHeaderView.newBuilder();

    fileHeader.copyrightLines(apiConfig.getCopyrightLines());
    fileHeader.licenseLines(apiConfig.getLicenseLines());
    fileHeader.packageName(namer.getPackageName());
    fileHeader.examplePackageName(namer.getExamplePackageName());
    fileHeader.localPackageName(namer.getLocalPackageName());
    fileHeader.localExamplePackageName(namer.getLocalExamplePackageName());
    fileHeader.imports(importTypeTransformer.generateImports(imports));

    return fileHeader.build();
  }
}
