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
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.BundlingTransformer;
import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ApiCallableType;
import com.google.api.codegen.viewmodel.ApiCallableView;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.PageStreamingDescriptorClassView;
import com.google.api.codegen.viewmodel.RequestObjectParamView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.SimpleParamDocView;
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
  private final CSharpApiCallableTransformer apiCallableTransformer;
  private final ImportTypeTransformer importTypeTransformer;
  private final PageStreamingTransformer pageStreamingTransformer;
  private final BundlingTransformer bundlingTransformer;

  public CSharpGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.serviceTransformer = new ServiceTransformer();
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.apiCallableTransformer = new CSharpApiCallableTransformer();
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
              service, apiConfig, createTypeTable(apiConfig.getPackageName()), namer);
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
    String grpcName = namer.className(Name.upperCamel(context.getInterface().getSimpleName()));
    xapiClass.name(name);
    xapiClass.grpcName(grpcName);
    xapiClass.grpcTypeName(grpcName + "." + name);
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
    return type != ApiMethodType.FlattenedMethodAsyncCancellationToken;
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
    xsettingsClass.callSettings(
        apiCallableTransformer.generateCallSettings(context, retryCodesDefs, retryParamsDefs));
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

  private List<StaticLangApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    Map<String, PageStreamingDescriptorClassView> pagedByName =
        Maps.uniqueIndex(
            pageStreamingTransformer.generateDescriptorClasses(context),
            new Function<PageStreamingDescriptorClassView, String>() {
              @Override
              public String apply(PageStreamingDescriptorClassView v) {
                return v.name();
              }
            });
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();
    for (Method method : context.getNonStreamingMethods()) {
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
                transformPagedMethod(
                    apiMethodTransformer.generatePagedFlattenedMethod(
                        methodContext, fields, ApiMethodType.PagedFlattenedMethodAsync),
                    pagedByName));
            apiMethods.add(
                transformPagedMethod(
                    apiMethodTransformer.generatePagedFlattenedMethod(
                        methodContext, fields, ApiMethodType.PagedFlattenedMethod),
                    pagedByName));
          }
        }
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(
                transformFlattenedMethod(
                    apiMethodTransformer.generateFlattenedMethod(
                        methodContext, fields, ApiMethodType.FlattenedMethodAsyncCallSettings)));
            apiMethods.add(
                transformFlattenedMethod(
                    apiMethodTransformer.generateFlattenedMethod(
                        methodContext,
                        fields,
                        ApiMethodType.FlattenedMethodAsyncCancellationToken)));
            apiMethods.add(
                transformFlattenedMethod(
                    apiMethodTransformer.generateFlattenedMethod(
                        methodContext, fields, ApiMethodType.FlattenedMethod)));
          }
        }
      }
    }

    return apiMethods;
  }

  private StaticLangApiMethodView transformFlattenedMethod(StaticLangApiMethodView method) {
    switch (method.type()) {
      case FlattenedMethod:
        method =
            AddParam(
                method,
                "CallSettings",
                "callSettings",
                "null",
                "If not null, applies overrides to this RPC call.");
        method = AddReturnDoc(method, "The RPC response.");
        break;
      case FlattenedMethodAsyncCallSettings:
        method =
            AddParam(
                method,
                "CallSettings",
                "callSettings",
                "null",
                "If not null, applies overrides to this RPC call.");
        method = MakeAsync(method);
        method = AddReturnDoc(method, "A Task containing the RPC response.");
        break;
      case FlattenedMethodAsyncCancellationToken:
        method =
            AddParam(
                method,
                "CancellationToken",
                "cancellationToken",
                null,
                "A <see cref=\"CancellationToken\"/> to use for this RPC.");
        method = MakeAsync(method);
        method = AddReturnDoc(method, "A Task containing the RPC response.");
        break;
      default:
        throw new RuntimeException("Unexpected method type: '" + method.type() + "'");
    }
    return method;
  }

  private StaticLangApiMethodView transformPagedMethod(
      StaticLangApiMethodView method, Map<String, PageStreamingDescriptorClassView> pagedByName) {
    method = method.toBuilder().pagedView(pagedByName.get(method.name() + "PageStrDesc")).build();
    method =
        AddParam(
            method,
            "string",
            "pageToken",
            "null",
            "The token returned from the previous request.",
            "A value of <c>null</c> or an empty string retrieves the first page.");
    method =
        AddParam(
            method,
            "int?",
            "pageSize",
            "null",
            "The size of page to request. The response will not be larger than this, but may be smaller.",
            "A value of <c>null</c> or 0 uses a server-defined page size.");
    method =
        AddParam(
            method,
            "CallSettings",
            "callSettings",
            "null",
            "If not null, applies overrides to this RPC call.");
    String resource = method.pagedView().resourceTypeName();
    switch (method.type()) {
      case PagedFlattenedMethod:
        method =
            AddReturnDoc(
                method, "A pageable sequence of <see cref=\"" + resource + "\"/> resources.");
        break;
      case PagedFlattenedMethodAsync:
        method =
            AddReturnDoc(
                method,
                "A pageable asynchronous sequence of <see cref=\"" + resource + "\"/> resources.");
        method = method.toBuilder().name(method.name() + "Async").build();
        break;
      default:
        throw new RuntimeException("Unexpected method type: '" + method.type() + "'");
    }
    return method;
  }

  private StaticLangApiMethodView AddParam(
      StaticLangApiMethodView method,
      String typeName,
      String name,
      String defaultValue,
      String... doc) {
    List<String> docList = Arrays.asList(doc);
    return method
        .toBuilder()
        .methodParamsWithExtras(
            FluentIterable.from(method.methodParamsWithExtras())
                .append(
                    RequestObjectParamView.newBuilder()
                        .name(name)
                        .elementTypeName("")
                        .typeName(typeName)
                        .setCallName("")
                        .isMap(false)
                        .isArray(false)
                        .defaultValue(defaultValue)
                        .build())
                .toList())
        .doc(
            method
                .doc()
                .toBuilder()
                .paramDocs(
                    FluentIterable.from(method.doc().paramDocs())
                        .append(
                            SimpleParamDocView.newBuilder()
                                .paramName(name)
                                .typeName(typeName)
                                .lines(docList)
                                .firstLine(doc[0])
                                .remainingLines(docList.subList(1, docList.size()))
                                .build())
                        .toList())
                .build())
        .build();
  }

  private StaticLangApiMethodView MakeAsync(StaticLangApiMethodView method) {
    return method
        .toBuilder()
        .name(method.name() + "Async")
        .responseTypeName(
            method.hasReturnValue() ? "Task<" + method.responseTypeName() + ">" : "Task")
        .build();
  }

  private StaticLangApiMethodView AddReturnDoc(StaticLangApiMethodView method, String... lines) {
    return method
        .toBuilder()
        .doc(method.doc().toBuilder().returnsDocLines(Arrays.asList(lines)).build())
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
                      return namer.methodName(Name.upperUnderscore(code.toString()));
                    }
                  })
              .toList();
      definitions.add(
          RetryCodesDefinitionView.newBuilder()
              .key(retryCodesDef.getKey())
              .name(namer.methodName(Name.from(retryCodesDef.getKey())))
              .codes(retryCodesDef.getValue())
              .codeNames(codeNames)
              .build());
    }

    return definitions;
  }

  private List<RetryParamsDefinitionView> generateRetryParamsDefinitions(
      SurfaceTransformerContext context) {
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
