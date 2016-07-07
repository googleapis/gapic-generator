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

import java.util.ArrayList;
import java.util.List;

public class ModelToPhpSurfaceTransformer implements ModelToSurfaceTransformer {
  private Interface service;
  private ApiConfig apiConfig;
  private ModelToPhpTypeTable typeTable;
  private IdentifierNamer namer;
  private GapicCodePathMapper pathMapper;

  public ModelToPhpSurfaceTransformer(ApiConfig apiConfig, GapicCodePathMapper pathMapper) {
    this.apiConfig = apiConfig;
    this.pathMapper = pathMapper;
    this.namer = new PhpIdentifierNamer();
  }

  @Override
  public List<String> getTemplateFileNames() {
    List<String> fileNames = new ArrayList<>();

    fileNames.add(new SurfaceDynamicXApi().getTemplateFileName());

    return fileNames;
  }

  public List<SurfaceDoc> transform(Interface service) {
    // FIXME pass around service instead of mutating state
    this.service = service;

    String outputPath = pathMapper.getOutputPath(service, apiConfig);

    List<SurfaceDoc> surfaceData = new ArrayList<>();

    typeTable = new ModelToPhpTypeTable();
    addXApiImports();

    SurfaceDynamicXApi xapiClass = new SurfaceDynamicXApi();
    xapiClass.packageName = apiConfig.getPackageName();
    xapiClass.name = getApiWrapperClassName();
    ServiceConfig serviceConfig = new ServiceConfig();
    xapiClass.serviceAddress = serviceConfig.getServiceAddress(service);
    xapiClass.servicePort = serviceConfig.getServicePort();

    // must be done as the last step to catch all imports
    xapiClass.imports = typeTable.getImports();

    xapiClass.outputPath = outputPath + "/" + xapiClass.name + ".php";

    surfaceData.add(xapiClass);

    return surfaceData;
  }

  private void addXApiImports() {
    typeTable.addImport("Google\\GAX\\AgentHeaderDescriptor");
    typeTable.addImport("Google\\GAX\\ApiCallable");
    typeTable.addImport("Google\\GAX\\CallSettings");
    typeTable.addImport("Google\\GAX\\GrpcBootstrap");
    typeTable.addImport("Google\\GAX\\GrpcConstants");
    typeTable.addImport("Google\\GAX\\PathTemplate");
  }

  private String getApiWrapperClassName() {
    return service.getSimpleName() + "Api";
  }
}
