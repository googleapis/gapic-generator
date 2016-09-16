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
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.go.GoTypeTable;
import com.google.api.codegen.viewmodel.RetryConfigDefinitionView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangXCombinedSurfaceView;
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
    GoSurfaceNamer namer = new GoSurfaceNamer(model, apiConfig.getPackageName());
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(service, apiConfig, createTypeTable(), namer);
      models.add(generate(context));
    }
    return models;
  }

  private StaticLangXCombinedSurfaceView generate(SurfaceTransformerContext context) {
    StaticLangXCombinedSurfaceView.Builder view = StaticLangXCombinedSurfaceView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    Model model = context.getModel();
    Interface service = context.getInterface();
    ApiConfig apiConfig = context.getApiConfig();

    view.templateFileName(XAPI_TEMPLATE_FILENAME);
    view.serviceDoc(doc(service));
    view.packageName(namer.getPackageName());
    view.clientName(namer.getApiWrapperClassName(service));
    view.clientConstructorName(namer.getApiWrapperClassConstructorName(service));
    view.defaultClientOptionFunctionName(namer.getDefaultApiSettingsFunctionName(service));
    view.defaultCallOptionFunctionName(namer.getDefaultCallSettingsFunctionName(service));
    view.callOptionsTypeName(namer.getCallSettingsTypeName(service));
    view.serviceName(namer.getServicePhraseName(service));
    view.grpcClientTypeName(namer.getGrpcClientTypeName(service));
    view.grpcClientConstructorName(namer.getGrpcClientConstructorName(service));
    view.outputPath(GoSurfaceNamer.getOutputPath(service, apiConfig));

    List<RetryConfigDefinitionView> retryDef = generateRetryConfigDefinitions(namer, context);
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

    addXApiImports(context, hasRetry);
    view.imports(GoTypeTable.formatImports(context.getTypeTable().getImports()));
    // view.imports(getImports(context, hasRetry));

    return view.build();
  }

  private ModelTypeTable createTypeTable() {
    return new ModelTypeTable(new GoTypeTable(), new GoModelTypeNameConverter());
  }

  /**
   * Returns the doc comments of Go for the proto elements.
   */
  public static List<String> doc(ProtoElement element) {
    if (!element.hasAttribute(ElementDocumentationAttribute.KEY)) {
      return ImmutableList.<String>of("");
    }

    // TODO(pongad): GoContextCommon should be deleted after we convert Go discovery to MVMM.
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

  private List<RetryConfigDefinitionView> generateRetryConfigDefinitions(
      SurfaceNamer namer, SurfaceTransformerContext context) {
    Set<RetryConfigDefinitionView.Name> retryNames = new HashSet<>();
    for (Method method : context.getNonStreamingMethods()) {
      MethodConfig conf = context.getMethodConfig(method);
      retryNames.add(
          RetryConfigDefinitionView.Name.create(
              conf.getRetrySettingsConfigName(), conf.getRetryCodesConfigName()));
    }

    TreeMap<RetryConfigDefinitionView.Name, RetryConfigDefinitionView> retryDef = new TreeMap<>();
    Map<String, ImmutableSet<Code>> retryCodesDef =
        context.getInterfaceConfig().getRetryCodesDefinition();
    Map<String, RetrySettings> retryParamsDef =
        context.getInterfaceConfig().getRetrySettingsDefinition();
    for (RetryConfigDefinitionView.Name name : retryNames) {
      ImmutableSet<Code> codes = retryCodesDef.get(name.codes());
      if (codes.isEmpty()) {
        continue;
      }
      List<String> retryCodeNames = new ArrayList<>();
      for (Code code : codes) {
        retryCodeNames.add(namer.getStatusCodeName(code));
      }
      retryDef.put(
          name,
          RetryConfigDefinitionView.newBuilder()
              .name(name)
              .retryCodes(retryCodeNames)
              .params(retryParamsDef.get(name.params()))
              .build());
    }
    return new ArrayList<>(retryDef.values());
  }

  private Set<RetryConfigDefinitionView.Name> getRetryNames(SurfaceTransformerContext context) {
    Set<RetryConfigDefinitionView.Name> retryNames = new HashSet<>();
    for (Method method : context.getNonStreamingMethods()) {
      MethodConfig conf = context.getMethodConfig(method);
      retryNames.add(
          RetryConfigDefinitionView.Name.create(
              conf.getRetrySettingsConfigName(), conf.getRetryCodesConfigName()));
    }
    return retryNames;
  }

  private static final String EMPTY_PROTO_PKG = "github.com/golang/protobuf/ptypes/empty";

  private void addXApiImports(SurfaceTransformerContext context, boolean hasRetry) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("fmt;;;");
    typeTable.saveNicknameFor("runtime;;;");
    typeTable.saveNicknameFor("golang.org/x/net/context;;;");
    typeTable.saveNicknameFor("google.golang.org/grpc;;;");
    typeTable.saveNicknameFor("google.golang.org/grpc/metadata;;;");
    typeTable.saveNicknameFor("github.com/googleapis/gax-go;gax;;");
    typeTable.saveNicknameFor("google.golang.org/api/option;;;");
    typeTable.saveNicknameFor("google.golang.org/api/transport;;;");

    if (hasRetry) {
      typeTable.saveNicknameFor("time;;;");
      typeTable.saveNicknameFor("google.golang.org/grpc/codes;;;");
    }
    for (Method method : context.getNonStreamingMethods()) {
      if (context.getMethodConfig(method).isPageStreaming()) {
        typeTable.saveNicknameFor("math;;;");
        break;
      }
    }
    typeTable.getImports().remove(EMPTY_PROTO_PKG);
  }
}
