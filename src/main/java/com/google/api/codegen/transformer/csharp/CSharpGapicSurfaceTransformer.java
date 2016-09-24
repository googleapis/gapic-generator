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
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiCallableType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangXApiView;
import com.google.api.codegen.viewmodel.StaticLangXCommonView;
import com.google.api.codegen.viewmodel.StaticLangXSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.gax.core.RetrySettings;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import io.grpc.Status.Code;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CSharpGapicSurfaceTransformer implements ModelToViewTransformer {

  private static final String XAPI_TEMPLATE_FILENAME = "csharp/gapic_client.snip";

  private final GapicCodePathMapper pathMapper;
  private final ApiMethodTransformer apiMethodTransformer;
  private final ServiceTransformer serviceTransformer;
  private final PathTemplateTransformer pathTemplateTransformer;
  private final ApiCallableTransformer apiCallableTransformer;
  private final ImportTypeTransformer importTypeTransformer;
  private final PageStreamingTransformer pageStreamingTransformer;
  private final BundlingTransformer bundlingTransformer;

  public CSharpGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiCallableTransformer = new ApiCallableTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.bundlingTransformer = new BundlingTransformer();
    this.importTypeTransformer = new ImportTypeTransformer();
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
      addCommonImports(context);
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
    List<ApiCallableView> callables =
        FluentIterable.from(apiCallableTransformer.generateStaticLangApiCallables(context))
            .filter(
                new Predicate<ApiCallableView>() {
                  @Override
                  public boolean apply(ApiCallableView call) {
                    return call.type() == ApiCallableType.SimpleApiCallable
                        || call.type() == ApiCallableType.BundlingApiCallable;
                  }
                })
            .toList();
    xapiClass.apiCallableMembers(callables);
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);
    xapiClass.apiMethodsImpl(
        FluentIterable.from(methods)
            .filter(
                new Predicate<StaticLangApiMethodView>() {
                  @Override
                  public boolean apply(StaticLangApiMethodView method) {
                    return methodTypeHasImpl(method.type());
                  }
                })
            .toList());

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
    List<RetryCodesDefinitionView> retryCodesDefs = generateRetryCodesDefinitions(context);
    List<RetryParamsDefinitionView> retryParamsDefs = generateRetryParamsDefinitions(context);
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
    xsettingsClass.callSettings(generateCallSettings(context, retryCodesDefs, retryParamsDefs));
    xsettingsClass.pageStreamingDescriptors(
        pageStreamingTransformer.generateDescriptorClasses(context));
    xsettingsClass.bundlingDescriptors(bundlingTransformer.generateDescriptorClasses(context));
    xsettingsClass.retryCodesDefinitions(retryCodesDefs);
    xsettingsClass.retryParamsDefinitions(retryParamsDefs);

    // must be done as the last step to catch all imports
    xsettingsClass.imports(
        importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xsettingsClass.outputPath(outputPath + "/" + name + ".java");

    return xsettingsClass.build();
  }

  private void addCommonImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    // Common imports, only one class per required namespace is needed.
    typeTable.saveNicknameFor("Google.Protobuf.WellKnownTypes.SomeSortOfWellKnownType");
    typeTable.saveNicknameFor("Grpc.Core.ByteString");
    typeTable.saveNicknameFor("System.Collections.ObjectModel.ReadOnlyCollection");
    typeTable.saveNicknameFor("System.Threading.Tasks.Task");
    typeTable.saveNicknameFor("System.Threading.Thread");
    typeTable.saveNicknameFor("System.NotImplementedException");
    typeTable.saveNicknameFor("System.Collections.IEnumerable");
    typeTable.saveNicknameFor("System.Collections.Generic.IEnumerable");
  }

  public List<ApiCallSettingsView> generateCallSettings(
      SurfaceTransformerContext context,
      List<RetryCodesDefinitionView> retryCodes,
      List<RetryParamsDefinitionView> retryParams) {
    final Map<String, RetryCodesDefinitionView> retryCodesByKey =
        Maps.uniqueIndex(
            retryCodes,
            new Function<RetryCodesDefinitionView, String>() {
              @Override
              public String apply(RetryCodesDefinitionView v) {
                return v.key();
              }
            });
    final Map<String, RetryParamsDefinitionView> retryParamsByKey =
        Maps.uniqueIndex(
            retryParams,
            new Function<RetryParamsDefinitionView, String>() {
              @Override
              public String apply(RetryParamsDefinitionView v) {
                return v.key();
              }
            });

    List<ApiCallSettingsView> settingsMembers = new ArrayList<>();
    for (Method method : context.getSupportedMethods()) {
      if (context.getMethodConfig(method).getRerouteToGrpcInterface() != null) {
        // Temporary hack to exclude mixins for the moment. To be removed.
        continue;
      }
      List<ApiCallSettingsView> calls =
          FluentIterable.from(
                  apiCallableTransformer.generateApiCallableSettings(
                      context.asMethodContext(method)))
              .transform(
                  new Function<ApiCallSettingsView, ApiCallSettingsView>() {
                    @Override
                    public ApiCallSettingsView apply(ApiCallSettingsView call) {
                      return call.toBuilder()
                          .retryCodesView(retryCodesByKey.get(call.retryCodesName()))
                          .retryParamsView(retryParamsByKey.get(call.retryParamsName()))
                          .build();
                    }
                  })
              .toList();
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

    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (methodConfig.getRerouteToGrpcInterface() != null) {
        // Temporary hack to exclude mixins for the moment. To be removed.
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

  private List<RetryCodesDefinitionView> generateRetryCodesDefinitions(
      SurfaceTransformerContext context) {
    // TODO: Merge this with the equivalent Java method, into a new transformer
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    final SurfaceNamer namer = context.getNamer();
    for (Entry<String, ImmutableSet<Code>> retryCodesDef :
        context.getInterfaceConfig().getRetryCodesDefinition().entrySet()) {
      List<String> codeNames =
          FluentIterable.from(retryCodesDef.getValue())
              .transform(
                  new Function<Code, String>() {
                    @Override
                    public String apply(Code code) {
                      return namer.getStatusCodeName(code);
                    }
                  })
              .toList();
      definitions.add(
          RetryCodesDefinitionView.newBuilder()
              .key(retryCodesDef.getKey())
              .name(namer.getRetryDefinitionName(retryCodesDef.getKey()))
              .codes(retryCodesDef.getValue())
              .codeNames(codeNames)
              .build());
    }

    return definitions;
  }

  private List<RetryParamsDefinitionView> generateRetryParamsDefinitions(
      SurfaceTransformerContext context) {
    // TODO: Merge this with the equivalent Java method, into a new transformer
    List<RetryParamsDefinitionView> definitions = new ArrayList<>();

    SurfaceNamer namer = context.getNamer();
    for (Entry<String, RetrySettings> retryCodesDef :
        context.getInterfaceConfig().getRetrySettingsDefinition().entrySet()) {
      RetrySettings settings = retryCodesDef.getValue();
      RetryParamsDefinitionView.Builder params = RetryParamsDefinitionView.newBuilder();
      params.key(retryCodesDef.getKey());
      params.name(namer.methodName(Name.from(retryCodesDef.getKey())));
      params.initialRetryDelay(settings.getInitialRetryDelay());
      params.retryDelayMultiplier(settings.getRetryDelayMultiplier());
      params.maxRetryDelay(settings.getMaxRetryDelay());
      params.initialRpcTimeout(settings.getInitialRpcTimeout());
      params.rpcTimeoutMultiplier(settings.getRpcTimeoutMultiplier());
      params.maxRpcTimeout(settings.getMaxRpcTimeout());
      params.totalTimeout(settings.getTotalTimeout());
      definitions.add(params.build());
    }

    return definitions;
  }
}
