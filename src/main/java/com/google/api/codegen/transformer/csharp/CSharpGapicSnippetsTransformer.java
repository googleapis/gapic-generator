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
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.StandardImportTypeTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.SnippetsFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodSnippetView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.Model;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSharpGapicSnippetsTransformer implements ModelToViewTransformer {

  private static final String SNIPPETS_TEMPLATE_FILENAME = "csharp/gapic_snippets.snip";

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportTypeTransformer());
  private final ApiMethodTransformer apiMethodTransformer = new ApiMethodTransformer();
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
              createTypeTable(apiConfig.getPackageName()),
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
    boolean mixinsDisabled = !context.getFeatureConfig().enableMixins();
    List<StaticLangApiMethodSnippetView> methods = new ArrayList<>();

    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (mixinsDisabled && methodConfig.getRerouteToGrpcInterface() != null) {
        continue;
      }
      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          ImmutableList<FlatteningConfig> flatteningGroups = methodConfig.getFlatteningConfigs();
          boolean requiresNameSuffix = flatteningGroups.size() > 1;
          for (int i = 0; i < flatteningGroups.size(); i++) {
            FlatteningConfig flatteningGroup = flatteningGroups.get(i);
            String nameSuffix = requiresNameSuffix ? Integer.toString(i + 1) : "";
            MethodTransformerContext methodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            methods.add(generatePagedFlattenedAsyncMethod(methodContext, nameSuffix));
            methods.add(generatePagedFlattenedMethod(methodContext, nameSuffix));
          }
        }
      } else {
        if (methodConfig.isFlattening()) {
          ImmutableList<FlatteningConfig> flatteningGroups = methodConfig.getFlatteningConfigs();
          boolean requiresNameSuffix = flatteningGroups.size() > 1;
          for (int i = 0; i < flatteningGroups.size(); i++) {
            FlatteningConfig flatteningGroup = flatteningGroups.get(i);
            String nameSuffix = requiresNameSuffix ? Integer.toString(i + 1) : "";
            MethodTransformerContext methodContext =
                context.asFlattenedMethodContext(method, flatteningGroup);
            methods.add(generateFlattenedAsyncMethod(methodContext, nameSuffix));
            methods.add(generateFlattenedMethod(methodContext, nameSuffix));
          }
        }
      }
    }

    return methods;
  }

  private StaticLangApiMethodSnippetView generatePagedFlattenedAsyncMethod(
      MethodTransformerContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generatePagedFlattenedAsyncMethod(
            methodContext, csharpCommonTransformer.pagedMethodAdditionalParams());
    SurfaceNamer namer = methodContext.getNamer();
    PageStreamingConfig pageStreaming = methodContext.getMethodConfig().getPageStreaming();
    Field resourceField = pageStreaming.getResourcesField();
    String callerResponseTypeName =
        namer.getAndSaveCallerAsyncPagedResponseTypeName(
            methodContext.getMethod(), methodContext.getTypeTable(), resourceField);
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
    Field resourceField = pageStreaming.getResourcesField();
    String callerResponseTypeName =
        namer.getAndSaveCallerPagedResponseTypeName(
            methodContext.getMethod(), methodContext.getTypeTable(), resourceField);
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

  private StaticLangApiMethodSnippetView generateFlattenedAsyncMethod(
      MethodTransformerContext methodContext, String suffix) {
    StaticLangApiMethodView method =
        apiMethodTransformer.generateFlattenedAsyncMethod(
            methodContext, ApiMethodType.FlattenedAsyncCallSettingsMethod);
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
}
