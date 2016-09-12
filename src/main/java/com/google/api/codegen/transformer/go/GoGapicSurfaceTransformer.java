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
import com.google.api.codegen.go.GoContextCommon;
import com.google.api.codegen.go.GoImport;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.transformer.ApiCallableTransformer;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.viewmodel.RetryPairDefinitionView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangXCombinedView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.gax.core.RetrySettings;
import com.google.api.tools.framework.aspects.documentation.model.DocumentationUtil;
import com.google.api.tools.framework.aspects.documentation.model.ElementDocumentationAttribute;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import io.grpc.Status.Code;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class GoGapicSurfaceTransformer implements ModelToViewTransformer {

  private static final String XAPI_TEMPLATE_FILENAME = "go/main.snip";
  private static final String SAMPLE_TEMPLATE_FILENAME = "go/example.snip";
  private static final String DOC_TEMPLATE_FILENAME = "go/doc.snip";

  private final ApiCallableTransformer apiCallableTransformer = new ApiCallableTransformer();
  private final ApiMethodTransformer apiMethodTransformer = new ApiMethodTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();

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

    view.templateFileName(XAPI_TEMPLATE_FILENAME);
    view.serviceDoc(comments(service));

    GoSurfaceNamer namer = new GoSurfaceNamer(model, service, apiConfig);
    SurfaceTransformerContext context =
        SurfaceTransformerContext.create(service, apiConfig, createTypeTable(), namer);

    view.packageName(namer.getPackageName());
    view.clientName(namer.getClientName());
    view.clientConstructorName(namer.getClientConstructorName());
    view.defaultClientOptionFunc(namer.getDefaultClientOptionFunc());
    view.defaultCallOptionFunc(namer.getDefaultCallOptionFunc());
    view.callOptionsName(namer.getCallOptionsName());
    view.serviceName(namer.getServiceName());
    view.grpcClientTypeName(namer.getGrpcClientTypeName());
    view.grpcClientConstructorName(namer.getGrpcClientConstructorName());
    view.outputPath(namer.getOutputPath());

    Collection<RetryPairDefinitionView> retryDef = generateRetryPairDefinitions(namer, context);
    boolean hasRetry = !retryDef.isEmpty();
    view.retryPairDefinitions(retryDef);

    view.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    view.pathTemplateGetters(pathTemplateTransformer.generatePathTemplateGetterFunctions(context));
    view.callSettings(apiCallableTransformer.generateCallSettings(context));
    view.apiMethods(generateApiMethods(context));
    view.pageStreamingDescriptorClasses(
        pageStreamingTransformer.generateDescriptorClasses(context));

    ServiceConfig serviceConfig = new ServiceConfig();
    view.serviceAddress(serviceConfig.getServiceAddress(service));
    view.servicePort(serviceConfig.getServicePort());
    view.authScopes(serviceConfig.getAuthScopes(service));

    view.imports(getImports(context, hasRetry));

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

  private Collection<RetryPairDefinitionView> generateRetryPairDefinitions(
      GoSurfaceNamer namer, SurfaceTransformerContext context) {
    Set<RetryPairDefinitionView.Name> retryNames = new HashSet<>();
    for (Method method : context.getNonStreamingMethods()) {
      MethodConfig conf = context.getMethodConfig(method);
      retryNames.add(
          RetryPairDefinitionView.Name.create(
              conf.getRetrySettingsConfigName(), conf.getRetryCodesConfigName()));
    }

    TreeMap<RetryPairDefinitionView.Name, RetryPairDefinitionView> retryDef = new TreeMap<>();
    Map<String, ImmutableSet<Code>> retryCodesDef =
        context.getInterfaceConfig().getRetryCodesDefinition();
    Map<String, RetrySettings> retryParamsDef =
        context.getInterfaceConfig().getRetrySettingsDefinition();
    for (RetryPairDefinitionView.Name name : retryNames) {
      ImmutableSet<Code> codes = retryCodesDef.get(name.codes());
      if (codes.isEmpty()) {
        continue;
      }
      ImmutableSet.Builder<String> builder = ImmutableSet.<String>builder();
      for (Code code : codes) {
        builder.add(namer.getStatusCodeName(code));
      }
      retryDef.put(
          name,
          RetryPairDefinitionView.newBuilder()
              .name(name)
              .retryCodes(builder.build())
              .params(retryParamsDef.get(name.params()))
              .build());
    }
    return retryDef.values();
  }

  private Set<RetryPairDefinitionView.Name> getRetryNames(SurfaceTransformerContext context) {
    Set<RetryPairDefinitionView.Name> retryNames = new HashSet<>();
    for (Method method : context.getNonStreamingMethods()) {
      MethodConfig conf = context.getMethodConfig(method);
      retryNames.add(
          RetryPairDefinitionView.Name.create(
              conf.getRetrySettingsConfigName(), conf.getRetryCodesConfigName()));
    }
    return retryNames;
  }

  private List<String> getImports(SurfaceTransformerContext context, boolean hasRetry) {
    TreeSet<GoImport> standard = new TreeSet<>();
    TreeSet<GoImport> thirdParty = new TreeSet<>();

    standard.add(GoImport.create("fmt"));
    standard.add(GoImport.create("runtime"));
    if (hasRetry) {
      standard.add(GoImport.create("time"));
      thirdParty.add(GoImport.create("google.golang.org/grpc/codes"));
    }
    for (Method method : context.getNonStreamingMethods()) {
      if (context.getMethodConfig(method).isPageStreaming()) {
        standard.add(GoImport.create("math"));
        break;
      }
    }
    thirdParty.add(GoImport.create("golang.org/x/net/context"));
    thirdParty.add(GoImport.create("google.golang.org/grpc"));
    thirdParty.add(GoImport.create("google.golang.org/grpc/metadata"));
    thirdParty.add(GoImport.create("github.com/googleapis/gax-go", "gax"));
    thirdParty.add(GoImport.create("google.golang.org/api/option"));
    thirdParty.add(GoImport.create("google.golang.org/api/transport"));

    for (Map.Entry<String, String> entry : context.getTypeTable().getImports().entrySet()) {
      thirdParty.add(GoImport.create(entry.getKey(), entry.getValue()));
    }

    return formatImports(standard, thirdParty);
  }

  private List<String> formatImports(Set<GoImport> standard, Set<GoImport> thirdParty) {
    List<String> result = new ArrayList<String>();
    if (standard != null) {
      for (GoImport goImport : standard) {
        result.add("    " + goImport.importString());
      }
      result.add("");
    }
    if (thirdParty != null) {
      for (GoImport goImport : thirdParty) {
        result.add("    " + goImport.importString());
      }
    }
    return result;
  }
}
