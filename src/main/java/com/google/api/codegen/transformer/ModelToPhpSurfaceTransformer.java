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
package com.google.api.codegen.transformer;

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.surface.SurfaceApiMethod;
import com.google.api.codegen.surface.SurfaceDoc;
import com.google.api.codegen.surface.SurfaceDynamicXApi;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;

import java.util.ArrayList;
import java.util.List;

public class ModelToPhpSurfaceTransformer implements ModelToSurfaceTransformer {
  private ApiConfig cachedApiConfig;
  private GapicCodePathMapper pathMapper;
  private PathTemplateTransformer pathTemplateTransformer;
  private PageStreamingTransformer pageStreamingTransformer;
  private ApiMethodTransformer apiMethodTransformer;

  public ModelToPhpSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.cachedApiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();

    fileNames.add(new SurfaceDynamicXApi().getTemplateFileName());

    return fileNames;
  }

  @Override
  public List<SurfaceDoc> transform(Model model) {
    List<SurfaceDoc> surfaceDocs = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      surfaceDocs.addAll(transform(service));
    }
    return surfaceDocs;
  }

  public List<SurfaceDoc> transform(Interface service) {
    ModelToSurfaceContext context =
        ModelToSurfaceContext.create(
            service, cachedApiConfig, new ModelToPhpTypeTable(), new PhpIdentifierNamer());

    String outputPath = pathMapper.getOutputPath(service, context.getApiConfig());
    IdentifierNamer namer = context.getNamer();

    List<SurfaceDoc> surfaceData = new ArrayList<>();

    addXApiImports(context);

    SurfaceDynamicXApi xapiClass = new SurfaceDynamicXApi();
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

  private void addXApiImports(ModelToSurfaceContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.saveNicknameFor("Google\\GAX\\AgentHeaderDescriptor");
    typeTable.saveNicknameFor("Google\\GAX\\ApiCallable");
    typeTable.saveNicknameFor("Google\\GAX\\CallSettings");
    typeTable.saveNicknameFor("Google\\GAX\\GrpcBootstrap");
    typeTable.saveNicknameFor("Google\\GAX\\GrpcConstants");
    typeTable.saveNicknameFor("Google\\GAX\\PathTemplate");
  }

  private List<String> generateMethodKeys(ModelToSurfaceContext context) {
    List<String> methodKeys = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      methodKeys.add(context.getNamer().getMethodKey(method));
    }

    return methodKeys;
  }

  private List<SurfaceApiMethod> generateApiMethods(ModelToSurfaceContext context) {
    List<SurfaceApiMethod> apiMethods = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);

      apiMethods.add(
          apiMethodTransformer.generateOptionalArrayMethod(context, method, methodConfig));
    }

    return apiMethods;
  }
}
