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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.config.*;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.transformer.*;
import com.google.api.codegen.util.TypeAlias;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.SymbolTable;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/* Creates the import sections of GAPIC generated code for Python. */
public class PythonImportSectionTransformer implements ImportSectionTransformer {

  @Override
  public ImportSectionView generateImportSection(TransformationContext context, String className) {
    InterfaceContext interfaceContext = (InterfaceContext) context;
    return ImportSectionView.newBuilder()
        .standardImports(generateFileHeaderStandardImports(interfaceContext))
        .externalImports(generateFileHeaderExternalImports(interfaceContext))
        .appImports(generateMainAppImports(interfaceContext))
        .build();
  }

  @Override
  public ImportSectionView generateImportSection(
      MethodContext context, Iterable<InitCodeNode> specItemNodes) {
    return ImportSectionView.newBuilder()
        .appImports(generateInitCodeAppImports(((GapicMethodContext) context), specItemNodes))
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
        .externalImports(ImmutableList.of())
        .appImports(generateTestAppImports(context))
        .build();
  }

  public ImportSectionView generateGrpcTransportImportSection(GapicInterfaceContext context) {
    return ImportSectionView.newBuilder()
        .standardImports(ImmutableList.of())
        .externalImports(generateGrpctransportExternalImports(context))
        .appImports(generateGrpcTransportAppImports(context))
        .build();
  }

  private List<ImportFileView> generateGrpctransportExternalImports(GapicInterfaceContext context) {
    ImmutableList.Builder<ImportFileView> externalImports = ImmutableList.builder();
    externalImports.add(createImport("google.api_core.grpc_helpers"));

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      externalImports.add(createImport("google.api_core.operations_v1"));
    }
    return externalImports.build();
  }

  private List<ImportFileView> generateGrpcTransportAppImports(GapicInterfaceContext context) {
    Map<String, InterfaceModel> interfaces = new TreeMap<>();
    for (MethodModel method : context.getSupportedMethods()) {
      InterfaceModel targetInterface = context.asRequestMethodContext(method).getTargetInterface();
      interfaces.put(targetInterface.getFullName(), targetInterface);
    }
    ModelTypeTable typeTable = context.getImportTypeTable().cloneEmpty();
    for (InterfaceModel targetInterface : interfaces.values()) {
      context.getNamer().getAndSaveNicknameForGrpcClientTypeName(typeTable, targetInterface);
    }
    return generateFileHeaderAppImports(typeTable.getImports());
  }

  private List<ImportFileView> generateFileHeaderStandardImports(InterfaceContext context) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    if (context.getInterfaceConfig().hasPageStreamingMethods()) {
      imports.add(createImport("functools"));
    }
    imports.add(createImport("pkg_resources"));
    imports.add(createImport("warnings"));
    return imports.build();
  }

  private List<ImportFileView> generateFileHeaderExternalImports(InterfaceContext context) {
    List<ImportFileView> imports = new ArrayList<>();
    imports.add(createImport("grpc"));
    imports.add(createImport("google.oauth2", "service_account"));
    imports.add(createImport("google.api_core.grpc_helpers"));
    imports.add(createImport("google.api_core.gapic_v1.client_info"));
    imports.add(createImport("google.api_core.gapic_v1.config"));
    imports.add(createImport("google.api_core.gapic_v1.method"));

    if (hasRequestHeaderParams((GapicInterfaceContext) context)) {
      imports.add(createImport("google.api_core.gapic_v1.routing_header"));
    }

    if (context.getInterfaceConfig().hasLongRunningOperations()) {
      imports.add(createImport("google.api_core.operations_v1"));
      imports.add(createImport("google.api_core.operation"));
    }

    if (context.getInterfaceConfig().hasPageStreamingMethods()) {
      imports.add(createImport("google.api_core.page_iterator"));
    }

    if (!(new PathTemplateTransformer().generateFormatResourceFunctions(context)).isEmpty()) {
      imports.add(createImport("google.api_core.path_template"));
    }

    if (hasOneOf(context)) {
      imports.add(createImport("google.api_core.protobuf_helpers"));
    }

    Collections.sort(imports, importFileViewComparator());
    return imports;
  }

  private boolean hasRequestHeaderParams(GapicInterfaceContext context) {
    return context
        .getInterfaceConfig()
        .getMethodConfigs()
        .stream()
        .anyMatch(config -> config.getHeaderRequestParams().iterator().hasNext());
  }

  private boolean hasOneOf(InterfaceContext context) {
    return context
        .getInterfaceConfig()
        .getMethodConfigs()
        .stream()
        .anyMatch(config -> !config.getOneofNames(context.getNamer()).isEmpty());
  }

  private List<ImportFileView> generateMainAppImports(InterfaceContext context) {
    List<ImportFileView> imports =
        generateFileHeaderAppImports(context.getImportTypeTable().getImports());
    SurfaceNamer namer = context.getNamer();
    imports.add(
        createImport(
            namer.getPackageName(), namer.getClientConfigName(context.getInterfaceConfig())));
    imports.add(
        createImport(
            namer.getPackageName() + ".transports",
            namer.getGrpcTransportImportName(context.getInterfaceConfig())));
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
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    imports
        .add(generateApiImport(context.getNamer()))
        .addAll(generateProtoImports(context, specItemNodes));
    boolean needIOUtility =
        Streams.stream(specItemNodes)
            .anyMatch(node -> node.getLineType() == InitCodeLineType.ReadFileInitLine);
    if (needIOUtility) {
      imports.add(createImport("io"));
    }
    return imports.build();
  }

  private ImportFileView generateApiImport(SurfaceNamer namer) {
    String moduleName = namer.getTopLevelNamespace();
    String attributeName = namer.getApiWrapperModuleName();
    if (Strings.isNullOrEmpty(moduleName)) {
      return createImport(attributeName);
    }
    return createImport(moduleName, attributeName);
  }

  private List<ImportFileView> generateProtoImports(
      GapicMethodContext context, Iterable<InitCodeNode> specItemNodes) {
    Set<ImportFileView> protoImports = new TreeSet<>(importFileViewComparator());
    for (InitCodeNode item : specItemNodes) {
      TypeModel type = item.getType();
      // Exclude map entry types
      // Exclude message types since the samples show the use of dicts rather than protobufs.
      if (type.isEnum()) {
        protoImports.add(
            createImport(context.getNamer().getVersionedDirectoryNamespace(), "enums"));
      }
    }
    return new ArrayList<>(protoImports);
  }

  private List<ImportFileView> generateTestStandardImports() {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    imports.add(createImport("mock"));
    imports.add(createImport("pytest"));
    return imports.build();
  }

  private List<ImportFileView> generateSmokeTestStandardImports(boolean requireProjectId) {
    ImmutableList.Builder<ImportFileView> imports = ImmutableList.builder();
    if (requireProjectId) {
      imports.add(createImport("os"));
    }
    imports.add(createImport("time"));
    return imports.build();
  }

  private List<ImportFileView> generateTestExternalImports(GapicInterfaceContext context) {
    ImmutableList.Builder<ImportFileView> externalImports = ImmutableList.builder();
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
        appImports.add(createImport(context.getNamer().getVersionedDirectoryNamespace(), "enums"));
      } else {
        appImports.add(generateAppImport(entry.getKey(), entry.getValue().getNickname()));
      }
    }
    appImports.add(generateApiImport(context.getNamer()));
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

  public ImportSectionView generateTypesImportSection(
      Model model, GapicProductConfig productConfig) {
    ImportSectionView.Builder importView = generateTypesProtoImports(model, productConfig);
    return importView
        .externalImports(generateTypesExternalImports())
        .standardImports(generateTypesStandardImports())
        .build();
  }

  private ImportSectionView.Builder generateTypesProtoImports(
      Model model, GapicProductConfig productConfig) {
    ImportSectionView.Builder importView = ImportSectionView.newBuilder();

    ModelTypeTable allTypeTable = emptyTypeTable(productConfig);

    // Imports from the same API client library package.
    Set<ImportFileView> localImports = new TreeSet<>(importFileViewComparator());

    // Shared imports, e.g. protobuf imports.
    Set<ImportFileView> sharedImports = new TreeSet<>(importFileViewComparator());

    // All imports.
    Set<ImportFileView> appImports = new TreeSet<>(importFileViewComparator());
    Set<String> localImportNames = new HashSet<>();
    Set<String> sharedImportNames = new HashSet<>();

    SymbolTable symbolTable = model.getSymbolTable();
    Collection<ProtoFile> localImportFiles =
        productConfig
            .getInterfaceConfigMap()
            .keySet()
            .stream()
            .map(symbolTable::lookupInterface)
            .filter(Objects::nonNull)
            .map(Interface::getFile)
            .collect(ImmutableSet.toImmutableSet());

    Iterable<ProtoFile> allFiles = model.reachable(model.getFiles());

    // Skip API interfaces excluded from interface configs.
    Collection<ProtoFile> skippedFiles =
        Streams.stream(allFiles)
            .filter(ProtoFile::isSource)
            .flatMap(f -> f.getInterfaces().stream())
            .filter(Interface::isReachable)
            .filter(i -> productConfig.getInterfaceConfig(i) == null)
            .map(Interface::getFile)
            .collect(ImmutableSet.toImmutableSet());

    // Can't use Collection#removeAll since allFiles is an Iterable.
    Iterable<ProtoFile> protoFileDependencies =
        Streams.stream(allFiles)
            .filter(f -> !skippedFiles.contains(f))
            .collect(ImmutableSet.toImmutableSet());

    // Save proto file import names to the type table for disambiguation.
    populateTypeTable(
        protoFileDependencies, allTypeTable, localImportNames, sharedImportNames, localImportFiles);

    // Get disambiguated imports.
    for (Map.Entry<String, TypeAlias> entry : allTypeTable.getImports().entrySet()) {
      String importFullName = entry.getKey();
      String nickName = entry.getValue().getNickname();
      appImports.add(generateAppImport(importFullName, nickName));
      if (localImportNames.contains(importFullName)) {
        localImports.add(generateAppImport(importFullName, nickName));
      } else if (sharedImportNames.contains(importFullName)) {
        sharedImports.add(generateAppImport(importFullName, nickName));
      }
    }
    importView.localImports(ImmutableList.copyOf(localImports));
    importView.sharedImports(ImmutableList.copyOf(sharedImports));
    importView.appImports(ImmutableList.copyOf(appImports));

    return importView;
  }

  private ModelTypeTable emptyTypeTable(GapicProductConfig productConfig) {
    return new ModelTypeTable(
        new PythonTypeTable(productConfig.getPackageName()),
        new PythonModelTypeNameConverter(productConfig.getPackageName()));
  }

  private List<ImportFileView> generateTypesExternalImports() {
    return ImmutableList.of(createImport("google.api_core.protobuf_helpers", "get_messages"));
  }

  private List<ImportFileView> generateTypesStandardImports() {
    return ImmutableList.of(createImport("__future__", "absolute_import"), createImport("sys"));
  }

  /**
   * For each ProtoFile dependency, put its TypeAlias in allTypeTable; and if it is a local import,
   * then put it in the given localImportNames, otherwise if it is a shared import, but it in the
   * given sharedImportNames.
   */
  private void populateTypeTable(
      Iterable<ProtoFile> protoFileDependencies,
      ModelTypeTable allTypeTable,
      Set<String> localImportNames,
      Set<String> sharedImportNames,
      Collection<ProtoFile> localImportFiles) {
    for (ProtoFile protoFile : protoFileDependencies) {
      // For python, adding a single message from the proto file to the type table will populate
      // the type table with the correct imports.
      ImmutableList<MessageType> messages = protoFile.getMessages();
      if (!messages.isEmpty()) {
        TypeRef typeRef = TypeRef.of(messages.get(0));
        allTypeTable.getAndSaveNicknameFor(typeRef);

        if (localImportFiles
            .stream()
            .anyMatch(f -> f.getFullName().equals(protoFile.getFullName()))) {
          localImportNames.add(allTypeTable.getFullNameFor(typeRef));
        } else {
          sharedImportNames.add(allTypeTable.getFullNameFor(typeRef));
        }
      }
    }
  }

  public ImportSectionView generateVersionedInitImportSection(
      ApiModel apiModel, ProductConfig productConfig, SurfaceNamer namer, boolean packageHasEnums) {
    return ImportSectionView.newBuilder()
        .appImports(
            generateVersionedInitAppImports(apiModel, productConfig, namer, packageHasEnums))
        .standardImports(generateAbsoluteImportImportSection())
        .build();
  }

  private List<ImportFileView> generateAbsoluteImportImportSection() {
    return ImmutableList.of(createImport("__future__", "absolute_import"));
  }

  private List<ImportFileView> generateVersionedInitAppImports(
      ApiModel apiModel, ProductConfig productConfig, SurfaceNamer namer, boolean packageHasEnums) {
    Set<ImportFileView> imports = new TreeSet<>(importFileViewComparator());
    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
      InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      if (interfaceConfig == null) {
        continue;
      }

      imports.add(
          createImport(
              productConfig.getPackageName(), namer.getApiWrapperVariableName(interfaceConfig)));
    }

    if (packageHasEnums) {
      imports.add(createImport(productConfig.getPackageName(), "enums"));
    }
    imports.add(createImport(namer.getVersionedDirectoryNamespace(), "types"));
    return ImmutableList.copyOf(imports);
  }

  public ImportSectionView generateTopLeveEntryPointImportSection(
      ApiModel apiModel, ProductConfig productConfig, SurfaceNamer namer, boolean packageHasEnums) {
    return ImportSectionView.newBuilder()
        .appImports(
            generateTopLevelEntryPointAppImports(apiModel, productConfig, namer, packageHasEnums))
        .standardImports(generateAbsoluteImportImportSection())
        .build();
  }

  private List<ImportFileView> generateTopLevelEntryPointAppImports(
      ApiModel apiModel, ProductConfig productConfig, SurfaceNamer namer, boolean packageHasEnums) {
    Set<ImportFileView> imports = new TreeSet<>(importFileViewComparator());
    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
      InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      if (interfaceConfig == null) {
        continue;
      }

      imports.add(
          createImport(
              namer.getVersionedDirectoryNamespace(),
              namer.getApiWrapperClassName(interfaceConfig)));
    }
    if (packageHasEnums) {
      imports.add(createImport(namer.getVersionedDirectoryNamespace(), "enums"));
    }
    imports.add(createImport(namer.getVersionedDirectoryNamespace(), "types"));
    return ImmutableList.copyOf(imports);
  }

  public ImportSectionView generateNoxImportSection() {
    return ImportSectionView.newBuilder()
        .appImports(ImmutableList.<ImportFileView>of())
        .externalImports(generateNoxExternalImports())
        .standardImports(generateNoxStandardImports())
        .build();
  }

  private List<ImportFileView> generateNoxExternalImports() {
    return ImmutableList.of(createImport("nox"));
  }

  private List<ImportFileView> generateNoxStandardImports() {
    List<ImportFileView> imports = new ArrayList<>();
    imports.addAll(generateAbsoluteImportImportSection());
    imports.add(createImport("os"));
    Collections.sort(imports, importFileViewComparator());
    return imports;
  }
}
