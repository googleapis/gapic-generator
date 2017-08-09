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

import com.google.api.codegen.ProtoFileView;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GrpcElementDocTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.GrpcDocView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;

/** Transforms a Model into a GAPIC surface for the gRPC docs in Python. */
public class PythonGapicSurfaceDocTransformer implements ModelToViewTransformer {
  private static final String DOC_TEMPLATE_FILENAME = "py/message.snip";

  private final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer = new FileHeaderTransformer(null);
  private final GrpcElementDocTransformer elementDocTransformer = new GrpcElementDocTransformer();

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(DOC_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> surfaceDocs = ImmutableList.builder();
    Set<ProtoFile> files = Sets.newHashSet(new ProtoFileView().getElementIterable(model));
    for (ProtoFile file : files) {
      ModelTypeTable typeTable =
          new ModelTypeTable(
              new PythonTypeTable(productConfig.getPackageName()),
              new PythonModelTypeNameConverter(productConfig.getPackageName()));
      addDocImports(file, typeTable, files);
      surfaceDocs.add(generateDoc(file, productConfig, typeTable));
    }
    return surfaceDocs.build();
  }

  private void addDocImports(
      ProtoFile file, ModelTypeTable typeTable, Set<ProtoFile> importableProtoFiles) {
    for (MessageType message : file.getMessages()) {
      for (Field field : message.getMessageFields()) {
        MessageType messageField = field.getType().getMessageType();
        // Don't include imports to messages in the same file.
        ProtoFile messageParentFile = messageField.getFile();
        if (!messageParentFile.equals(file) && importableProtoFiles.contains(messageParentFile)) {
          typeTable.getAndSaveNicknameFor(TypeRef.of(messageField));
        }
      }
    }
  }

  private ViewModel generateDoc(
      ProtoFile file, GapicProductConfig productConfig, ModelTypeTable typeTable) {
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());

    GrpcDocView.Builder doc = GrpcDocView.newBuilder();
    doc.templateFileName(DOC_TEMPLATE_FILENAME);

    String filename = file.getSimpleName();
    doc.outputPath(filename.substring(0, filename.lastIndexOf('.')) + "_pb2.py");

    ImportSectionView importSection =
        importSectionTransformer.generateImportSection(typeTable.getImports());
    doc.fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, importSection, namer));

    doc.elementDocs(elementDocTransformer.generateElementDocs(typeTable, namer, file));
    return doc.build();
  }
}
