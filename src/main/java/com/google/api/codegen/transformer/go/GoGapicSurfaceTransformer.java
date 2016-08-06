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
package com.google.api.codegen.transformer.go;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.CollectionConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.InterfaceConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.LanguageUtil;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.codegen.go.GoContextCommon;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ApiCallSettingsView;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.BundlingDescriptorClassView;
import com.google.api.codegen.viewmodel.PathTemplateView;
import com.google.api.codegen.viewmodel.RetryCodesDefinitionView;
import com.google.api.codegen.viewmodel.RetryParamsDefinitionView;
import com.google.api.codegen.viewmodel.SettingsDocView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangXApiView;
import com.google.api.codegen.viewmodel.StaticLangXCombinedView;
import com.google.api.codegen.viewmodel.StaticLangXSettingsView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.gax.core.RetrySettings;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.grpc.Status.Code;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;

public class GoGapicSurfaceTransformer implements ModelToViewTransformer {

  private static final String XAPI_TEMPLATE_FILENAME = "go/main.snip";
  private static final String SAMPLE_TEMPLATE_FILENAME = "go/example.snip";
  private static final String DOC_TEMPLATE_FILENAME = "go/doc.snip";

  private final GapicCodePathMapper pathMapper;
  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final ApiMethodTransformer apiMethodTransformer = new ApiMethodTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();

  public GoGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> models = new ArrayList<ViewModel>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      models.add(generate(model, service, apiConfig));
    }
    return models;
  }

  private StaticLangXCombinedView generate(Model model, Interface service, ApiConfig apiConfig) {
    StaticLangXCombinedView.Builder view = StaticLangXCombinedView.newBuilder();

    String packageName = getPackageName(apiConfig.getPackageName());
    view.packageName(packageName);

    view.outputPath(
        apiConfig.getPackageName().replace('/', File.separatorChar)
            + File.separator
            + getReducedServiceName(service)
            + "_client.go");

    view.templateFileName(XAPI_TEMPLATE_FILENAME);
    view.name(getClientPrefix(model, service, packageName));
    view.serviceName(service.getSimpleName());
    view.servicePackageName(GoModelTypeNameConverter.localPackageName(service));
    view.serviceDoc(comments(service));

    SurfaceNamer namer = new GoSurfaceNamer(getReducedServiceName(service));
    SurfaceTransformerContext context =
        SurfaceTransformerContext.create(service, apiConfig, createTypeTable(), namer);

    view.retryCodesDefinitions(generateRetryCodesDefinitions(context));
    view.retryParamsDefinitions(generateRetryParamsDefinitions(context));

    view.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    view.pathTemplateGetters(pathTemplateTransformer.generatePathTemplateGetterFunctions(context));
    view.callSettings(apiCallableTransformer.generateCallSettings(context));
    view.apiMethods(generateApiMethods(context));

    ServiceConfig serviceConfig = new ServiceConfig();
    view.serviceAddress(serviceConfig.getServiceAddress(service));
    view.servicePort(serviceConfig.getServicePort());
    view.authScopes(serviceConfig.getAuthScopes(service));

    return view.build();
  }

  private ModelTypeTable createTypeTable() {
    return new ModelTypeTable(new GoTypeTable(), new GoModelTypeNameConverter());
  }

  /**
   * Returns the doc comments of Go for the proto elements.
   */
  public static List<String> comments(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }
    return new GoContextCommon().getCommentLines(DocumentationUtil.getScopedDescription(element));
  }

  private List<StaticLangApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    for (Method method : context.getNonStreamingMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodTransformerContext methodContext = context.asMethodContext(method);

      if (methodConfig.isPageStreaming()) {
        apiMethods.add(apiMethodTransformer.generatePagedRequestObjectMethod(methodContext));
      } else {
        apiMethods.add(apiMethodTransformer.generateRequestObjectMethod(methodContext));
      }
    }

    return apiMethods;
  }

  /**
   * Returns the Go package name.
   *
   * Retrieved by splitting the package string on `/` and returning the second to last string if
   * possible, and otherwise the last string in the resulting array.
   *
   * TODO(saicheems): Figure out how to reliably get the intended value...
   */
  private static String getPackageName(String configPackageName) {
    String split[] = configPackageName.split("/");
    if (split.length - 2 >= 0) {
      return split[split.length - 2];
    }
    return split[split.length - 1];
  }

  /**
   * Returns the service's client name prefix.
   */
  private String getClientPrefix(Model model, Interface service, String packageName) {
    String name = getReducedServiceName(service);
    // If there's only one service, or the service name matches the package name, don't prefix with
    // the service name.
    if (model.getSymbolTable().getInterfaces().size() == 1 || name.equals(packageName)) {
      return "";
    }
    return LanguageUtil.lowerUnderscoreToUpperCamel(name);
  }

  /**
   * Returns the service name with common suffixes removed.
   *
   * For example:
   *  LoggingServiceV2 => logging
   */
  private static String getReducedServiceName(Interface service) {
    String name = service.getSimpleName().replaceAll("V[0-9]+$", "");
    name = name.replaceAll("Service$", "");
    return LanguageUtil.upperCamelToLowerUnderscore(name);
  }

  private List<RetryCodesDefinitionView> generateRetryCodesDefinitions(
      SurfaceTransformerContext context) {
    List<RetryCodesDefinitionView> definitions = new ArrayList<>();

    for (Entry<String, ImmutableSet<Code>> retryCodesDef :
        context.getInterfaceConfig().getRetryCodesDefinition().entrySet()) {
      if (!retryCodesDef.getValue().isEmpty()) {
        definitions.add(
            RetryCodesDefinitionView.newBuilder()
                .key(formatRetryDefName(retryCodesDef.getKey()))
                .codes(retryCodesDef.getValue())
                .build());
      }
    }
    return definitions;
  }

  private List<RetryParamsDefinitionView> generateRetryParamsDefinitions(
      SurfaceTransformerContext context) {
    List<RetryParamsDefinitionView> definitions = new ArrayList<>();

    for (Entry<String, RetrySettings> retryCodesDef :
        context.getInterfaceConfig().getRetrySettingsDefinition().entrySet()) {
      RetrySettings settings = retryCodesDef.getValue();
      RetryParamsDefinitionView.Builder params = RetryParamsDefinitionView.newBuilder();
      params.key(retryCodesDef.getKey());
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

  private String formatRetryDefName(String name) {
    return String.format("with%sRetryCodes", LanguageUtil.lowerCamelToUpperCamel(name));
  }
}
