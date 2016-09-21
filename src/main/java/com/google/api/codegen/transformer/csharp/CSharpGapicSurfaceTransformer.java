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
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.transformer.java.JavaModelTypeNameConverter;
import com.google.api.codegen.transformer.java.JavaSurfaceNamer;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.ApiMethodView;
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
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import io.grpc.Status.Code;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
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
      
      surfaceDocs.add(StaticLangXCommonView.newBuilder()
          .api(xapi).settings(xsettings).build());

    }
    
    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    throw new RuntimeException();
  }
  
  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new CSharpTypeTable(implicitPackageName),
        new CSharpModelTypeNameConverter(implicitPackageName));
  }

  private StaticLangXApiView generateXApi(SurfaceTransformerContext context) {
    List<StaticLangApiMethodView> methods = generateApiMethods(context);

    StaticLangXApiView.Builder xapiClass = StaticLangXApiView.newBuilder();

    xapiClass.doc(serviceTransformer.generateServiceDoc(context, null));

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.packageName(context.getApiConfig().getPackageName());
    String name = context.getNamer().getApiWrapperClassName(context.getInterface());
    xapiClass.name(name);
    xapiClass.settingsClassName(context.getNamer().getApiSettingsClassName(context.getInterface()));
    xapiClass.apiCallableMembers(apiCallableTransformer.generateStaticLangApiCallables(context));
    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.apiMethods(methods);

    // must be done as the last step to catch all imports
    xapiClass.imports(importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    xapiClass.outputPath(outputPath + File.separator + name + ".cs");

    return xapiClass.build();
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
    xsettingsClass.callSettings(apiCallableTransformer.generateCallSettings(context, retryCodesDefs, retryParamsDefs));
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
    // Common imports
    //typeTable.saveNicknameFor("java.util.List");
  }

  private List<StaticLangApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    for (Method method : context.getNonStreamingMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodTransformerContext methodContext = context.asMethodContext(method);

      if (methodConfig.isPageStreaming()) {
        /*if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(methodContext, fields));
          }
        }
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(methodContext));
        apiMethods.add(apiMethodTransformer.generatePagedCallableMethod(methodContext));
        apiMethods.add(apiMethodTransformer.generateUnpagedListCallableMethod(methodContext));*/
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            apiMethods.add(transformFlattenedMethod(apiMethodTransformer.generateFlattenedMethod(methodContext, fields, ApiMethodType.FlattenedMethod)));
            apiMethods.add(transformFlattenedMethod(apiMethodTransformer.generateFlattenedMethod(methodContext, fields, ApiMethodType.FlattenedMethodAsyncCallSettings)));
            apiMethods.add(transformFlattenedMethod(apiMethodTransformer.generateFlattenedMethod(methodContext, fields, ApiMethodType.FlattenedMethodAsyncCancellationToken)));
          }
        }
        //apiMethods.add(apiMethodTransformer.generateRequestObjectMethod(methodContext));
        //apiMethods.add(apiMethodTransformer.generateCallableMethod(methodContext));
      }
    }

    return apiMethods;
  }
  
  private StaticLangApiMethodView transformFlattenedMethod(StaticLangApiMethodView method) {
    switch (method.type())
    {
      case FlattenedMethod:
        method = AddParam(method, "CallSettings", "callSettings", "null", "If not null, applies overrides to this RPC call.");
        method = AddReturnDoc(method, "The RPC response.");
        break;
      case FlattenedMethodAsyncCallSettings:
        method = AddParam(method, "CallSettings", "callSettings", "null", "If not null, applies overrides to this RPC call.");
        method = MakeAsync(method);
        method = AddReturnDoc(method, "A Task containing the RPC response.");
        break;
      case FlattenedMethodAsyncCancellationToken:
        method = AddParam(method, "CancellationToken", "cancellationToken", null, "A <see cref=\"CancellationToken\"/> to use for this RPC.");
        method = MakeAsync(method);
        method = AddReturnDoc(method, "A Task containing the RPC response.");
        break;
      default:
        throw new RuntimeException("Unexpected method type: '" + method.type() + "'");
    }
    return method;
  }

  private StaticLangApiMethodView AddParam(StaticLangApiMethodView method, String typeName, String name, String defaultValue, String... doc) {
    List<String> docList = Arrays.asList(doc);
    return method.toBuilder()
        .methodParamsWithExtras(FluentIterable.from(method.methodParamsWithExtras()).append(RequestObjectParamView.newBuilder()
            .name(name)
            .elementTypeName("")
            .typeName(typeName)
            .setCallName("")
            .isMap(false).isArray(false)
            .defaultValue(defaultValue)
            .build()).toList())
        .doc(method.doc().toBuilder().paramDocs(FluentIterable.from(method.doc().paramDocs()).append(SimpleParamDocView.newBuilder()
            .paramName(name)
            .typeName(typeName)
            .lines(docList)
            .firstLine(doc[0])
            .remainingLines(docList.subList(1, docList.size()))
            .build()).toList()).build())
        .build();
  }

  private StaticLangApiMethodView MakeAsync(StaticLangApiMethodView method) {
    return method.toBuilder()
        .name(method.name() + "Async")
        .responseTypeName(method.hasReturnValue() ? "Task<" + method.responseTypeName() + ">" : "Task")
        .build();
  }

  private StaticLangApiMethodView AddReturnDoc(StaticLangApiMethodView method, String... lines) {
    return method.toBuilder()
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
      List<String> codeNames = FluentIterable.from(retryCodesDef.getValue())
          .transform(new Function<Code, String>() {
            @Override public String apply(Code code) {
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
