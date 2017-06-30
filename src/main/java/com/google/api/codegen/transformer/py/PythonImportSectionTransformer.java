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
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class PythonImportSectionTransformer implements ImportSectionTransformer {
  @Override
  public ImportSectionView generateImportSection(InterfaceContext context) {
    // TODO(eoogbe): implement when migrating to MVVM
    return ImportSectionView.newBuilder().build();
  }

  @Override
  public ImportSectionView generateImportSection(
      GapicMethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return ImportSectionView.newBuilder()
        .appImports(generateInitCodeAppImports(context, specItemNodes))
        .build();
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

  public ImportSectionView generateTypesImportSection(
      Model model, GapicProductConfig productConfig, SurfaceNamer namer) {
    return ImportSectionView.newBuilder()
        .appImports(generateTypesProtoImports(model, productConfig, namer))
        .externalImports(generateTypesExternalImports())
        .standardImports(generateTypesStandardImports())
        .build();
  }

  public ImportSectionView generateVersionedInitImportSection(
      Model model,
      GapicProductConfig productConfig,
      PackageMetadataConfig packageConfig,
      SurfaceNamer namer,
      boolean packageHasEnums) {
    return ImportSectionView.newBuilder()
        .appImports(
            generateVersionedInitAppImports(
                model, productConfig, packageConfig, namer, packageHasEnums))
        .standardImports(generateVersionedInitStandardImports())
        .build();
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
    for (Map.Entry<String, TypeAlias> entry : context.getModelTypeTable().getImports().entrySet()) {
      String fullName = entry.getKey();
      if (fullName.startsWith(context.getNamer().getTestPackageName() + ".enums")) {
        appImports.add(createImport(context.getProductConfig().getPackageName(), "enums"));
      } else {
        appImports.add(generateAppImport(entry.getKey(), entry.getValue().getNickname()));
      }
    }
    return new ArrayList<>(appImports);
  }

  private List<ImportFileView> generateTypesProtoImports(
      Model model, GapicProductConfig productConfig, SurfaceNamer namer) {
    ModelTypeTable typeTable = emptyTypeTable(productConfig);
    Set<ImportFileView> imports = new TreeSet<>(importFileViewComparator());

    // Save proto file import names to the type table for disambiguation.
    List<ProtoFile> protoFileDependencies = model.getFiles();
    populateTypeTable(protoFileDependencies, typeTable);

    // Get disambiguated imports.
    for (Map.Entry<String, TypeAlias> entry : typeTable.getImports().entrySet()) {
      imports.add(generateAppImport(entry.getKey(), entry.getValue().getNickname()));
    }
    return ImmutableList.<ImportFileView>builder().addAll(imports).build();
  }

  private ModelTypeTable emptyTypeTable(GapicProductConfig productConfig) {
    return new ModelTypeTable(
        new PythonTypeTable(productConfig.getPackageName()),
        new PythonModelTypeNameConverter(productConfig.getPackageName()));
  }

  private List<ImportFileView> generateTypesExternalImports() {
    return ImmutableList.of(createImport("google.gax.utils.messages", "get_messages"));
  }

  private List<ImportFileView> generateTypesStandardImports() {
    return ImmutableList.of(createImport("__future__", "absoulute_import"), createImport("sys"));
  }

  private void populateTypeTable(List<ProtoFile> protoFileDependencies, ModelTypeTable typeTable) {
    for (ProtoFile protoFile : protoFileDependencies) {
      // For python, adding a single message from the proto file to the type table will populate
      // the type table with the correct imports.
      ImmutableList<MessageType> messages = protoFile.getMessages();
      if (!messages.isEmpty()) {
        typeTable.getAndSaveNicknameFor(TypeRef.of(messages.get(0)));
      }
    }
  }

  private List<ImportFileView> generateVersionedInitStandardImports() {
    return ImmutableList.of(createImport("__future__", "absoulte_import"));
  }

  private List<ImportFileView> generateVersionedInitAppImports(
      Model model,
      GapicProductConfig productConfig,
      PackageMetadataConfig packageConfig,
      SurfaceNamer namer,
      boolean packageHasEnums) {

    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    ModelTypeTable typeTable = emptyTypeTable(productConfig);
    for (Interface apiInterface : apiInterfaces) {
      namer.getAndSaveNicknameForGrpcClientTypeName(typeTable, apiInterface);
    }

    Set<ImportFileView> imports = new TreeSet<>(importFileViewComparator());
    if (packageHasEnums) {
      imports.add(createImport(productConfig.getPackageName(), "enums"));
    }
    String packageNamespace = namer.getPackageNamespace(packageConfig.apiVersion());
    imports.add(
        createImport(
            packageNamespace + ".helpers", namer.getHelpersClassName(packageConfig.shortName())));
    imports.add(
        createImport(
            namer.getVersionedDirectoryNamespace(apiInterfaces.iterator().next()), "types"));
    for (Map.Entry<String, TypeAlias> entry : typeTable.getImports().entrySet()) {
      imports.add(generateAppImport(entry.getKey(), entry.getValue().getNickname()));
    }
    return ImmutableList.<ImportFileView>builder().addAll(imports).build();
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
