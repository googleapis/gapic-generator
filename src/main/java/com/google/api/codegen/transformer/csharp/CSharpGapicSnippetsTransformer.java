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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.config.ApiConfig;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.StandardImportTypeTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.SnippetsFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodSnippetView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
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
      new FileHeaderTransformer(new StandardImportTypeTransformer());
  private final ApiMethodTransformer apiMethodTransformer = new CSharpApiMethodTransformer();
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();

  public CSharpGapicSnippetsTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<ViewModel> transform(Model model, ApiConfig apiConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(apiConfig.getPackageName());

    for (Interface service : new InterfaceView().getElementIterable(model)) {
      SurfaceTransformerContext context =
          SurfaceTransformerContext.create(
              service,
              apiConfig,
              createTypeTable(namer.getExamplePackageName()),
              namer,
              new CSharpFeatureConfig());
      csharpCommonTransformer.addCommonImports(context);
      context.getTypeTable().saveNicknameFor("Google.Protobuf.Bytestring");
      context.getTypeTable().saveNicknameFor("System.Linq.__import__");
      SnippetsFileView snippets = generateSnippets(context);
      surfaceDocs.add(snippets);
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(SNIPPETS_TEMPLATE_FILENAME);
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new CSharpTypeTable(implicitPackageName),
        new CSharpModelTypeNameConverter(implicitPackageName));
  }

  private SnippetsFileView generateSnippets(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getApiSnippetsClassName(context.getInterface());
    SnippetsFileView.Builder snippetsBuilder = SnippetsFileView.newBuilder();

    snippetsBuilder.templateFileName(SNIPPETS_TEMPLATE_FILENAME);
    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    snippetsBuilder.outputPath(
        outputPath + File.separator + name.replace("Generated", "") + ".g.cs");
    snippetsBuilder.name(name);
    snippetsBuilder.snippetMethods(generateMethods(context));

    // must be done as the last step to catch all imports
    snippetsBuilder.fileHeader(fileHeaderTransformer.generateFileHeader(context));

    return snippetsBuilder.build();
  }

  private List<StaticLangApiMethodSnippetView> generateMethods(SurfaceTransformerContext context) {
    List<StaticLangApiMethodSnippetView> methods = new ArrayList<>();

    for (Method method : csharpCommonTransformer.getSupportedMethods(context)) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      MethodTransformerContext methodContext = context.asRequestMethodContext(method);
      if (methodConfig.isGrpcStreaming()) {
        methods.add(generateGrpcStreamingRequestMethod(methodContext));
      } else if (methodConfig.isLongRunningOperation()) {
        if (methodConfig.isFlattening()) {
          ImmutableList<FlatteningConfig> flatteningGroups = methodConfig.getFlatteningConfigs();
          boolean requiresNameSuffix = flatteningGroups.size() > 1;
          for (int i = 0; i < flatteningGroups.size(); i++) {
            FlatteningConfig flatteningGroup = flatteningGroups.get(i);
            String nameSuffix = requiresNameSuffix ? Integer.toString(i + 1) : "";
            MethodTransformerContext methodContextFlat =
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
            MethodTransformerContext methodContextFlat =
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
            MethodTransformerContext methodContextFlat =
                context.asFlattenedMethodContext(method, flatteningGroup);
            methods.add(generateFlattenedAsyncMethod(methodContextFlat, nameSuffix));
            methods.add(generateFlattenedMethod(methodContextFlat, nameSuffix));
          }
          methods.add(generateRequestAsyncMethod(methodContext));
          methods.add(generateRequestMethod(methodContext));
        }
      }
    }

    return methods;
  }

  private StaticLangApiMethodSnippetView generateGrpcStreamingRequestMethod(
      MethodTransformerContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateGrpcStreamingRequestObjectMethod(methodContext);
    String callerResponseTypeName = method.name() + "Stream";
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name())
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationFlattenedAsyncMethod(
      MethodTransformerContext methodContext, String suffix) {
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
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationFlattenedMethod(
      MethodTransformerContext methodContext, String suffix) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateOperationFlattenedMethod(
            methodContext, Collections.<ParamWithSimpleDoc>emptyList());
    String callerResponseTypeName = method.operationMethod().clientReturnTypeName();
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationRequestAsyncMethod(
      MethodTransformerContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateAsyncOperationRequestObjectMethod(
            methodContext, Collections.<ParamWithSimpleDoc>emptyList(), true);
    String callerResponseTypeName = method.operationMethod().clientReturnTypeName();
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateOperationRequestMethod(
      MethodTransformerContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateOperationRequestObjectMethod(methodContext);
    String callerResponseTypeName = method.operationMethod().clientReturnTypeName();
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedFlattenedAsyncMethod(
      MethodTransformerContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedFlattenedAsyncMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerAsyncPagedResponseTypeName(
            methodContext.getMethod(), methodContext.getTypeTable(), resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedFlattenedMethod(
      MethodTransformerContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedFlattenedMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerPagedResponseTypeName(
            methodContext.getMethod(), methodContext.getTypeTable(), resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedRequestAsyncMethod(
      MethodTransformerContext methodContext) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedRequestObjectAsyncMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerAsyncPagedResponseTypeName(
            methodContext.getMethod(), methodContext.getTypeTable(), resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedRequestMethod(
      MethodTransformerContext methodContext) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedRequestObjectMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    FieldConfig resourceFieldConfig = pageStreaming.getResourcesFieldConfig();
    String callerResponseTypeName =
        namer.getAndSaveCallerPagedResponseTypeName(
            methodContext.getMethod(), methodContext.getTypeTable(), resourceFieldConfig);
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateFlattenedAsyncMethod(
      MethodTransformerContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generateFlattenedAsyncMethod(
            methodContext, ClientMethodType.FlattenedAsyncCallSettingsMethod);
    SurfaceNamer namer = methodContext.getNamer();
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(
                namer.getStaticLangCallerAsyncReturnTypeName(
                    methodContext.getMethod(), methodContext.getMethodConfig()));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateFlattenedMethod(
      MethodTransformerContext methodContext, String suffix) {
    StaticLangApiMethodView method = apiMethodTransformer.generateFlattenedMethod(methodContext);
    SurfaceNamer namer = methodContext.getNamer();
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(
                namer.getStaticLangCallerReturnTypeName(
                    methodContext.getMethod(), methodContext.getMethodConfig()));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + suffix)
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateRequestMethod(
      MethodTransformerContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateRequestObjectMethod(methodContext);
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(
                namer.getStaticLangCallerAsyncReturnTypeName(
                    methodContext.getMethod(), methodContext.getMethodConfig()));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }

  private StaticLangApiMethodSnippetView generateRequestAsyncMethod(
      MethodTransformerContext methodContext) {
    SurfaceNamer namer = methodContext.getNamer();
    StaticLangApiMethodView method =
        apiMethodTransformer.generateRequestObjectAsyncMethod(methodContext);
    String callerResponseTypeName =
        methodContext
            .getTypeTable()
            .getAndSaveNicknameFor(
                namer.getStaticLangCallerAsyncReturnTypeName(
                    methodContext.getMethod(), methodContext.getMethodConfig()));
    return StaticLangApiMethodSnippetView.newBuilder()
        .method(method)
        .snippetMethodName(method.name() + "_RequestObject")
        .callerResponseTypeName(callerResponseTypeName)
        .apiClassName(
            namer.getApiWrapperClassName(
                methodContext.getSurfaceTransformerContext().getInterface()))
        .apiVariableName(method.apiVariableName())
        .build();
  }
}
