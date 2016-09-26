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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.BundlingTransformer;
import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.RetryDefinitionsTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangXApiView;
import com.google.api.codegen.viewmodel.StaticLangXCommonView;
import com.google.api.codegen.viewmodel.StaticLangXSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public abstract class CSharpGapicSurfaceCommonTransformer implements ModelToViewTransformer {

  private final ApiMethodTransformer apiMethodTransformer;
  private final ServiceTransformer serviceTransformer;
  private final PathTemplateTransformer pathTemplateTransformer;
  private final ApiCallableTransformer apiCallableTransformer;
  private final ImportTypeTransformer importTypeTransformer;
  private final PageStreamingTransformer pageStreamingTransformer;
  private final BundlingTransformer bundlingTransformer;
  private final RetryDefinitionsTransformer retryDefinitionsTransformer;

  public CSharpGapicSurfaceCommonTransformer() {
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiCallableTransformer = new ApiCallableTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.bundlingTransformer = new BundlingTransformer();
    this.importTypeTransformer = new ImportTypeTransformer();
    this.retryDefinitionsTransformer = new RetryDefinitionsTransformer();
  }

  protected abstract String getTemplateFileName();

  protected abstract String getOutputPath(SurfaceTransformerContext context);

  protected abstract String getPackageName(ApiConfig apiConfig);

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(getPackageName(apiConfig));

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              createTypeTable(getPackageName(apiConfig)),
              namer,
              new CSharpFeatureConfig());
      addCommonImports(context);
      StaticLangXApiView xapi = generateXApi(context);
      StaticLangXSettingsView xsettings = generateXSettings(context);

      surfaceDocs.add(StaticLangXCommonView.newBuilder().api(xapi).settings(xsettings).build());
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(getTemplateFileName());
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new CSharpTypeTable(implicitPackageName),
        new CSharpModelTypeNameConverter(implicitPackageName));
  }

  private StaticLangXApiView generateXApi(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    StaticLangXApiView.Builder xapiClass = StaticLangXApiView.newBuilder();
    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    xapiClass.doc(serviceTransformer.generateServiceDoc(context, null));

    xapiClass.templateFileName(getTemplateFileName());
    xapiClass.packageName(getPackageName(context.getApiConfig()));
    String name = namer.getApiWrapperClassName(context.getInterface());
    xapiClass.name(name);
    xapiClass.implName(namer.getApiWrapperClassImplName(context.getInterface()));
    xapiClass.grpcServiceName(namer.getGrpcContainerTypeName(context.getInterface()));
    xapiClass.grpcTypeName(namer.getGrpcServiceClassName(context.getInterface()));
    xapiClass.settingsClassName(context.getNamer().getApiSettingsClassName(context.getInterface()));
    List<ApiCallableView> callables = new ArrayList<>();
    for (ApiCallableView call : apiCallableTransformer.generateStaticLangApiCallables(context)) {
      if (call.type() == ApiCallableType.SimpleApiCallable
          || call.type() == ApiCallableType.BundlingApiCallable) {
        callables.add(call);
      }
    }
    xapiClass.apiCallableMembers(callables);
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);
    List<StaticLangApiMethodView> methodsImpl = new ArrayList<>();
    for (StaticLangApiMethodView method : methods) {
      if (methodTypeHasImpl(method.type())) {
        methodsImpl.add(method);
      }
    }
    xapiClass.apiMethodsImpl(methodsImpl);

    // must be done as the last step to catch all imports
    xapiClass.imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    xapiClass.outputPath(getOutputPath(context));

    return xapiClass.build();
  }

  private boolean methodTypeHasImpl(ApiMethodType type) {
    return type != ApiMethodType.FlattenedAsyncCancellationTokenMethod;
  }

  private StaticLangXSettingsView generateXSettings(SurfaceTransformerContext context) {
    StaticLangXSettingsView.Builder xsettingsClass = StaticLangXSettingsView.newBuilder();
    xsettingsClass.templateFileName(""); // Unused in C#
    xsettingsClass.packageName(getPackageName(context.getApiConfig()));
    xsettingsClass.doc(generateSettingsDoc(context));
    String name = context.getNamer().getApiSettingsClassName(context.getInterface());
    xsettingsClass.name(name);
    ServiceConfig serviceConfig = new ServiceConfig();
    xsettingsClass.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    xsettingsClass.servicePort(serviceConfig.getServicePort());
    xsettingsClass.authScopes(serviceConfig.getAuthScopes(context.getInterface()));
    xsettingsClass.callSettings(generateCallSettings(context));
    xsettingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    xsettingsClass.bundlingDescriptors(bundlingTransformer.generateDescriptorClasses(context));
    xsettingsClass.retryCodesDefinitions(
        retryDefinitionsTransformer.generateRetryCodesDefinitions(context));
    xsettingsClass.retryParamsDefinitions(
        retryDefinitionsTransformer.generateRetryParamsDefinitions(context));

    // must be done as the last step to catch all imports
    xsettingsClass.imports(
        importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    xsettingsClass.outputPath(""); // Not used in C#

    return xsettingsClass.build();
  }

  private void addCommonImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    // Common imports, only one class per required namespace is needed.
    typeTable.saveNicknameFor("Google.Protobuf.WellKnownTypes.SomeSortOfWellKnownType");
    typeTable.saveNicknameFor("Google.Protobuf.ByteString");
    typeTable.saveNicknameFor("Grpc.Core.ByteString");
    typeTable.saveNicknameFor("System.Collections.ObjectModel.ReadOnlyCollection");
    typeTable.saveNicknameFor("System.Threading.Tasks.Task");
    typeTable.saveNicknameFor("System.Threading.Thread");
    typeTable.saveNicknameFor("System.NotImplementedException");
    typeTable.saveNicknameFor("System.Collections.IEnumerable");
    typeTable.saveNicknameFor("System.Collections.Generic.IEnumerable");
  }

  public List<ApiCallSettingsView> generateCallSettings(SurfaceTransformerContext context) {
    // This method can be removed once mixins are supported in C#
    boolean mixinsDisabled = !context.getFeatureConfig().enableMixins();
    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();
    for (Method method : context.getSupportedMethods()) {
      if (mixinsDisabled && context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        continue;
      }
      List<ApiCallSettingsView> calls =
          apiCallableTransformer.generateApiCallableSettings(context.asMethodContext(method));
      settingsMembers.addAll(calls);
    }
    return settingsMembers;
  }

  private List<StaticLangApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<ParamWithSimpleDoc> callSettingsParam =
        ImmutableList.of(
            makeParam(
                "CallSettings",
                "callSettings",
                "null",
                "If not null, applies overrides to this RPC call."));
    List<ParamWithSimpleDoc> cancellationTokenParam =
        ImmutableList.of(
            makeParam(
                "CancellationToken",
                "cancellationToken",
                null,
                "A <see cref=\"CancellationToken\"/> to use for this RPC."));
    List<ParamWithSimpleDoc> pagedMethodAdditionalParams =
        ImmutableList.of(
            makeParam(
                "string",
                "pageToken",
                "null",
                "The token returned from the previous request.",
                "A value of <c>null</c> or an empty string retrieves the first page."),
            makeParam(
                "int?",
                "pageSize",
                "null",
                "The size of page to request. The response will not be larger than this, but may be smaller.",
                "A value of <c>null</c> or 0 uses a server-defined page size."),
            callSettingsParam.get(0));
    boolean mixinsDisabled = !context.getFeatureConfig().enableMixins();

    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (mixinsDisabled && methodConfig.getRerouteToGrpcInterface() != null) {
        continue;
      }
      MethodTransformerContext methodContext = context.asMethodContext(method);
      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedAsyncMethod(
                    methodContext, fields, pagedMethodAdditionalParams));
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(
                    methodContext, fields, pagedMethodAdditionalParams));
          }
        }
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(
                apiMethodTransformer.generateFlattenedAsyncMethod(
                    methodContext,
                    fields,
                    callSettingsParam,
                    ApiMethodType.FlattenedAsyncCallSettingsMethod));
            apiMethods.add(
                apiMethodTransformer.generateFlattenedAsyncMethod(
                    methodContext,
                    fields,
                    cancellationTokenParam,
                    ApiMethodType.FlattenedAsyncCancellationTokenMethod));
            apiMethods.add(
                apiMethodTransformer.generateFlattenedMethod(
                    methodContext, fields, callSettingsParam));
          }
        }
      }
    }

    return apiMethods;
  }

  private ParamWithSimpleDoc makeParam(
      String typeName, String name, String defaultValue, String... doc) {
    return ParamWithSimpleDoc.newBuilder()
        .name(name)
        .elementTypeName("")
        .typeName(typeName)
        .setCallName("")
        .isMap(false)
        .isArray(false)
        .defaultValue(defaultValue)
        .docLines(Arrays.asList(doc))
        .build();
  }

  public SettingsDocView generateSettingsDoc(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    SettingsDocView.Builder settingsDoc = SettingsDocView.newBuilder();
    ServiceConfig serviceConfig = new ServiceConfig();
    settingsDoc.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    settingsDoc.servicePort(serviceConfig.getServicePort());
    settingsDoc.exampleApiMethodName(""); // Unused in C#
    settingsDoc.exampleApiMethodSettingsGetter(""); // Unused in C#
    settingsDoc.apiClassName(namer.getApiWrapperClassName(context.getInterface()));
    settingsDoc.settingsVarName(namer.getApiSettingsVariableName(context.getInterface()));
    settingsDoc.settingsClassName(namer.getApiSettingsClassName(context.getInterface()));
    settingsDoc.settingsBuilderVarName(namer.getApiSettingsBuilderVarName(context.getInterface()));
    return settingsDoc.build();
  }
}
