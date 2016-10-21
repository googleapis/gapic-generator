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

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.InterfaceConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
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
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSharpGapicClientTransformer implements ModelToViewTransformer {

  private static final String XAPI_TEMPLATE_FILENAME = "csharp/gapic_client.snip";

  private final GapicCodePathMapper pathMapper;
  private final ApiMethodTransformer apiMethodTransformer;
  private final ServiceTransformer serviceTransformer;
  private final PathTemplateTransformer pathTemplateTransformer;
  private final ApiCallableTransformer apiCallableTransformer;
  private final ImportTypeTransformer importTypeTransformer;
  private final PageStreamingTransformer pageStreamingTransformer;
  private final BundlingTransformer bundlingTransformer;
  private final RetryDefinitionsTransformer retryDefinitionsTransformer;
  private final CSharpCommonTransformer csharpCommonTransformer;

  public CSharpGapicClientTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiCallableTransformer = new ApiCallableTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.bundlingTransformer = new BundlingTransformer();
    this.importTypeTransformer = new ImportTypeTransformer();
    this.retryDefinitionsTransformer = new RetryDefinitionsTransformer();
    this.csharpCommonTransformer = new CSharpCommonTransformer();
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(apiConfig.getPackageName());

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              createTypeTable(apiConfig.getPackageName()),
              namer,
              new CSharpFeatureConfig());
      csharpCommonTransformer.addCommonImports(context);
      StaticLangXApiView xapi = generateXApi(context);
      StaticLangXSettingsView xsettings = generateXSettings(context);

      surfaceDocs.add(StaticLangXCommonView.newBuilder().api(xapi).settings(xsettings).build());
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME);
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

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.packageName(context.getApiConfig().getPackageName());
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
    xapiClass.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());

    // must be done as the last step to catch all imports
    xapiClass.imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xapiClass.outputPath(outputPath + File.separator + name + ".cs");

    return xapiClass.build();
  }

  private boolean methodTypeHasImpl(ApiMethodType type) {
    return type != ApiMethodType.FlattenedAsyncCancellationTokenMethod;
  }

  private StaticLangXSettingsView generateXSettings(SurfaceTransformerContext context) {
    StaticLangXSettingsView.Builder xsettingsClass = StaticLangXSettingsView.newBuilder();
    xsettingsClass.templateFileName(""); // Unused in C#
    xsettingsClass.packageName(context.getApiConfig().getPackageName());
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
    xsettingsClass.pagedListResponseFactories(
        pageStreamingTransformer.generateFactoryClasses(context));
    xsettingsClass.bundlingDescriptors(bundlingTransformer.generateDescriptorClasses(context));
    xsettingsClass.retryCodesDefinitions(
        retryDefinitionsTransformer.generateRetryCodesDefinitions(context));
    xsettingsClass.retryParamsDefinitions(
        retryDefinitionsTransformer.generateRetryParamsDefinitions(context));
    InterfaceConfig interfaceConfig = context.getInterfaceConfig();
    xsettingsClass.hasDefaultServiceAddress(interfaceConfig.hasDefaultServiceAddress());
    xsettingsClass.hasDefaultServiceScopes(interfaceConfig.hasDefaultServiceScopes());
    xsettingsClass.hasDefaultInstance(interfaceConfig.hasDefaultInstance());

    // must be done as the last step to catch all imports
    xsettingsClass.imports(
        importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    xsettingsClass.outputPath(""); // Not used in C#

    return xsettingsClass.build();
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
    List<ParamWithSimpleDoc> pagedMethodAdditionalParams =
        new ImmutableList.Builder<ParamWithSimpleDoc>()
            .addAll(csharpCommonTransformer.pagedMethodAdditionalParams())
            .addAll(csharpCommonTransformer.callSettingsParam())
            .build();
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
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedAsyncMethod(
                    methodContext, flatteningGroup, pagedMethodAdditionalParams));
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(
                    methodContext, flatteningGroup, pagedMethodAdditionalParams));
          }
        }
      } else {
        if (methodConfig.isFlattening()) {
          for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
            apiMethods.add(
                apiMethodTransformer.generateFlattenedAsyncMethod(
                    methodContext,
                    flatteningGroup,
                    csharpCommonTransformer.callSettingsParam(),
                    ApiMethodType.FlattenedAsyncCallSettingsMethod));
            apiMethods.add(
                apiMethodTransformer.generateFlattenedAsyncMethod(
                    methodContext,
                    flatteningGroup,
                    csharpCommonTransformer.cancellationTokenParam(),
                    ApiMethodType.FlattenedAsyncCancellationTokenMethod));
            apiMethods.add(
                apiMethodTransformer.generateFlattenedMethod(
                    methodContext, flatteningGroup, csharpCommonTransformer.callSettingsParam()));
          }
        }
      }
    }

    return apiMethods;
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
    settingsDoc.hasDefaultInstance(context.getInterfaceConfig().hasDefaultInstance());
    return settingsDoc.build();
  }
}
