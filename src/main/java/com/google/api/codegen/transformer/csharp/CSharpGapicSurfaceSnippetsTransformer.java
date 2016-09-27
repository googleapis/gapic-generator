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

import com.google.api.codegen.ApiConfig;
import com.google.api.codegen.InterfaceView;
import com.google.api.codegen.MethodConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.ApiMethodTransformer;
import com.google.api.codegen.transformer.ImportTypeTransformer;
import com.google.api.codegen.transformer.MethodTransformerContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.ParamWithSimpleDoc;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.transformer.SurfaceTransformerContext;
import com.google.api.codegen.util.csharp.CSharpTypeTable;
import com.google.api.codegen.viewmodel.ApiMethodType;
import com.google.api.codegen.viewmodel.SnippetsFileView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangXApiView;
import com.google.api.codegen.viewmodel.StaticLangXCommonView;
import com.google.api.codegen.viewmodel.StaticLangXSettingsView;
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

public class CSharpGapicSurfaceSnippetsTransformer implements ModelToViewTransformer {

  private static final String XAPI_TEMPLATE_FILENAME = "csharp/gapic_snippets.snip";

  private final GapicCodePathMapper pathMapper;
  private final ImportTypeTransformer importTypeTransformer;
  private final ApiMethodTransformer apiMethodTransformer;

  public CSharpGapicSurfaceSnippetsTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
    this.importTypeTransformer = new ImportTypeTransformer();
    this.apiMethodTransformer = new ApiMethodTransformer();
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
      addCommonImports(context);
      SnippetsFileView snippets = generateSnippets(context);
      surfaceDocs.add(snippets);
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(XAPI_TEMPLATE_FILENAME);
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    return new ModelTypeTable(
        new CSharpTypeTable(implicitPackageName),
        new CSharpModelTypeNameConverter(implicitPackageName));
  }

  private void addCommonImports(SurfaceTransformerContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    // Common imports, only one class per required namespace is needed.
    typeTable.saveNicknameFor("Google.Protobuf.Bytestring");
    typeTable.saveNicknameFor("Google.Protobuf.WellKnownTypes.SomeSortOfWellKnownType");
    typeTable.saveNicknameFor("Grpc.Core.ByteString");
    typeTable.saveNicknameFor("System.Collections.ObjectModel.ReadOnlyCollection");
    typeTable.saveNicknameFor("System.Threading.Tasks.Task");
    typeTable.saveNicknameFor("System.Threading.Thread");
    typeTable.saveNicknameFor("System.NotImplementedException");
    typeTable.saveNicknameFor("System.Collections.IEnumerable");
    typeTable.saveNicknameFor("System.Collections.Generic.IEnumerable");
  }

  private SnippetsFileView generateSnippets(SurfaceTransformerContext context) {
    SurfaceNamer namer = context.getNamer();
    String name = namer.getApiSnippetsClassName(context.getInterface());
    SnippetsFileView.Builder snippetsBuilder = SnippetsFileView.newBuilder();

    snippetsBuilder.templateFileName(XAPI_TEMPLATE_FILENAME);
    String outputPath = pathMapper.getOutputPath(context.getInterface(), context.getApiConfig());
    snippetsBuilder.outputPath(outputPath + File.separator + name + ".g.cs");
    snippetsBuilder.packageName(context.getApiConfig().getPackageName() + ".Snippets");
    snippetsBuilder.name(name);
    snippetsBuilder.clientTypeName(namer.getApiWrapperClassName(context.getInterface()));
    snippetsBuilder.snippetMethods(generateMethods(context));

    // must be done as the last step to catch all imports
    snippetsBuilder.imports(
        importTypeTransformer.generateImports(context.getTypeTable().getImports()));

    return snippetsBuilder.build();
  }

  private List<StaticLangApiMethodView> generateMethods(SurfaceTransformerContext context) {
    boolean mixinsDisabled = !context.getFeatureConfig().enableMixins();
    List<ParamWithSimpleDoc> pagedMethodAdditionalParams =
        ImmutableList.of(
            makeParam(
                "string",
                "pageToken",
                "null",
                "The token returned from the previous request.",
                "A value of <c>null</c> or an empty string retrieves the first page."),
            makeParam(
                "int?",
                "pageSize",
                "null",
                "The size of page to request. The response will not be larger than this, but may be smaller.",
                "A value of <c>null</c> or 0 uses a server-defined page size."));
    List<StaticLangApiMethodView> methods = new ArrayList<>();

    for (Method method : context.getSupportedMethods()) {
      MethodConfig methodConfig = context.getMethodConfig(method);
      if (mixinsDisabled && methodConfig.getRerouteToGrpcInterface() != null) {
        continue;
      }
      MethodTransformerContext methodContext = context.asMethodContext(method);
      if (methodConfig.isPageStreaming()) {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            methods.add(
                apiMethodTransformer.generatePagedFlattenedAsyncMethod(
                    methodContext, fields, pagedMethodAdditionalParams));
            methods.add(
                apiMethodTransformer.generatePagedFlattenedMethod(
                    methodContext, fields, pagedMethodAdditionalParams));
          }
        }
      } else {
        if (methodConfig.isFlattening()) {
          for (ImmutableList<Field> fields : methodConfig.getFlattening().getFlatteningGroups()) {
            methods.add(
                apiMethodTransformer.generateFlattenedAsyncMethod(
                    methodContext, fields, ApiMethodType.FlattenedAsyncCallSettingsMethod));
            methods.add(apiMethodTransformer.generateFlattenedMethod(methodContext, fields));
          }
        }
      }
    }

    return methods;
  }

  private ParamWithSimpleDoc makeParam(
      String typeName, String name, String defaultValue, String... doc) {
    return ParamWithSimpleDoc.newBuilder()
        .name(name)
        .elementTypeName("")
        .typeName(typeName)
        .setCallName("")
        .isMap(false)
        .isArray(false)
        .defaultValue(defaultValue)
        .docLines(Arrays.asList(doc))
        .build();
  }
}
