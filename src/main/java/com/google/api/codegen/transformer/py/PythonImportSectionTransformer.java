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

import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.TransformationContext;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PythonImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(TransformationContext context) {
    InterfaceContext interfaceContext = (InterfaceContext) context;
    return ImportSectionView.newBuilder()
        .standardImports(generateFileHeaderStandardImports())
        .externalImports(generateFileHeaderExternalImports(interfaceContext))
        .appImports(generateFileHeaderAppImports(context.getImportTypeTable().getImports()))
        .build();
  }

  @Override
  public ImportSectionView generateImportSection(
      MethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return ImportSectionView.newBuilder()
        .appImports(generateInitCodeAppImports(((GapicMethodContext) context), specItemNodes))
        .build();
  }

  public ImportSectionView generateImportSection(Map<String, TypeAlias> typeImports) {
    ImportSectionView.Builder importSection = ImportSectionView.newBuilder();
    importSection.appImports(generateFileHeaderAppImports(typeImports));
    return importSection.build();
  }

  public ImportSectionView generateTestImportSection(GapicInterfaceContext context) {
    return ImportSectionView.newBuilder()
        .standardImports(generateTestStandardImports())
        .externalImports(generateTestExternalImports(context))
        .appImports(generateTestAppImports(context))
        .build();
  }

  public ImportSectionView generateSmokeTestImportSection(
      GapicInterfaceContext context, boolean requireProjectId) {
    return ImportSectionView.newBuilder()
        .standardImports(generateSmokeTestStandardImports(requireProjectId))
        .externalImports(ImmutableList.<ImportFileView>of())
        .appImports(generateTestAppImports(context))
        .build();
  }

  private List<ImportFileView> generateFileHeaderStandardImports() {
    return ImmutableList.of(
        createImport("collections"),
        createImport("json"),
        createImport("os"),
        createImport("pkg_resources"),
        createImport("platform"));
  }

  private List<ImportFileView> generateFileHeaderExternalImports(InterfaceContext context) {
    List<ImportFileView> imports = new ArrayList<>();
    imports.add(createImport("google.gax"));
    imports.add(createImport("google.gax", "api_callable"));
    imports.add(createImport("google.gax", "config"));
    imports.add(createImport("google.gax", "path_template"));

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      imports.add(createImport("google.gapic.longrunning", "operations_client"));
    }

    for (MethodConfig methodConfig : context.getInterfaceConfig().getMethodConfigs()) {
      // Add the import for gax.utils.oneof if and only if there is at
      // least one "one of" argument set.
      if (!Iterables.isEmpty(methodConfig.getOneofNames(context.getNamer()))) {
        imports.add(createImport("google.gax.utils", "oneof"));
        break;
      }
    }

    Collections.sort(imports, importFileViewComparator());
    return imports;
  }

  private List<ImportFileView> generateFileHeaderAppImports(Map<String, TypeAlias> typeImports) {
    List<ImportFileView> appImports = new ArrayList<>();
    for (Map.Entry<String, TypeAlias> entry : typeImports.entrySet()) {
      appImports.add(generateAppImport(entry.getKey(), entry.getValue().getNickname()));
    }
    Collections.sort(appImports, importFileViewComparator());
    return appImports;
  }

  private List<ImportFileView> generateInitCodeAppImports(
      GapicMethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return ImmutableList.<ImportFileView>builder()
        .add(generateApiImport(context))
        .addAll(generateProtoImports(context, specItemNodes))
        .addAll(generatePageStreamingImports(context))
        .build();
  }

  private ImportFileView generateApiImport(GapicMethodContext context) {
    String moduleName = context.getProductConfig().getPackageName();
    String attributeName =
        context.getNamer().getApiWrapperVariableName(context.getInterfaceConfig());
    return createImport(moduleName, attributeName);
  }

  private List<ImportFileView> generateProtoImports(
      GapicMethodContext context, Iterable<InitCodeNode> specItemNodes) {
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
        protoImports.add(createImport(context.getProductConfig().getPackageName(), "enums"));
      }
    }
    return new ArrayList<>(protoImports);
  }

  private List<ImportFileView> generatePageStreamingImports(GapicMethodContext context) {
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

  private List<ImportFileView> generateTestStandardImports() {
    return ImmutableList.of(createImport("mock"), createImport("unittest"));
  }

  private List<ImportFileView> generateSmokeTestStandardImports(boolean requireProjectId) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    if (requireProjectId) {
      imports.add(createImport("os"));
    }
    imports.add(createImport("time"), createImport("unittest"));
    return imports.build();
  }

  private List<ImportFileView> generateTestExternalImports(GapicInterfaceContext context) {
    ImmutableList.Builder<ImportFileView> externalImports = ImmutableList.builder();
    externalImports.add(createImport("google.gax", "errors"));
    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      externalImports.add(createImport("google.rpc", "status_pb2"));
    }
    return externalImports.build();
  }

  private List<ImportFileView> generateTestAppImports(GapicInterfaceContext context) {
    Set<ImportFileView> appImports = new TreeSet<>(importFileViewComparator());
    for (Map.Entry<String, TypeAlias> entry :
        context.getImportTypeTable().getImports().entrySet()) {
      String fullName = entry.getKey();
      if (fullName.startsWith(context.getNamer().getTestPackageName() + ".enums")) {
        appImports.add(createImport(context.getProductConfig().getPackageName(), "enums"));
      } else {
        appImports.add(generateAppImport(entry.getKey(), entry.getValue().getNickname()));
      }
    }
    return new ArrayList<>(appImports);
  }

  /**
   * Orders the imports by:
   *
   * <ol>
   *   <li>number of attributes (least to most)
   *   <li>module name (A-Z)
   *   <li>attribute name (A-Z)
   * </ol>
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
   * <p>Handles the following cases:
   *
   * <ul>
   *   <li>Module only -- generateAppImport("foo.Bar", "foo.Bar") => import foo
   *   <li>Module and attribute -- generateAppImport("foo.bar.Baz", "bar.Baz") => from foo import
   *       bar
   *   <li>Module, attribute, local -- generateAppImport("foo.bar.Baz", "qux.Baz") => from * foo
   *       import bar as qux
   * </ul>
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
