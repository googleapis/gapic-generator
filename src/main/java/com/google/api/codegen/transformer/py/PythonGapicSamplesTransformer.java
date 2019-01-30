/* Copyright 2018 Google LLC
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
package com.google.api.codegen.transformer.py;

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.GapicMethodConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.OutputTransformer;
import com.google.api.codegen.transformer.SampleFileRegistry;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StandardSampleImportTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.io.File;
import java.util.Collections;
import java.util.List;

/**
 * A transformer to generate Python standalone samples for each method in the GAPIC surface
 * generated from the same ApiModel.
 */
public class PythonGapicSamplesTransformer implements ModelToViewTransformer<ProtoApiModel> {
  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "py/standalone_sample.snip";
  private static final SampleType sampleType = SampleType.STANDALONE;

  private final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new PythonApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer))
              .sampleType(sampleType)
              .outputTransformer(
                  new OutputTransformer(
                      new PythonSampleOutputImportTransformer(),
                      new PythonSamplePrintArgTransformer()))
              .sampleImportTransformer(
                  new StandardSampleImportTransformer(new PythonImportSectionTransformer()))
              .build());
  private final PythonMethodViewGenerator methodGenerator =
      new PythonMethodViewGenerator(apiMethodTransformer);
  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;

  public PythonGapicSamplesTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(STANDALONE_SAMPLE_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel apiModel, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> views = ImmutableList.builder();
    views.addAll(generateSampleFiles(apiModel, productConfig));
    return views.build();
  }

  private Iterable<ViewModel> generateSampleFiles(
      ProtoApiModel apiModel, GapicProductConfig productConfig) {
    ModelTypeTable modelTypeTable =
        new ModelTypeTable(
            new PythonTypeTable(productConfig.getPackageName()),
            new PythonModelTypeNameConverter(productConfig.getPackageName()));
    SurfaceNamer namer = new PythonSurfaceNamer(productConfig.getPackageName());
    FeatureConfig featureConfig = new DefaultFeatureConfig();
    ImmutableList.Builder<ViewModel> serviceSurfaces = ImmutableList.builder();

    for (InterfaceModel apiInterface : apiModel.getInterfaces()) {
      if (!productConfig.hasInterfaceConfig(apiInterface)) {
        continue;
      }

      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface, productConfig, modelTypeTable, namer, featureConfig);
      addApiImports(context);
      serviceSurfaces.addAll(generateSampleClasses(context));
    }

    return serviceSurfaces.build();
  }

  private void addApiImports(GapicInterfaceContext context) {
    for (TypeRef type : context.getInterface().getModel().getSymbolTable().getDeclaredTypes()) {
      if (type.isEnum() && type.getEnumType().isReachable()) {
        context.getImportTypeTable().getAndSaveNicknameFor(type);
        break;
      }
    }

    for (MethodModel method : context.getSupportedMethods()) {
      addMethodImports(context.asDynamicMethodContext(method));
    }
  }

  private void addMethodImports(GapicMethodContext context) {
    ModelTypeTable typeTable = context.getTypeTable();
    GapicMethodConfig methodConfig = context.getMethodConfig();
    if (methodConfig.isLongRunningOperation()) {
      typeTable.getAndSaveNicknameFor(methodConfig.getLongRunningConfig().getReturnType());
      typeTable.getAndSaveNicknameFor(methodConfig.getLongRunningConfig().getMetadataType());
    }

    typeTable.getAndSaveNicknameFor(context.getMethod().getInputType());
    addFieldsImports(typeTable, methodConfig.getRequiredFields());
    addFieldsImports(typeTable, methodConfig.getOptionalFields());
  }

  private void addFieldsImports(ModelTypeTable typeTable, Iterable<FieldModel> fields) {
    for (FieldModel field : fields) {
      typeTable.getAndSaveNicknameFor(field);
    }
  }

  private List<ViewModel> generateSampleClasses(GapicInterfaceContext context) {
    ImmutableList.Builder<ViewModel> viewModels = new ImmutableList.Builder<>();
    SurfaceNamer namer = context.getNamer();
    SampleFileRegistry generatedSamples = new SampleFileRegistry();

    List<OptionalArrayMethodView> allmethods = methodGenerator.generateApiMethods(context);

    DynamicLangSampleView.Builder sampleClassBuilder = DynamicLangSampleView.newBuilder();
    for (OptionalArrayMethodView method : allmethods) {
      String subPath =
          pathMapper.getSamplesOutputPath(
              context.getInterfaceModel().getFullName(), context.getProductConfig(), method.name());
      for (MethodSampleView methodSample : method.samples()) {
        String callingForm = methodSample.callingForm().toLowerCamel();
        String valueSet = methodSample.valueSet().id();
        String className = namer.getApiSampleClassName(method.name(), callingForm, valueSet);
        String sampleOutputPath = subPath + File.separator + namer.getApiSampleFileName(className);
        generatedSamples.addFile(
            sampleOutputPath, method.name(), callingForm, valueSet, methodSample.regionTag());
        viewModels.add(
            sampleClassBuilder
                .templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME)
                .fileHeader(fileHeaderTransformer.generateFileHeader(context))
                .outputPath(sampleOutputPath)
                .className(className)
                .libraryMethod(
                    method.toBuilder().samples(Collections.singletonList(methodSample)).build())
                .gapicPackageName(namer.getGapicPackageName(packageConfig.packageName()))
                .build());
      }
    }
    return viewModels.build();
  }
}
