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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.ruby.RubyUtil;
import com.google.api.codegen.transformer.BatchingTransformer;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.VersionMatcher;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.CredentialsClassFileView;
import com.google.api.codegen.viewmodel.CredentialsClassView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.GrpcStreamingDetailView;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ImportTypeView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.PathTemplateGetterFunctionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.ModuleView;
import com.google.api.codegen.viewmodel.metadata.SimpleModuleView;
import com.google.api.codegen.viewmodel.metadata.VersionIndexRequireView;
import com.google.api.codegen.viewmodel.metadata.VersionIndexType;
import com.google.api.codegen.viewmodel.metadata.VersionIndexView;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in Ruby. */
public class RubyGapicSurfaceTransformer implements ModelToViewTransformer {
  private static final String VERSION_INDEX_TEMPLATE_FILE = "ruby/version_index.snip";
  private static final String XAPI_TEMPLATE_FILENAME = "ruby/main.snip";
  private static final String CREDENTIALS_CLASS_TEMPLATE_FILE = "ruby/credentials.snip";
  // This assumes the api is a google-cloud api.
  private static final List<String> DEFAULT_PATH_ENV_VARS =
      ImmutableList.of("GOOGLE_CLOUD_KEYFILE", "GCLOUD_KEYFILE");
  private static final List<String> DEFAULT_JSON_ENV_VARS =
      ImmutableList.of("GOOGLE_CLOUD_KEYFILE_JSON", "GCLOUD_KEYFILE_JSON");
  private static final List<String> DEFAULT_PATHS =
      ImmutableList.of("~/.config/gcloud/application_default_credentials.json");

  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new RubyImportSectionTransformer());
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(new RubyApiMethodParamTransformer());
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final BatchingTransformer batchingTransformer = new BatchingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();

  public RubyGapicSurfaceTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(
        XAPI_TEMPLATE_FILENAME, VERSION_INDEX_TEMPLATE_FILE, CREDENTIALS_CLASS_TEMPLATE_FILE);
  }

  @Override
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.add(generateVersionIndexView(model, productConfig));
    if (RubyUtil.hasMajorVersion(productConfig.getPackageName())) {
      views.add(generateTopLevelIndexView(model, productConfig));
    }
    views.addAll(generateApiClasses(model, productConfig));
    if (!RubyUtil.isLongrunning(productConfig.getPackageName())) {
      views.add(generateCredentialsView(model, productConfig));
    }
    return views.build();
  }

  private List<ViewModel> generateApiClasses(ApiModel model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());
    FeatureConfig featureConfig = new RubyFeatureConfig();
    ImmutableList.Builder<ViewModel> serviceSurfaces = ImmutableList.builder();
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      String packageName = productConfig.getPackageName();
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new RubyTypeTable(productConfig.getPackageName()),
              new RubyModelTypeNameConverter(packageName));
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, modelTypeTable, namer, featureConfig);
      serviceSurfaces.add(generateApiClass(context, packageName));
    }
    return serviceSurfaces.build();
  }

  private ViewModel generateApiClass(GapicInterfaceContext context, String packageName) {
    SurfaceNamer namer = context.getNamer();
    PackageMetadataNamer metadataNamer = new RubyPackageMetadataNamer(packageName);
    String subPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    String name = namer.getApiWrapperClassName(context.getInterfaceConfig());
    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder xapiClass = DynamicLangXApiView.newBuilder();
    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.outputPath(namer.getSourceFilePath(subPath, name));

    xapiClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    xapiClass.protoFilename(context.getInterface().getFile().getSimpleName());

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
    xapiClass.batchingDescriptors(batchingTransformer.generateDescriptors(context));
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
    xapiClass.grpcClientTypeName(
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getImportTypeTable(), context.getInterfaceModel()));

    xapiClass.apiMethods(methods);

    xapiClass.gapicPackageName(
        RubyUtil.isLongrunning(context.getProductConfig().getPackageName())
            ? "google-gax"
            : metadataNamer.getMetadataIdentifier());

    xapiClass.fullyQualifiedCredentialsClassName(namer.getFullyQualifiedCredentialsClassName());
    xapiClass.defaultCredentialsInitializerCall(
        RubyUtil.isLongrunning(context.getProductConfig().getPackageName())
            ? "default(scopes: scopes)"
            : "default");
    return xapiClass.build();
  }

  private List<ApiMethodView> generateApiMethods(GapicInterfaceContext context) {
    ImmutableList.Builder<ApiMethodView> apiMethods = ImmutableList.builder();
    for (MethodModel method : context.getSupportedMethods()) {
      apiMethods.add(
          apiMethodTransformer.generateMethod(
              context.asDynamicMethodContext(method), context.getApiModel().hasMultipleServices()));
    }
    return apiMethods.build();
  }

  private ViewModel generateVersionIndexView(ApiModel model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());

    ImmutableList.Builder<VersionIndexRequireView> requireViews = ImmutableList.builder();
    Iterable<? extends InterfaceModel> interfaces = model.getInterfaces();
    for (InterfaceModel apiInterface : interfaces) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      InterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      requireViews.add(
          VersionIndexRequireView.newBuilder()
              .clientName(namer.getFullyQualifiedApiWrapperClassName(interfaceConfig))
              .fileName(namer.getServiceFileName(interfaceConfig))
              .serviceName(namer.getPackageServiceName(context.getInterfaceConfig()))
              .doc(
                  serviceTransformer.generateServiceDoc(
                      context, generateApiMethods(context).get(0), productConfig))
              .build());
    }

    // append any additional types
    Set<String> requireTypes = new HashSet<>();
    for (TypeModel type : model.getAdditionalTypes()) {
      if (type instanceof ProtoTypeRef) {
        ProtoTypeRef t = (ProtoTypeRef) type;
        String name =
            namer.getProtoFileImportName(
                t.getProtoType().getMessageType().getFile().getSimpleName());
        requireTypes.add(name);
      }
    }

    return VersionIndexView.newBuilder()
        .apiVersion(packageConfig.apiVersion())
        .requireViews(requireViews.build())
        .requireTypes(ImmutableList.copyOf(requireTypes))
        .templateFileName(VERSION_INDEX_TEMPLATE_FILE)
        .packageVersion(packageConfig.generatedPackageVersionBound(TargetLanguage.RUBY).lower())
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig, ImportSectionView.newBuilder().build(), namer))
        .outputPath("lib" + File.separator + versionPackagePath(namer) + ".rb")
        .modules(generateModuleViews(model, productConfig, true))
        .type(VersionIndexType.VersionIndex)
        .build();
  }

  private ViewModel generateCredentialsView(ApiModel model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());
    CredentialsClassView credentialsClass = generateCredentialsClass(model, productConfig);
    ImportSectionView importSection =
        ImportSectionView.newBuilder()
            .externalImports(
                ImmutableList.of(
                    ImportFileView.newBuilder()
                        .moduleName("googleauth")
                        .types(ImmutableList.<ImportTypeView>of())
                        .build()))
            .build();
    List<String> modules = namer.getTopLevelApiModules();
    return CredentialsClassFileView.newBuilder()
        .outputPath("lib" + File.separator + namer.getCredentialsClassImportName() + ".rb")
        .templateFileName(CREDENTIALS_CLASS_TEMPLATE_FILE)
        .credentialsClass(credentialsClass)
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig, importSection, namer, ImmutableList.copyOf(modules)))
        .build();
  }

  private CredentialsClassView generateCredentialsClass(
      ApiModel model, GapicProductConfig productConfig) {

    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());

    String sanitizedShortName = packageConfig.shortName().replaceAll("[^A-Za-z0-9]", " ");
    Name.lowerCamel(sanitizedShortName.split(" "));
    String apiSpecificPathEnvVar =
        namer.inittedConstantName(Name.lowerCamel(sanitizedShortName.split(" ")).join("keyfile"));
    String apiSpecificJsonEnvVar =
        namer.inittedConstantName(
            Name.lowerCamel(sanitizedShortName.split(" ")).join("keyfile").join("json"));

    List<String> pathEnvVars =
        ImmutableList.<String>builder()
            .add(apiSpecificPathEnvVar)
            .addAll(DEFAULT_PATH_ENV_VARS)
            .build();
    List<String> jsonEnvVars =
        ImmutableList.<String>builder()
            .add(apiSpecificJsonEnvVar)
            .addAll(DEFAULT_JSON_ENV_VARS)
            .build();

    return CredentialsClassView.newBuilder()
        .pathEnvVars(pathEnvVars)
        .jsonEnvVars(jsonEnvVars)
        .defaultPaths(DEFAULT_PATHS)
        .scopes(model.getAuthScopes())
        .build();
  }

  private ViewModel generateTopLevelIndexView(ApiModel model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());

    ImmutableList.Builder<VersionIndexRequireView> requireViews = ImmutableList.builder();
    List<String> modules = namer.getTopLevelApiModules();
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      GapicInterfaceContext context = createContext(apiInterface, productConfig);
      String clientName = namer.getPackageName();
      String serviceName = namer.getPackageServiceName(context.getInterfaceConfig());
      if (model.hasMultipleServices()) {
        clientName += "::" + serviceName;
      }
      String topLevelNamespace = namer.getTopLevelNamespace();
      String postVersionNamespace = postVersionNamespace(namer);
      requireViews.add(
          VersionIndexRequireView.newBuilder()
              .clientName(clientName)
              .serviceName(serviceName)
              .fileName(versionPackagePath(namer))
              .topLevelNamespace(topLevelNamespace)
              .postVersionNamespace(postVersionNamespace)
              .doc(
                  serviceTransformer.generateServiceDoc(
                      context, generateApiMethods(context).get(0), productConfig))
              .build());
    }

    String versionDirBasePath =
        namer.packageFilePathPiece(Name.upperCamel(modules.get(modules.size() - 1)));

    return VersionIndexView.newBuilder()
        .apiVersion(packageConfig.apiVersion())
        .requireViews(requireViews.build())
        .templateFileName(VERSION_INDEX_TEMPLATE_FILE)
        .packageVersion(packageConfig.generatedPackageVersionBound(TargetLanguage.RUBY).lower())
        .fileHeader(
            fileHeaderTransformer.generateFileHeader(
                productConfig, ImportSectionView.newBuilder().build(), namer))
        .outputPath("lib" + File.separator + topLevelPackagePath(namer) + ".rb")
        .modules(generateModuleViews(model, productConfig, false))
        .type(VersionIndexType.TopLevelIndex)
        .versionDirBasePath(versionDirBasePath)
        .postVersionDirPath(postVersionDirPath(namer))
        .build();
  }

  private List<ModuleView> generateModuleViews(
      ApiModel model, GapicProductConfig productConfig, boolean includeVersionModule) {
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());
    RubyPackageMetadataTransformer metadataTransformer =
        new RubyPackageMetadataTransformer(packageConfig);
    RubyPackageMetadataNamer packageNamer =
        new RubyPackageMetadataNamer(productConfig.getPackageName());

    ImmutableList.Builder<ModuleView> moduleViews = ImmutableList.builder();

    for (String moduleName : namer.getApiModules()) {
      boolean isVersion = moduleName.equals(namer.getApiWrapperModuleVersion());

      if (moduleName.equals(namer.getModuleServiceName())) {
        moduleViews.add(
            metadataTransformer
                .generateReadmeMetadataView(model, productConfig, packageNamer)
                .moduleName(moduleName)
                .build());
      } else if (includeVersionModule || !isVersion) {
        moduleViews.add(SimpleModuleView.newBuilder().moduleName(moduleName).build());
      }

      if (!includeVersionModule && isVersion) {
        break;
      }
    }
    return moduleViews.build();
  }

  private String versionPackagePath(SurfaceNamer namer) {
    List<String> parts = namer.getApiModules();
    List<String> paths = new ArrayList<>();
    for (String part : parts) {
      paths.add(namer.packageFilePathPiece(Name.upperCamel(part)));
    }
    return Joiner.on(File.separator).join(paths);
  }

  private String topLevelPackagePath(SurfaceNamer namer) {
    List<String> paths = new ArrayList<>();
    for (String part : namer.getTopLevelApiModules()) {
      paths.add(namer.packageFilePathPiece(Name.upperCamel(part)));
    }
    return Joiner.on(File.separator).join(paths);
  }

  private String postVersionDirPath(SurfaceNamer namer) {
    List<String> parts = namer.getApiModules();
    List<String> paths = new ArrayList<>();
    boolean versionFound = false;
    for (String part : parts) {
      if (versionFound) {
        paths.add(namer.packageFilePathPiece(Name.upperCamel(part)));
      }
      if (VersionMatcher.isVersion(part)) {
        versionFound = true;
      }
    }
    return Joiner.on(File.separator).join(paths);
  }

  private String postVersionNamespace(SurfaceNamer namer) {
    List<String> parts = namer.getApiModules();
    List<String> paths = new ArrayList<>();
    boolean versionFound = false;
    for (String part : parts) {
      if (versionFound) {
        paths.add(part);
      }
      if (VersionMatcher.isVersion(part)) {
        versionFound = true;
      }
    }
    return Joiner.on("::").join(paths);
  }

  private GapicInterfaceContext createContext(
      InterfaceModel apiInterface, GapicProductConfig productConfig) {
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        new ModelTypeTable(
            new RubyTypeTable(productConfig.getPackageName()),
            new RubyModelTypeNameConverter(productConfig.getPackageName())),
        new RubySurfaceNamer(productConfig.getPackageName()),
        new RubyFeatureConfig());
  }
}
