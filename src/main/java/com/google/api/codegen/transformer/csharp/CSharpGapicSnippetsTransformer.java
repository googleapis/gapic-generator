/* Copyright 2016 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PageStreamingConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.SnippetsFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodSnippetView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/* Transforms a ProtoApiModel into the standalone C# code snippets of an API. */
public class CSharpGapicSnippetsTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String SNIPPETS_TEMPLATE_FILENAME = "csharp/gapic_snippets.snip";

  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.MessagesOnly;

  private final GapicCodePathMapper pathMapper;
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final StaticLangApiMethodTransformer apiMethodTransformer =
      new CSharpApiMethodTransformer();
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();
  private final InitCodeTransformer initCodeTransformer = new InitCodeTransformer();
  private final SampleTransformer sampleTransformer =
      SampleTransformer.newBuilder().initCodeTransformer(initCodeTransformer).build();

  public CSharpGapicSnippetsTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName(), ALIAS_MODE);

    for (InterfaceModel apiInterface : model.getInterfaces(productConfig)) {
      if (!productConfig.hasInterfaceConfig(apiInterface)) {
        continue;
      }

      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              csharpCommonTransformer.createTypeTable(namer.getExamplePackageName(), ALIAS_MODE),
              namer,
              new CSharpFeatureConfig());
      csharpCommonTransformer.addCommonImports(context);
      context.getImportTypeTable().saveNicknameFor("Google.Protobuf.Bytestring");
      context.getImportTypeTable().saveNicknameFor("System.Linq.__import__");
      surfaceDocs.add(generateSnippets(context));
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
          // Find flattenings that have ambiguous parameters, and mark them to use named arguments.
          // Ambiguity occurs in a page-stream flattening that has one or two extra string
          // parameters (that are not resource-names) compared to any other flattening of this same
          // method.
          // Create a string for each flattening, encoding which parameters are strings and
          // not-strings. Each character in the string refers to a parameter. Each string refers
          // to a flattening.
          String[] stringParams =
              flatteningGroups
                  .stream()
                  .map(
                      flat ->
                          flat.getFlattenedFieldConfigs()
                              .values()
                              .stream()
                              .map(
                                  field ->
                                      field.getField().getType().isStringType()
                                              && field.getResourceNameConfig() == null
                                          ? 's'
                                          : '.')
                              .collect(
                                  StringBuilder::new,
                                  StringBuilder::appendCodePoint,
                                  StringBuilder::append)
                              .toString())
                  .toArray(String[]::new);
          // Array of which flattenings need to use named arguments.
          // Each array entry refers to the correspondingly indexed flattening.
          Boolean[] requiresNamedParameters =
              Arrays.stream(stringParams)
                  .map(
                      a ->
                          Arrays.stream(stringParams)
                              .anyMatch(b -> a.startsWith(b + "s") || a.startsWith(b + "ss")))
                  .toArray(Boolean[]::new);
          boolean requiresNameSuffix = flatteningGroups.size() > 1;
          // Build method list.
          for (int i = 0; i < flatteningGroups.size(); i++) {
            FlatteningConfig flatteningGroup = flatteningGroups.get(i);
            String nameSuffix = requiresNameSuffix ? Integer.toString(i + 1) : "";
            MethodContext methodContextFlat =
                context.asFlattenedMethodContext(method, flatteningGroup);
            methods.add(
                generatePagedFlattenedAsyncMethod(
                    methodContextFlat, nameSuffix, requiresNamedParameters[i]));
            methods.add(
                generatePagedFlattenedMethod(
                    methodContextFlat, nameSuffix, requiresNamedParameters[i]));
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
        generateInitCode(
            apiMethodTransformer.generateGrpcStreamingRequestObjectMethod(methodContext),
            methodContext,
            methodContext.getMethodConfig().getRequiredFieldConfigs(),
            InitCodeOutputType.SingleObject,
            CallingForm.RequestStreamingServer);
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
        generateInitCode(
            apiMethodTransformer.generateAsyncOperationFlattenedMethod(
                methodContext,
                Collections.<ParamWithSimpleDoc>emptyList(),
                ClientMethodType.AsyncOperationFlattenedMethod,
                true),
            methodContext,
            methodContext.getFlatteningConfig().getFlattenedFieldConfigs().values(),
            InitCodeOutputType.FieldList,
            CallingForm.LongRunningFlattenedAsync);
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
        generateInitCode(
            apiMethodTransformer.generateOperationFlattenedMethod(
                methodContext, Collections.<ParamWithSimpleDoc>emptyList()),
            methodContext,
            methodContext.getFlatteningConfig().getFlattenedFieldConfigs().values(),
            InitCodeOutputType.FieldList,
            CallingForm.LongRunningFlattened);
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
        generateInitCode(
            apiMethodTransformer.generateAsyncOperationRequestObjectMethod(
                methodContext, Collections.<ParamWithSimpleDoc>emptyList(), true),
            methodContext,
            methodContext.getMethodConfig().getRequiredFieldConfigs(),
            InitCodeOutputType.SingleObject,
            CallingForm.LongRunningRequestAsync);
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
        generateInitCode(
            apiMethodTransformer.generateOperationRequestObjectMethod(methodContext),
            methodContext,
            methodContext.getMethodConfig().getRequiredFieldConfigs(),
            InitCodeOutputType.SingleObject,
            CallingForm.LongRunningRequest);
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
      MethodContext methodContext, String suffix, boolean requiresNamedArguments) {
    StaticLangApiMethodView method =
        generateInitCode(
            apiMethodTransformer.generatePagedFlattenedAsyncMethod(
                methodContext, csharpCommonTransformer.pagedMethodAdditionalParams()),
            methodContext,
            methodContext.getFlatteningConfig().getFlattenedFieldConfigs().values(),
            InitCodeOutputType.FieldList,
            CallingForm.FlattenedAsyncPaged);
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
        .requiresNamedArguments(requiresNamedArguments)
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedFlattenedMethod(
      MethodContext methodContext, String suffix, boolean requiresNamedArguments) {
    StaticLangApiMethodView method =
        generateInitCode(
            apiMethodTransformer.generatePagedFlattenedMethod(
                methodContext, csharpCommonTransformer.pagedMethodAdditionalParams()),
            methodContext,
            methodContext.getFlatteningConfig().getFlattenedFieldConfigs().values(),
            InitCodeOutputType.FieldList,
            CallingForm.FlattenedPaged);
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
        .requiresNamedArguments(requiresNamedArguments)
        .build();
  }

  private StaticLangApiMethodSnippetView generatePagedRequestAsyncMethod(
      MethodContext methodContext) {
    StaticLangApiMethodView method =
        generateInitCode(
            apiMethodTransformer.generatePagedRequestObjectAsyncMethod(
                methodContext, csharpCommonTransformer.pagedMethodAdditionalParams()),
            methodContext,
            methodContext.getMethodConfig().getRequiredFieldConfigs(),
            InitCodeOutputType.SingleObject,
            CallingForm.RequestAsyncPaged);
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
        generateInitCode(
            apiMethodTransformer.generatePagedRequestObjectMethod(
                methodContext, csharpCommonTransformer.pagedMethodAdditionalParams()),
            methodContext,
            methodContext.getMethodConfig().getRequiredFieldConfigs(),
            InitCodeOutputType.SingleObject,
            CallingForm.RequestPaged);
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
        generateInitCode(
            apiMethodTransformer.generateFlattenedAsyncMethod(
                methodContext, ClientMethodType.FlattenedAsyncCallSettingsMethod),
            methodContext,
            methodContext.getFlatteningConfig().getFlattenedFieldConfigs().values(),
            InitCodeOutputType.FieldList,
            CallingForm.FlattenedAsync);
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
    StaticLangApiMethodView method =
        generateInitCode(
            apiMethodTransformer.generateFlattenedMethod(methodContext),
            methodContext,
            methodContext.getFlatteningConfig().getFlattenedFieldConfigs().values(),
            InitCodeOutputType.FieldList,
            CallingForm.Flattened);
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
        generateInitCode(
            apiMethodTransformer.generateRequestObjectMethod(methodContext),
            methodContext,
            methodContext.getMethodConfig().getRequiredFieldConfigs(),
            InitCodeOutputType.SingleObject,
            CallingForm.Request);
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
        generateInitCode(
            apiMethodTransformer.generateRequestObjectAsyncMethod(methodContext),
            methodContext,
            methodContext.getMethodConfig().getRequiredFieldConfigs(),
            InitCodeOutputType.SingleObject,
            CallingForm.RequestAsync);
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

  private StaticLangApiMethodView generateInitCode(
      StaticLangApiMethodView method,
      MethodContext context,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType initCodeOutputType,
      CallingForm callingForm) {
    // Replace the sample/init code using the same context as for the whole snippet file.
    // This is a bit hacky, but fixes the problem that initcode is generated using a different
    // context. Without this, the per-snippet imports don't get included in the snippet file.
    StaticLangApiMethodView.Builder builder = method.toBuilder();
    sampleTransformer.generateSamples(
        builder, context, fieldConfigs, initCodeOutputType, Arrays.asList(callingForm));
    return builder.build();
  }
}
