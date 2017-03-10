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

import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.InitCodeImportSectionTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

// TODO(eoogbe): implement ImportSectionTransformer when migrating to MVVM
public class PythonImportSectionTransformer implements InitCodeImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(
      MethodTransformerContext context, Iterable<InitCodeNode> specItemNodes) {
    return ImportSectionView.newBuilder()
        .appImports(generateInitCodeAppImports(context, specItemNodes))
        .build();
  }

  private List<ImportFileView> generateInitCodeAppImports(
      MethodTransformerContext context, Iterable<InitCodeNode> specItemNodes) {
    return ImmutableList.<ImportFileView>builder()
        .add(generateApiImport(context))
        .addAll(generateProtoImports(context, specItemNodes))
        .addAll(generatePageStreamingImports(context))
        .build();
  }

  private ImportFileView generateApiImport(MethodTransformerContext context) {
    return createImport(
        context.getApiConfig().getPackageName(),
        context.getNamer().getApiWrapperVariableName(context.getInterfaceConfig()));
  }

  private List<ImportFileView> generateProtoImports(
      MethodTransformerContext context, Iterable<InitCodeNode> specItemNodes) {
    ModelTypeTable typeTable = context.getTypeTable();
    Set<ImportFileView> protoImports = new TreeSet<>(importFileViewComparator());
    for (InitCodeNode item : specItemNodes) {
      TypeRef type = item.getType();
      // Exclude map entry types
      if (!type.isRepeated() && type.isMessage()) {
        String fullName = typeTable.getFullNameFor(type);
        String nickname = typeTable.getNicknameFor(type);
        protoImports.add(generateAppImport(fullName, nickname));
      } else if (type.isEnum()) {
        protoImports.add(createImport(context.getApiConfig().getPackageName(), "enums"));
      }
    }
    return new ArrayList<>(protoImports);
  }

  private List<ImportFileView> generatePageStreamingImports(MethodTransformerContext context) {
    ImmutableList.Builder<ImportFileView> pageStreamingImports = ImmutableList.builder();
    if (context.getMethodConfig().isPageStreaming()) {
      ImportTypeView callOptionsImport =
          ImportTypeView.newBuilder().fullName("CallOptions").nickname("").build();
      ImportTypeView initialPageImport =
          ImportTypeView.newBuilder().fullName("INITIAL_PAGE").nickname("").build();
      ImportFileView fileImport =
          ImportFileView.newBuilder()
              .moduleName("google.gax")
              .types(ImmutableList.of(callOptionsImport, initialPageImport))
              .build();
      pageStreamingImports.add(fileImport);
    }
    return pageStreamingImports.build();
  }

  /**
   * Orders the imports by: (1) number of attributes (least to most), (2) module name (A-Z), (3)
   * attribute name (A-Z)
   */
  private Comparator<ImportFileView> importFileViewComparator() {
    return new Comparator<ImportFileView>() {
      @Override
      public int compare(ImportFileView o1, ImportFileView o2) {
        if (o1.types().size() != o2.types().size()) {
          return o2.types().size() - o1.types().size();
        }

        if (!o1.moduleName().equals(o2.moduleName())) {
          return o1.moduleName().compareTo(o2.moduleName());
        }

        for (int i = 0; i < o1.types().size(); ++i) {
          String attributeName1 = o1.types().get(i).fullName();
          String attributeName2 = o2.types().get(i).fullName();

          if (!attributeName1.equals(attributeName2)) {
            return attributeName1.compareTo(attributeName2);
          }
        }

        return 0;
      }
    };
  }

  /**
   * Generates an import from the fullName and the nickname of a type.
   *
   * <p>Handles the following cases: (1) Module only -- generateAppImport("foo.Bar", "foo.Bar") =>
   * import foo (2) Module and attribute -- generateAppImport("foo.bar.Baz", "bar.Baz") => from foo
   * import bar (3) Module, attribute, local -- generateAppImport("foo.bar.Baz", "qux.Baz") => from
   * foo import bar as qux
   */
  private ImportFileView generateAppImport(String fullName, String nickname) {
    int nicknameDottedClassIndex = nickname.indexOf(".");
    String localName = nickname.substring(0, nicknameDottedClassIndex);

    if (fullName.equals(nickname)) {
      return createImport(localName);
    }

    if (fullName.endsWith(nickname)) {
      String moduleName = fullName.substring(0, fullName.length() - nickname.length() - 1);
      return createImport(moduleName, localName);
    }

    int fullNameDottedClassIndex = fullName.length() - nickname.length() + nicknameDottedClassIndex;
    int dottedAttributeIndex = fullName.lastIndexOf(".", fullNameDottedClassIndex - 1);
    String moduleName = fullName.substring(0, dottedAttributeIndex);
    String attributeName = fullName.substring(dottedAttributeIndex + 1, fullNameDottedClassIndex);
    return createImport(moduleName, attributeName, localName);
  }

  private ImportFileView createImport(String moduleName) {
    return ImportFileView.newBuilder()
        .moduleName(moduleName)
        .types(ImmutableList.<ImportTypeView>of())
        .build();
  }

  private ImportFileView createImport(String moduleName, String attributeName) {
    ImportTypeView typeImport =
        ImportTypeView.newBuilder().fullName(attributeName).nickname("").build();
    return ImportFileView.newBuilder()
        .moduleName(moduleName)
        .types(ImmutableList.of(typeImport))
        .build();
  }

  private ImportFileView createImport(String moduleName, String attributeName, String localName) {
    ImportTypeView typeImport =
        ImportTypeView.newBuilder().fullName(attributeName).nickname(localName).build();
    return ImportFileView.newBuilder()
        .moduleName(moduleName)
        .types(ImmutableList.of(typeImport))
        .build();
  }
}
