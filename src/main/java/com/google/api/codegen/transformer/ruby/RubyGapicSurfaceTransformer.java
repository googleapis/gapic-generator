/* Copyright 2017 Google Inc
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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.ServiceConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.BundlingTransformer;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GrpcStubTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.PageStreamingTransformer;
import com.google.api.codegen.transformer.PathTemplateTransformer;
import com.google.api.codegen.transformer.ServiceTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodView;
import com.google.api.codegen.viewmodel.DynamicLangXApiView;
import com.google.api.codegen.viewmodel.GrpcStreamingDetailView;
import com.google.api.codegen.viewmodel.LongRunningOperationDetailView;
import com.google.api.codegen.viewmodel.PathTemplateGetterFunctionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

/** The ModelToViewTransformer to transform a Model into the standard GAPIC surface in Ruby. */
public class RubyGapicSurfaceTransformer implements ModelToViewTransformer {
  private static final String XAPI_TEMPLATE_FILENAME = "ruby/main.snip";

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new RubyImportSectionTransformer());
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(new RubyParamDocTransformer());
  private final ServiceTransformer serviceTransformer = new ServiceTransformer();
  private final GrpcStubTransformer grpcStubTransformer = new GrpcStubTransformer();
  private final PageStreamingTransformer pageStreamingTransformer = new PageStreamingTransformer();
  private final BundlingTransformer bundlingTransformer = new BundlingTransformer();
  private final PathTemplateTransformer pathTemplateTransformer = new PathTemplateTransformer();

  public RubyGapicSurfaceTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(XAPI_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(apiConfig.getPackageName());
    FeatureConfig featureConfig = new RubyFeatureConfig();
    List<ViewModel> serviceSurfaces = new ArrayList<>();
    for (Interface service : new InterfaceView().getElementIterable(model)) {
      ModelTypeTable modelTypeTable =
          new ModelTypeTable(
              new RubyTypeTable(apiConfig.getPackageName()),
              new RubyModelTypeNameConverter(apiConfig.getPackageName()));
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service, apiConfig, modelTypeTable, namer, featureConfig);
      serviceSurfaces.add(generateApiClass(context));
    }
    return serviceSurfaces;
  }

  private ViewModel generateApiClass(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    String subPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    String serviceFilename = namer.getServiceFileName(context.getInterface());
    List<ApiMethodView> methods = generateApiMethods(context);

    DynamicLangXApiView.Builder xapiClass = DynamicLangXApiView.newBuilder();

    xapiClass.templateFileName(XAPI_TEMPLATE_FILENAME);
    xapiClass.outputPath(
        subPath + serviceFilename.substring(serviceFilename.lastIndexOf("/")) + ".rb");

    xapiClass.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    xapiClass.protoFilename(context.getInterface().getFile().getSimpleName());

    xapiClass.name(namer.getApiWrapperClassName(context.getInterface()));
    xapiClass.doc(serviceTransformer.generateServiceDoc(context, methods.get(0)));
    xapiClass.stubs(grpcStubTransformer.generateGrpcStubs(context));

    ServiceConfig serviceConfig = new ServiceConfig();
    xapiClass.serviceAddress(serviceConfig.getServiceAddress(context.getInterface()));
    xapiClass.servicePort(serviceConfig.getServicePort());
    xapiClass.serviceTitle(serviceConfig.getTitle(context.getInterface()));
    xapiClass.authScopes(serviceConfig.getAuthScopes(context.getInterface()));
    xapiClass.hasDefaultServiceAddress(context.getInterfaceConfig().hasDefaultServiceAddress());
    xapiClass.hasDefaultServiceScopes(context.getInterfaceConfig().hasDefaultServiceScopes());

    xapiClass.pageStreamingDescriptors(pageStreamingTransformer.generateDescriptors(context));
    xapiClass.bundlingDescriptors(bundlingTransformer.generateDescriptors(context));
    xapiClass.longRunningDescriptors(ImmutableList.<LongRunningOperationDetailView>of());
    xapiClass.grpcStreamingDescriptors(ImmutableList.<GrpcStreamingDetailView>of());
    xapiClass.hasPageStreamingMethods(context.getInterfaceConfig().hasPageStreamingMethods());
    xapiClass.hasBundlingMethods(context.getInterfaceConfig().hasBundlingMethods());
    xapiClass.hasLongRunningOperations(context.getInterfaceConfig().hasLongRunningOperations());

    xapiClass.pathTemplates(pathTemplateTransformer.generatePathTemplates(context));
    xapiClass.formatResourceFunctions(
        pathTemplateTransformer.generateFormatResourceFunctions(context));
    xapiClass.parseResourceFunctions(
        pathTemplateTransformer.generateParseResourceFunctions(context));
    xapiClass.pathTemplateGetterFunctions(ImmutableList.<PathTemplateGetterFunctionView>of());

    xapiClass.methodKeys(ImmutableList.<String>of());
    xapiClass.interfaceKey(context.getInterface().getFullName());
    xapiClass.clientConfigPath(namer.getClientConfigPath(context.getInterface()));
    xapiClass.grpcClientTypeName(
        namer.getAndSaveNicknameForGrpcClientTypeName(
            context.getTypeTable(), context.getInterface()));

    xapiClass.apiMethods(methods);

    return xapiClass.build();
  }

  private List<ApiMethodView> generateApiMethods(SurfaceTransformerContext context) {
    ImmutableList.Builder<ApiMethodView> apiMethods = ImmutableList.builder();

    for (Method method : context.getSupportedMethods()) {
      apiMethods.add(
          apiMethodTransformer.generateApiMethod(context.asDynamicMethodContext(method)));
    }

    return apiMethods.build();
  }
}
