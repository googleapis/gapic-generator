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
import com.google.api.codegen.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.surface.SurfaceDoc;
import com.google.api.codegen.surface.SurfaceDynamicXApi;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;

import java.util.ArrayList;
import java.util.List;

public class ModelToPhpSurfaceTransformer implements ModelToSurfaceTransformer {
  private ApiConfig cachedApiConfig;
  private GapicCodePathMapper pathMapper;
  private PathTemplateTransformer pathTemplateTransformer;
  private PageStreamingTransformer pageStreamingTransformer;

  public ModelToPhpSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.cachedApiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.pathTemplateTransformer = new PathTemplateTransformer();
    this.pageStreamingTransformer = new PageStreamingTransformer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();

    fileNames.add(new SurfaceDynamicXApi().getTemplateFileName());

    return fileNames;
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
    xapiClass.grpcClientTypeName =
        context.getTypeTable().importAndGetShortestName(grpcClientTypeName);

    // must be done as the last step to catch all imports
    xapiClass.imports = context.getTypeTable().getImports();

    xapiClass.outputPath = outputPath + "/" + xapiClass.name + ".php";

    surfaceData.add(xapiClass);

    return surfaceData;
  }

  private List<String> generateMethodKeys(ModelToSurfaceContext context) {
    List<String> methodKeys = new ArrayList<>();

    for (Method method : context.getInterface().getMethods()) {
      methodKeys.add(context.getNamer().getMethodKey(method));
    }

    return methodKeys;
  }

  private void addXApiImports(ModelToSurfaceContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    typeTable.addImport("Google\\GAX\\AgentHeaderDescriptor");
    typeTable.addImport("Google\\GAX\\ApiCallable");
    typeTable.addImport("Google\\GAX\\CallSettings");
    typeTable.addImport("Google\\GAX\\GrpcBootstrap");
    typeTable.addImport("Google\\GAX\\GrpcConstants");
    typeTable.addImport("Google\\GAX\\PathTemplate");
  }
}
