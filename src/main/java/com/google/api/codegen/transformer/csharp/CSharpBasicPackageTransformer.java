/* Copyright 2017 Google LLC
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

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.SimpleInitFileView;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

/* Transforms a ProtoApiModel into the smoke tests of an API for C#. */
public class CSharpBasicPackageTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String SMOKETEST_CSPROJ_TEMPLATE_FILENAME =
      "csharp/gapic_smoketest_csproj.snip";
  private static final String SNIPPETS_CSPROJ_TEMPLATE_FILENAME =
      "csharp/gapic_snippets_csproj.snip";
  private static final String UNITTEST_CSPROJ_TEMPLATE_FILENAME =
      "csharp/gapic_unittest_csproj.snip";

  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.MessagesOnly;

  private final GapicCodePathMapper pathMapper;
  private final String templateFilename;
  private final String fileBaseSuffix;
  private final Predicate<GapicInterfaceContext> shouldGenerateFn;
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());

  private CSharpBasicPackageTransformer(
      GapicCodePathMapper pathMapper,
      String templateFilename,
      String fileBaseSuffix,
      Predicate<GapicInterfaceContext> shouldGenerateFn) {
    this.pathMapper = pathMapper;
    this.templateFilename = templateFilename;
    this.fileBaseSuffix = fileBaseSuffix;
    this.shouldGenerateFn = shouldGenerateFn;
  }

  public static CSharpBasicPackageTransformer forSmokeTests(GapicCodePathMapper pathMapper) {
    return new CSharpBasicPackageTransformer(
        pathMapper,
        SMOKETEST_CSPROJ_TEMPLATE_FILENAME,
        ".SmokeTests.csproj",
        shouldGenSmokeTestPackage());
  }

  public static CSharpBasicPackageTransformer forSnippets(GapicCodePathMapper pathMapper) {
    return new CSharpBasicPackageTransformer(
        pathMapper, SNIPPETS_CSPROJ_TEMPLATE_FILENAME, ".Snippets.csproj", parameter -> true);
  }

  public static CSharpBasicPackageTransformer forUnitTests(GapicCodePathMapper pathMapper) {
    return new CSharpBasicPackageTransformer(
        pathMapper, UNITTEST_CSPROJ_TEMPLATE_FILENAME, ".Tests.csproj", parameter -> true);
  }

  private static Predicate<GapicInterfaceContext> shouldGenSmokeTestPackage() {
    return parameter -> parameter.getInterfaceConfig().getSmokeTestConfig() != null;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName(), ALIAS_MODE);

    for (InterfaceModel apiInterface : model.getInterfaces()) {
      if (!productConfig.hasInterfaceConfig(apiInterface)) {
        continue;
      }

      GapicInterfaceContext context =
          GapicInterfaceContext.create(
              apiInterface,
              productConfig,
              csharpCommonTransformer.createTypeTable(namer.getPackageName(), ALIAS_MODE),
              namer,
              new CSharpFeatureConfig());
      if (shouldGenerateFn.test(context)) {
        surfaceDocs.add(generateCsproj(context));
      }
    }

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(templateFilename);
  }

  private SimpleInitFileView generateCsproj(GapicInterfaceContext context) {
    GapicProductConfig productConfig = context.getProductConfig();
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), productConfig);
    return SimpleInitFileView.create(
        templateFilename,
        outputPath + File.separator + productConfig.getPackageName() + fileBaseSuffix,
        fileHeaderTransformer.generateFileHeader(context));
  }
}
