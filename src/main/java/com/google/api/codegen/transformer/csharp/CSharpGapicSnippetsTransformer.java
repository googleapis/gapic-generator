/* Copyright 2016 Google LLC
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

import com.google.api.codegen.config.ApiModel;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.SnippetsFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodSnippetView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class CSharpGapicSnippetsTransformer implements ModelToViewTransformer {

  private static final String SNIPPETS_TEMPLATE_FILENAME = "csharp/gapic_snippets.snip";

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final StaticLangApiMethodTransformer apiMethodTransformer =
      new CSharpApiMethodTransformer();
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();

  public CSharpGapicSnippetsTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<ViewModel> transform(ApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName());

    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              csharpCommonTransformer.createTypeTable(namer.getExamplePackageName()),
              namer,
              new CSharpFeatureConfig());
      csharpCommonTransformer.addCommonImports(context);
      context.getImportTypeTable().saveNicknameFor("Google.Protobuf.Bytestring");
      context.getImportTypeTable().saveNicknameFor("System.Linq.__import__");
      SnippetsFileView snippets = generateSnippets(context);
      surfaceDocs.add(snippets);
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(SNIPPETS_TEMPLATE_FILENAME);
  }

  private SnippetsFileView generateSnippets(GapicInterfaceContext context) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getApiSnippetsClassName(context.getInterfaceConfig());
    SnippetsFileView.Builder snippetsBuilder = SnippetsFileView.newBuilder();

    snippetsBuilder.templateFileName(SNIPPETS_TEMPLATE_FILENAME);
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    snippetsBuilder.outputPath(
        outputPath + File.separator + name.replace("Generated", "") + ".g.cs");
    snippetsBuilder.name(name);
    snippetsBuilder.snippetMethods(generateMethods(context));

    // must be done as the last step to catch all imports
    snippetsBuilder.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return snippetsBuilder.build();
  }

  private List<StaticLangApiMethodSnippetView> generateMethods(InterfaceContext context) {
    List<StaticLangApiMethodSnippetView> methods = new ArrayList<>();

    for (MethodModel method : csharpCommonTransformer.getSupportedMethods(context)) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodContext methodContext = context.asRequestMethodContext(method);
      if (methodConfig.isGrpcStreaming()) {
        methods.add(generateGrpcStreamingRequestMethod(methodContext));
      } else if (methodConfig.isLongRunningOperation()) {
        if (methodConfig.isFlattening()) {
          ImmutableList<FlatteningConfig> flatteningGroups = methodConfig.getFlatteningConfigs();
          boolean requiresNameSuffix = flatteningGroups.size() > 1;
          for (int i = 0; i < flatteningGroups.size(); i++) {
            FlatteningConfig flatteningGroup = flatteningGroups.get(i);
            String nameSuffix = requiresNameSuffix ? Integer.toString(i + 1) : "";
            MethodContext methodContextFlat =
                context.asFlattenedMethodContext(method, flatteningGroup);
            methods.add(generateOperationFlattenedAsyncMethod(methodContextFlat, nameSuffix));
            methods.add(generateOperationFlattenedMethod(methodContextFlat, nameSuffix));
          }
        }
        methods.add(generateOperationRequestAsyncMethod(methodContext));
        methods.add(generateOperationRequestMethod(methodContext));
      } else if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          ImmutableList<FlatteningConfig> flatteningGroups = methodConfig.getFlatteningConfigs();
          boolean requiresNameSuffix = flatteningGroups.size() > 1;
          for (int i = 0; i < flatteningGroups.size(); i++) {
            FlatteningConfig flatteningGroup = flatteningGroups.get(i);
            String nameSuffix = requiresNameSuffix ? Integer.toString(i + 1) : "";
            MethodContext methodContextFlat =
                context.asFlattenedMethodContext(method, flatteningGroup);
            methods.add(generatePagedFlattenedAsyncMethod(methodContextFlat, nameSuffix));
            methods.add(generatePagedFlattenedMethod(methodContextFlat, nameSuffix));
          }
        }
        methods.add(generatePagedRequestAsyncMethod(methodContext));
        methods.add(generatePagedRequestMethod(methodContext));
      } else {
        if (methodConfig.isFlattening()) {
          ImmutableList<FlatteningConfig> flatteningGroups = methodConfig.getFlatteningConfigs();
          boolean requiresNameSuffix = flatteningGroups.size() > 1;
          for (int i = 0; i < flatteningGroups.size(); i++) {
            FlatteningConfig flatteningGroup = flatteningGroups.get(i);
            String nameSuffix = requiresNameSuffix ? Integer.toString(i + 1) : "";
            MethodContext methodContextFlat =
                context.asFlattenedMethodContext(method, flatteningGroup);
            methods.add(generateFlattenedAsyncMethod(methodContextFlat, nameSuffix));
            methods.add(generateFlattenedMethod(methodContextFlat, nameSuffix));
          }
        }
        methods.add(generateRequestAsyncMethod(methodContext));
        methods.add(generateRequestMethod(methodContext));
      }
    }

    return methods;
  }

  private StaticLangApiMethodSnippetView generateGrpcStreamingRequestMethod(
      MethodContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateGrpcStreamingRequestObjectMethod(methodContext);
    String callerResponseTypeName = method.name() + "Stream";
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name())
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationFlattenedAsyncMethod(
      MethodContext methodContext, String suffix) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateAsyncOperationFlattenedMethod(
            methodContext,
            Collections.<ParamWithSimpleDoc>emptyList(),
            ClientMethodType.AsyncOperationFlattenedMethod,
            true);
    String callerResponseTypeName = method.operationMethod().clientReturnTypeName();
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationFlattenedMethod(
      MethodContext methodContext, String suffix) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateOperationFlattenedMethod(
            methodContext, Collections.<ParamWithSimpleDoc>emptyList());
    String callerResponseTypeName = method.operationMethod().clientReturnTypeName();
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationRequestAsyncMethod(
      MethodContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateAsyncOperationRequestObjectMethod(
            methodContext, Collections.<ParamWithSimpleDoc>emptyList(), true);
    String callerResponseTypeName = method.operationMethod().clientReturnTypeName();
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationRequestMethod(
      MethodContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateOperationRequestObjectMethod(methodContext);
    String callerResponseTypeName = method.operationMethod().clientReturnTypeName();
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedFlattenedAsyncMethod(
      MethodContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedFlattenedAsyncMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerAsyncPagedResponseTypeName(methodContext, resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedFlattenedMethod(
      MethodContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedFlattenedMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerPagedResponseTypeName(methodContext, resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedRequestAsyncMethod(
      MethodContext methodContext) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedRequestObjectAsyncMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerAsyncPagedResponseTypeName(methodContext, resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedRequestMethod(MethodContext methodContext) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedRequestObjectMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerPagedResponseTypeName(methodContext, resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateFlattenedAsyncMethod(
      MethodContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generateFlattenedAsyncMethod(
            methodContext, ClientMethodType.FlattenedAsyncCallSettingsMethod);
    SurfaceNamer namer = methodContext.getNamer();
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(namer.getStaticLangCallerAsyncReturnTypeName(methodContext));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateFlattenedMethod(
      MethodContext methodContext, String suffix) {
    StaticLangApiMethodView method = apiMethodTransformer.generateFlattenedMethod(methodContext);
    SurfaceNamer namer = methodContext.getNamer();
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(namer.getStaticLangCallerReturnTypeName(methodContext));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateRequestMethod(MethodContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateRequestObjectMethod(methodContext);
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(namer.getStaticLangCallerAsyncReturnTypeName(methodContext));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateRequestAsyncMethod(MethodContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateRequestObjectAsyncMethod(methodContext);
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(namer.getStaticLangCallerAsyncReturnTypeName(methodContext));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(namer.getApiWrapperClassName(methodContext.getInterfaceConfig()))
        .apiVariableName(method.apiVariableName())
        .build();
  }
}
