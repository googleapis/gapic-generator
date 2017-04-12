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

import com.google.api.codegen.ProtoFileView;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GrpcElementDocTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.GrpcDocView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ImmutableList;
import java.util.List;

public class RubyGapicSurfaceDocTransformer implements ModelToViewTransformer {
  private static final String DOC_TEMPLATE_FILENAME = "ruby/message.snip";

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer = new FileHeaderTransformer(null);
  private final GrpcElementDocTransformer elementDocTransformer = new GrpcElementDocTransformer();

  public RubyGapicSurfaceDocTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(DOC_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> surfaceDocs = ImmutableList.builder();
    for (ProtoFile file : new ProtoFileView().getElementIterable(model)) {
      surfaceDocs.add(generateDoc(file, productConfig));
    }
    return surfaceDocs.build();
  }

  private ViewModel generateDoc(ProtoFile file, GapicProductConfig productConfig) {
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new RubyTypeTable(productConfig.getPackageName()),
            new RubyModelTypeNameConverter(productConfig.getPackageName()));
    // Use file path for package name to get file-specific package instead of package for the API.
    SurfaceNamer namer = new RubySurfaceNamer(typeTable.getFullNameFor(file));
    String subPath = pathMapper.getOutputPath(file, productConfig);
    String baseFilename = namer.getProtoFileName(file);
    GrpcDocView.Builder doc = GrpcDocView.newBuilder();
    doc.templateFileName(DOC_TEMPLATE_FILENAME);
    doc.outputPath(subPath + "/doc/" + baseFilename);
    doc.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));
    doc.elementDocs(elementDocTransformer.generateElementDocs(typeTable, namer, file));
    return doc.build();
  }
}
