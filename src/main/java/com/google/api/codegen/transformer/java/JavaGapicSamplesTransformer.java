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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleFileRegistry;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import com.google.api.codegen.viewmodel.StaticLangFileView;
import com.google.api.codegen.viewmodel.StaticLangSampleClassView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.common.base.CaseFormat;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A transformer to generate Java standalone samples for each method in the GAPIC surface generated
 * from the same ApiModel.
 */
public class JavaGapicSamplesTransformer implements ModelToViewTransformer<ProtoApiModel> {
  private final GapicCodePathMapper pathMapper;

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "java/standalone_sample.snip";
  private final JavaMethodViewGenerator methodGenerator =
      new JavaMethodViewGenerator(SampleType.STANDALONE);
  private final StandardImportSectionTransformer importSectionTransformer =
      new StandardImportSectionTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);

  public JavaGapicSamplesTransformer(GapicCodePathMapper pathMapper) {
    this.pathMapper = pathMapper;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = createSurfaceNamer(productConfig);

    for (InterfaceModel apiInterface : model.getInterfaces()) {
      if (!productConfig.hasInterfaceConfig(apiInterface)) {
        continue;
      }

      boolean enableStringFormatFunctions = productConfig.getResourceNameMessageConfigs().isEmpty();
      ImportTypeTable typeTable = createTypeTable(namer.getExamplePackageName());
      InterfaceContext context =
          createInterfaceContext(
              apiInterface, productConfig, namer, typeTable, enableStringFormatFunctions);

      List<ViewModel> sampleFiles = generateSampleFiles(context);
      surfaceDocs.addAll(sampleFiles);
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(STANDALONE_SAMPLE_TEMPLATE_FILENAME);
  }

  private List<ViewModel> generateSampleFiles(InterfaceContext context) {
    List<ViewModel> files = new ArrayList<>();
    SurfaceNamer namer = context.getNamer();
    SampleFileRegistry generatedSamples = new SampleFileRegistry();

    StaticLangFileView.Builder<StaticLangSampleClassView> sampleFile =
        StaticLangFileView.<StaticLangSampleClassView>newBuilder();
    sampleFile.templateFileName(STANDALONE_SAMPLE_TEMPLATE_FILENAME);

    List<StaticLangApiMethodView> allMethods = methodGenerator.generateApiMethods(context);

    for (StaticLangApiMethodView method : allMethods) {
      for (MethodSampleView methodSample : method.samples()) {
        String callingForm = methodSample.callingForm().toLowerCamel();
        String valueSet = methodSample.valueSet().id();
        StaticLangSampleClassView classView =
            generateSampleClass(context, method, methodSample, callingForm, valueSet);
        String outputPath =
            pathMapper.getSamplesOutputPath(
                context.getInterfaceModel().getFullName(),
                context.getProductConfig(),
                method.name());
        String fullPath =
            outputPath + File.separator + namer.getApiSampleFileName(classView.name());
        generatedSamples.addFile(
            fullPath, method.name(), callingForm, valueSet, methodSample.regionTag());
        files.add(
            sampleFile
                .classView(classView)
                .outputPath(fullPath)
                .fileHeader(
                    fileHeaderTransformer.generateFileHeader(
                        context,
                        classView.name())) // must be done as the last step to catch all imports
                .build());
      }
    }

    return files;
  }

  private StaticLangSampleClassView generateSampleClass(
      InterfaceContext context,
      StaticLangApiMethodView method,
      MethodSampleView methodSample,
      String callingForm,
      String valueSet) {
    SurfaceNamer namer = context.getNamer();
    StaticLangSampleClassView.Builder sampleClass = StaticLangSampleClassView.newBuilder();
    sampleClass
        .name(
            namer.getApiSampleClassName(
                CaseFormat.LOWER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, method.name()),
                callingForm,
                valueSet))
        .libraryMethod(method.toBuilder().samples(Arrays.asList(methodSample)).build());

    return sampleClass.build();
  }

  private SurfaceNamer createSurfaceNamer(GapicProductConfig productConfig) {
    // TODO(vchudnov-g): Consider factoring out this code duplicated from JavaSurfaceTransformer.
    return new JavaSurfaceNamer(productConfig.getPackageName(), productConfig.getPackageName());
  }

  private GapicInterfaceContext createInterfaceContext(
      InterfaceModel apiInterface,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      ImportTypeTable typeTable,
      boolean enableStringFormatFunctions) {
    // TODO(vchudnov-g): Consider factoring out this code duplicated from JavaSurafceTransformer.
    return GapicInterfaceContext.create(
        apiInterface,
        productConfig,
        (ModelTypeTable) typeTable,
        namer,
        JavaFeatureConfig.newBuilder()
            .enableStringFormatFunctions(enableStringFormatFunctions)
            .build());
  }

  private ModelTypeTable createTypeTable(String implicitPackageName) {
    // TODO(vchudnov-g): Consider factoring out this code duplicated from JavaSurfaceTransformer.
    return new ModelTypeTable(
        new JavaTypeTable(implicitPackageName),
        new JavaModelTypeNameConverter(implicitPackageName));
  }
}
