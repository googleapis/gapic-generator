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
package com.google.api.codegen.transformer.php;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.DynamicXApiView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ModelToPhpSurfaceTransformer implements ModelToViewTransformer {
  private ApiConfig cachedApiConfig;
  private GapicCodePathMapper pathMapper;
  private PathTemplateTransformer pathTemplateTransformer;
  private PageStreamingTransformer pageStreamingTransformer;
  private ApiMethodTransformer apiMethodTransformer;

  private static final String XAPI_TEMPLATE_FILENAME = "php/xapi.snip";

  public ModelToPhpSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.cachedApiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      surfaceDocs.addAll(transform(service));
    }
    return surfaceDocs;
  }

  public List<ViewModel> transform(Interface service) {
    SurfaceTransformerContext context =
        SurfaceTransformerContext.create(
            service, cachedApiConfig, new ModelToPhpTypeTable(), new PhpSurfaceNamer());

    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    SurfaceNamer namer = context.getNamer();

    List<ViewModel> surfaceData = new ArrayList<>();

    addXApiImports(context);

    DynamicXApiView xapiClass = new DynamicXApiView();
    xapiClass.templateFileName = XAPI_TEMPLATE_FILENAME;
    xapiClass.packageName = context.getApiConfig().getPackageName();
    xapiClass.name = namer.getApiWrapperClassName(context.getInterface());
    ServiceConfig serviceConfig = new ServiceConfig();
    xapiClass.serviceAddress = serviceConfig.getServiceAddress(service);
    xapiClass.servicePort = serviceConfig.getServicePort();
    xapiClass.serviceTitle = serviceConfig.getTitle(service);
    xapiClass.authScopes = serviceConfig.getAuthScopes(service);

    xapiClass.pathTemplates = pathTemplateTransformer.generatePathTemplates(context);
    xapiClass.formatResourceFunctions =
        pathTemplateTransformer.generateFormatResourceFunctions(context);
    xapiClass.parseResourceFunctions =
        pathTemplateTransformer.generateParseResourceFunctions(context);
    xapiClass.pathTemplateGetterFunctions =
        pathTemplateTransformer.generatePathTemplateGetterFunctions(context);
    xapiClass.pageStreamingDescriptors = pageStreamingTransformer.generateDescriptors(context);

    xapiClass.methodKeys = generateMethodKeys(context);
    xapiClass.clientConfigPath = namer.getClientConfigPath(service);
    xapiClass.interfaceKey = service.getFullName();
    String grpcClientTypeName = namer.getGrpcClientTypeName(context.getInterface());
    xapiClass.grpcClientTypeName = context.getTypeTable().getAndSaveNicknameFor(grpcClientTypeName);

    xapiClass.apiMethods = generateApiMethods(context);

    // must be done as the last step to catch all imports
    xapiClass.imports = context.getTypeTable().getImports();

    xapiClass.outputPath = outputPath + "/" + xapiClass.name + ".php";

    surfaceData.add(xapiClass);

    return surfaceData;
  }

  private void addXApiImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("Google\\GAX\\AgentHeaderDescriptor");
    typeTable.saveNicknameFor("Google\\GAX\\ApiCallable");
    typeTable.saveNicknameFor("Google\\GAX\\CallSettings");
    typeTable.saveNicknameFor("Google\\GAX\\GrpcBootstrap");
    typeTable.saveNicknameFor("Google\\GAX\\GrpcConstants");
    typeTable.saveNicknameFor("Google\\GAX\\PathTemplate");
  }

  private List<String> generateMethodKeys(SurfaceTransformerContext context) {
    List<String> methodKeys = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      methodKeys.add(context.getNamer().getMethodKey(method));
    }

    return methodKeys;
  }

  private List<ApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    List<ApiMethodView> apiMethods = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      apiMethods.add(
          apiMethodTransformer.generateOptionalArrayMethod(context.asMethodContext(method)));
    }

    return apiMethods;
  }
}
