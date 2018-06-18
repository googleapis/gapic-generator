/* Copyright 2017 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.ProtoFiles;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GrpcElementDocTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.js.JSCommentReformatter;
import com.google.api.codegen.util.js.JSTypeTable;
import com.google.api.codegen.viewmodel.GrpcDocView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import java.util.List;

/* Transforms a ProtoApiModel into the documentation stubs of a GAPIC library for NodeJS. */
public class NodeJSGapicSurfaceDocTransformer implements ModelToViewTransformer<ProtoApiModel> {
  private static final String DOC_TEMPLATE_FILENAME = "nodejs/message.snip";

  private final FileHeaderTransformer fileHeaderTransformer = new FileHeaderTransformer(null);
  private final GrpcElementDocTransformer grpcElementDocTransformer =
      new GrpcElementDocTransformer();

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(DOC_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel apiModel, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> surfaceDocs = ImmutableList.builder();
    for (ProtoFile file : ProtoFiles.getProtoFiles(productConfig)) {
      surfaceDocs.add(generateDoc(file, productConfig));
    }
    return surfaceDocs.build();
  }

  private ViewModel generateDoc(ProtoFile file, GapicProductConfig productConfig) {
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new JSTypeTable(productConfig.getPackageName()),
            new NodeJSModelTypeNameConverter(productConfig.getPackageName()));
    // Use file path for package name to get file-specific package instead of package for the API.
    SurfaceNamer namer =
        new NodeJSSurfaceNamer(productConfig.getPackageName(), NodeJSUtils.isGcloud(productConfig));
    JSCommentReformatter commentReformatter = new JSCommentReformatter();

    GrpcDocView.Builder doc = GrpcDocView.newBuilder();
    doc.templateFileName(DOC_TEMPLATE_FILENAME);
    doc.outputPath(getOutputPath(namer, file));
    doc.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));
    doc.elementDocs(grpcElementDocTransformer.generateElementDocs(typeTable, namer, file));
    return doc.build();
  }

  private String getOutputPath(SurfaceNamer namer, ProtoFile file) {
    String version = namer.getApiWrapperModuleVersion();
    boolean hasVersion = version != null && !version.isEmpty();
    String path = hasVersion ? "src/" + version + "/doc/" : "src/doc/";
    String packageDirPath = file.getFullName().replace('.', '/');
    if (!Strings.isNullOrEmpty(packageDirPath)) {
      packageDirPath += "/";
    } else {
      packageDirPath = "";
    }
    return path + packageDirPath + "doc_" + namer.getProtoFileName(file.getSimpleName());
  }
}
