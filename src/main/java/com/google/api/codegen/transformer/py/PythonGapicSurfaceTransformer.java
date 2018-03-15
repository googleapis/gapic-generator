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

import com.google.api.codegen.GeneratorVersionProvider;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.GrpcElementDocTransformer;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.NamePath;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.BatchingDescriptorView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.GrpcDocView;
import com.google.api.codegen.viewmodel.GrpcElementDocView;
import com.google.api.codegen.viewmodel.GrpcMessageDocView;
import com.google.api.codegen.viewmodel.GrpcStreamingDetailView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.ParamDocView;
import com.google.api.codegen.viewmodel.PathTemplateGetterFunctionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.VersionIndexRequireView;
import com.google.api.codegen.viewmodel.metadata.VersionIndexView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoContainerElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.List;

public class PythonGapicSurfaceTransformer implements ModelToViewTransformer {
  private static final String XAPI_TEMPLATE_FILENAME = "py/main.snip";
  private static final String ENUM_TEMPLATE_FILENAME = "py/enum.snip";
  private static final String TYPES_TEMPLEATE_FILENAME = "py/types.snip";
  private static final String VERSIONED_INIT_TEMPLATE_FILENAME =
      "py/versioned_directory__init__.py.snip";
  private static final String TOP_LEVEL_ENTRY_POINT_FILENAME = "py/top_level_entry_point.snip";

  private final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new PythonApiMethodParamTransformer(), new InitCodeTransformer(importSectionTransformer));
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
  private final GrpcElementDocTransformer elementDocTransformer = new GrpcElementDocTransformer();
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;

  public PythonGapicSurfaceTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(
        XAPI_TEMPLATE_FILENAME,
        ENUM_TEMPLATE_FILENAME,
        TYPES_TEMPLEATE_FILENAME,
        VERSIONED_INIT_TEMPLATE_FILENAME,
        TOP_LEVEL_ENTRY_POINT_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ApiModel apiModel, GapicProductConfig productConfig) {
    // TODO(andrealin): Remove the ProtoApiModel cast.
    Model model = ((ProtoApiModel) apiModel).getProtoModel();
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.addAll(generateServiceSurfaces(model, productConfig));
    views.addAll(generateVersionedDirectoryViews(model, productConfig));
    views.addAll(generateTopLevelViews(model, productConfig));
    return views.build();
  }

  private Iterable<ViewModel> generateServiceSurfaces(
      Model model, GapicProductConfig productConfig) {
    ModelTypeTable modelTypeTable =
        new ModelTypeTable(
            new PythonTypeTable(productConfig.getPackageName()),
            new PythonModelTypeNameConverter(productConfig.getPackageName()));
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());
    FeatureConfig featureConfig = new DefaultFeatureConfig();
    ProtoApiModel apiModel = new ProtoApiModel(model);
    ImmutableList.Builder<ViewModel> serviceSurfaces = ImmutableList.builder();
    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, modelTypeTable, namer, featureConfig);
      addApiImports(context);
      serviceSurfaces.add(generateApiClass(context));
    }
    GrpcDocView enumFile = generateEnumView(productConfig, modelTypeTable, namer, model.getFiles());
    if (!enumFile.elementDocs().isEmpty()) {
      serviceSurfaces.add(enumFile);
    }
    return serviceSurfaces.build();
  }

  private void addApiImports(GapicInterfaceContext context) {
    for (TypeRef type : context.getInterface().getModel().getSymbolTable().getDeclaredTypes()) {
      if (type.isEnum() && type.getEnumType().isReachable()) {
        context.getImportTypeTable().getAndSaveNicknameFor(type);
        break;
      }
    }

    for (MethodModel method : context.getSupportedMethods()) {
      addMethodImports(context.asDynamicMethodContext(method));
    }
  }

  private void addMethodImports(GapicMethodContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    if (methodConfig.isLongRunningOperation()) {
      typeTable.getAndSaveNicknameFor(methodConfig.getLongRunningConfig().getReturnType());
      typeTable.getAndSaveNicknameFor(methodConfig.getLongRunningConfig().getMetadataType());
    }

    typeTable.getAndSaveNicknameFor(context.getMethod().getInputType());
    addFieldsImports(typeTable, methodConfig.getRequiredFields());
    addFieldsImports(typeTable, methodConfig.getOptionalFields());
  }

  private void addFieldsImports(ModelTypeTable typeTable, Iterable<FieldModel> fields) {
    for (FieldModel field : fields) {
      typeTable.getAndSaveNicknameFor(field);
    }
  }

  private ViewModel generateApiClass(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    String subPath =
        pathMapper.getOutputPath(
            context.getInterfaceModel().getFullName(), context.getProductConfig());
    String name = namer.getApiWrapperClassName(context.getInterfaceConfig());
    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder xapiClass = DynamicLangXApiView.newBuilder();
    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.outputPath(namer.getSourceFilePath(subPath, name));

    xapiClass.protoFilename(context.getInterface().getFile().getSimpleName());
    xapiClass.servicePhraseName(namer.getServicePhraseName(context.getInterfaceConfig()));

    xapiClass.name(name);
    xapiClass.doc(
        serviceTransformer.generateServiceDoc(context, methods.get(0), context.getProductConfig()));
    xapiClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    ApiModel model = context.getApiModel();
    xapiClass.serviceAddress(model.getServiceAddress());
    xapiClass.servicePort(model.getServicePort());
    xapiClass.serviceTitle(model.getTitle());
    xapiClass.authScopes(model.getAuthScopes());
    xapiClass.hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress());
    xapiClass.hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes());

    xapiClass.pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context));
    xapiClass.batchingDescriptors(ImmutableList.<BatchingDescriptorView>of());
    xapiClass.longRunningDescriptors(ImmutableList.<LongRunningOperationDetailView>of());
    xapiClass.grpcStreamingDescriptors(ImmutableList.<GrpcStreamingDetailView>of());
    xapiClass.hasPageStreamingMethods(context.getInterfaceConfig().hasPageStreamingMethods());
    xapiClass.hasBatchingMethods(context.getInterfaceConfig().hasBatchingMethods());
    xapiClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());

    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.pathTemplateGetterFunctions(ImmutableList.<PathTemplateGetterFunctionView>of());

    xapiClass.methodKeys(ImmutableList.<String>of());
    xapiClass.interfaceKey(context.getInterface().getFullName());
    xapiClass.clientConfigPath(namer.getClientConfigPath(context.getInterfaceConfig()));
    xapiClass.clientConfigName(namer.getClientConfigName(context.getInterfaceConfig()));
    xapiClass.grpcClientTypeName(
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getImportTypeTable(), context.getInterfaceModel()));

    xapiClass.apiMethods(methods);

    xapiClass.toolkitVersion(GeneratorVersionProvider.getGeneratorVersion());
    xapiClass.gapicPackageName(
        namer.getGapicPackageName(packageConfig.packageName(TargetLanguage.PYTHON)));
    xapiClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return xapiClass.build();
  }

  private List<ApiMethodView> generateApiMethods(GapicInterfaceContext context) {
    ImmutableList.Builder<ApiMethodView> apiMethods = ImmutableList.builder();

    for (MethodModel method : context.getSupportedMethods()) {
      apiMethods.add(apiMethodTransformer.generateMethod(context.asDynamicMethodContext(method)));
    }

    return apiMethods.build();
  }

  private GrpcDocView generateEnumView(
      GapicProductConfig productConfig,
      ModelTypeTable typeTable,
      SurfaceNamer namer,
      List<ProtoFile> files) {
    String subPath = pathMapper.getOutputPath(null, productConfig);
    GrpcDocView.Builder enumFile = GrpcDocView.newBuilder();
    enumFile.templateFileName(ENUM_TEMPLATE_FILENAME);
    enumFile.outputPath(subPath + File.separator + "enums.py");
    enumFile.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));
    enumFile.elementDocs(generateEnumFileElements(typeTable, namer, files));
    return enumFile.build();
  }

  private List<GrpcElementDocView> generateEnumFileElements(
      ModelTypeTable typeTable, SurfaceNamer namer, List<ProtoFile> containerElements) {
    ImmutableList.Builder<GrpcElementDocView> elements = ImmutableList.builder();
    for (ProtoContainerElement containerElement : containerElements) {
      elements.addAll(generateEnumFileElements(typeTable, namer, containerElement));
    }
    return elements.build();
  }

  private List<GrpcElementDocView> generateEnumFileElements(
      ModelTypeTable typeTable, SurfaceNamer namer, ProtoContainerElement containerElement) {
    ImmutableList.Builder<GrpcElementDocView> elements = ImmutableList.builder();
    elements.addAll(elementDocTransformer.generateEnumDocs(typeTable, namer, containerElement));
    for (MessageType message : containerElement.getMessages()) {
      List<GrpcElementDocView> elementDocs = generateEnumFileElements(typeTable, namer, message);
      if (!elementDocs.isEmpty()) {
        GrpcMessageDocView.Builder messageView = GrpcMessageDocView.newBuilder();
        messageView.name(namer.publicClassName(Name.upperCamel(message.getSimpleName())));
        messageView.fullName(typeTable.getFullNameFor(TypeRef.of(message)));
        messageView.fileUrl(namer.getFileUrl(message.getFile()));
        messageView.lines(namer.getDocLines(message));
        messageView.properties(ImmutableList.<ParamDocView>of());
        messageView.elementDocs(elementDocs);
        messageView.packageName(message.getFile().getFullName());
        elements.add(messageView.build());
      }
    }
    return elements.build();
  }

  private Iterable<ViewModel> generateVersionedDirectoryViews(
      Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.add(generateTypesView(model, productConfig));
    views.add(generateVersionedInitView(model, productConfig));
    return views.build();
  }

  private ViewModel generateTypesView(Model model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());
    ImportSectionView imports =
        importSectionTransformer.generateTypesImportSection(model, productConfig);
    return VersionIndexView.newBuilder()
        .templateFileName(TYPES_TEMPLEATE_FILENAME)
        .outputPath(typesOutputFile(namer))
        .requireViews(ImmutableList.<VersionIndexRequireView>of())
        .apiVersion(namer.getApiWrapperModuleVersion())
        .namespace(namer.getVersionedDirectoryNamespace())
        .packageVersion(packageConfig.generatedPackageVersionBound(TargetLanguage.PYTHON).lower())
        .fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, imports, namer))
        .build();
  }

  private ViewModel generateVersionedInitView(Model model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());
    boolean packageHasEnums = packageHasEnums(model);
    ImportSectionView imports =
        importSectionTransformer.generateVersionedInitImportSection(
            model, productConfig, namer, packageHasEnums);
    return VersionIndexView.newBuilder()
        .templateFileName(VERSIONED_INIT_TEMPLATE_FILENAME)
        .outputPath(versionedInitOutputFile(namer))
        .requireViews(versionedInitRequireViews(model, productConfig, namer))
        .apiVersion(namer.getApiWrapperModuleVersion())
        .namespace(namer.getVersionedDirectoryNamespace())
        .packageVersion(packageConfig.generatedPackageVersionBound(TargetLanguage.PYTHON).lower())
        .fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, imports, namer))
        .packageHasEnums(packageHasEnums)
        .build();
  }

  private boolean packageHasEnums(Model model) {
    for (TypeRef type : model.getSymbolTable().getDeclaredTypes()) {
      if (type.isEnum() && type.getEnumType().isReachable()) {
        return true;
      }
    }
    return false;
  }

  private List<VersionIndexRequireView> versionedInitRequireViews(
      Model model, GapicProductConfig productConfig, SurfaceNamer namer) {
    ImmutableList.Builder<VersionIndexRequireView> views = ImmutableList.builder();
    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    for (Interface apiInterface : apiInterfaces) {
      views.add(
          VersionIndexRequireView.newBuilder()
              .clientName(
                  namer.getApiWrapperClassName(productConfig.getInterfaceConfig(apiInterface)))
              .localName(
                  namer.getApiWrapperVariableName(productConfig.getInterfaceConfig(apiInterface)))
              .fileName(namer.getNotImplementedString("VersionIndexRequireView.fileName"))
              .build());
    }
    return views.build();
  }

  private ModelTypeTable emptyTypeTable(GapicProductConfig productConfig) {
    return new ModelTypeTable(
        new PythonTypeTable(productConfig.getPackageName()),
        new PythonModelTypeNameConverter(productConfig.getPackageName()));
  }

  private String typesOutputFile(SurfaceNamer namer) {
    return versionedDirectoryPath(namer) + File.separator + "types.py";
  }

  private String versionedInitOutputFile(SurfaceNamer namer) {
    return versionedDirectoryPath(namer) + File.separator + "__init__.py";
  }

  private String versionedDirectoryPath(SurfaceNamer namer) {
    String namespace = namer.getVersionedDirectoryNamespace();
    return namespace.replace(".", File.separator);
  }

  private Iterable<ViewModel> generateTopLevelViews(Model model, GapicProductConfig productConfig) {
    return ImmutableList.of(generateTopLevelEntryPoint(model, productConfig));
  }

  private ViewModel generateTopLevelEntryPoint(Model model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());
    boolean packageHasEnums = packageHasEnums(model);
    ImportSectionView imports =
        importSectionTransformer.generateTopLeveEntryPointImportSection(
            model, productConfig, namer, packageHasEnums);
    return VersionIndexView.newBuilder()
        .templateFileName(TOP_LEVEL_ENTRY_POINT_FILENAME)
        .outputPath(topLevelEntryPointFileName(namer))
        .requireViews(topLevelRequireViews(model, productConfig, namer))
        .apiVersion(namer.getApiWrapperModuleVersion())
        .namespace(namer.getVersionedDirectoryNamespace())
        .packageVersion(packageConfig.generatedPackageVersionBound(TargetLanguage.PYTHON).lower())
        .fileHeader(fileHeaderTransformer.generateFileHeader(productConfig, imports, namer))
        .packageHasEnums(packageHasEnums)
        .build();
  }

  private String topLevelEntryPointFileName(SurfaceNamer namer) {
    NamePath namePath = NamePath.dotted(namer.getVersionedDirectoryNamespace());
    String name = namePath.getHead();
    int lastUnderscoreIndex = name.lastIndexOf("_");
    if (lastUnderscoreIndex > -1
        && VersionMatcher.isVersion(name.substring(lastUnderscoreIndex + 1))) {
      name = name.substring(0, lastUnderscoreIndex);
    }
    String topLevelPath = namePath.withHead(name).toSlashed();
    return String.format("%s.py", topLevelPath);
  }

  private List<VersionIndexRequireView> topLevelRequireViews(
      Model model, GapicProductConfig productConfig, SurfaceNamer namer) {
    ImmutableList.Builder<VersionIndexRequireView> views = ImmutableList.builder();
    Iterable<Interface> apiInterfaces = new InterfaceView().getElementIterable(model);
    for (Interface apiInterface : apiInterfaces) {
      views.add(
          VersionIndexRequireView.newBuilder()
              .clientName(
                  namer.getApiWrapperClassName(productConfig.getInterfaceConfig(apiInterface)))
              .localName(
                  namer.getApiWrapperVariableName(productConfig.getInterfaceConfig(apiInterface)))
              .fileName(namer.getNotImplementedString("VersionIndexRequireView.fileName"))
              .build());
    }
    return views.build();
  }
}
