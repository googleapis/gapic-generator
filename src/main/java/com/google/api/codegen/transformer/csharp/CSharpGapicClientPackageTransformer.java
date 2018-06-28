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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.PackageMetadataNamer;
import com.google.api.codegen.transformer.PackageMetadataTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.viewmodel.PackageInfoView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.PackageMetadataView;
import com.google.api.tools.framework.model.Model;
import com.google.common.base.Splitter;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CSharpGapicClientPackageTransformer implements ModelToViewTransformer<ProtoApiModel> {
  private static final String CSPROJ_TEMPLATE_FILENAME = "csharp/gapic_csproj.snip";

  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.Global;

  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageMetadataConfig;
  private final CSharpCommonTransformer csharpCommonTransformer = new CSharpCommonTransformer();
  private final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());
  private final PackageMetadataTransformer metadataTransformer = new PackageMetadataTransformer();

  public CSharpGapicClientPackageTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageMetadataConfig) {
    this.pathMapper = pathMapper;
    this.packageMetadataConfig = packageMetadataConfig;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    List<ViewModel> surfaceDocs = new ArrayList<>();
    SurfaceNamer namer = new CSharpSurfaceNamer(productConfig.getPackageName(), ALIAS_MODE);
    CSharpFeatureConfig featureConfig = new CSharpFeatureConfig();

    InterfaceModel lastApiInterface = null;
    for (InterfaceModel apiInterface : model.getInterfaces()) {
      lastApiInterface = apiInterface;
    }

    GapicInterfaceContext context =
        GapicInterfaceContext.create(
            lastApiInterface,
            productConfig,
            csharpCommonTransformer.createTypeTable(productConfig.getPackageName(), ALIAS_MODE),
            namer,
            featureConfig);
    surfaceDocs.add(generateCsProjView(context));

    return surfaceDocs;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Arrays.asList(CSPROJ_TEMPLATE_FILENAME);
  }

  private PackageInfoView generateCsProjView(GapicInterfaceContext context) {
    Model model = context.getModel();
    GapicProductConfig productConfig = context.getProductConfig();
    PackageInfoView.Builder view = PackageInfoView.newBuilder();
    view.templateFileName(CSPROJ_TEMPLATE_FILENAME);
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), productConfig);
    view.outputPath(outputPath + File.separator + productConfig.getPackageName() + ".csproj");
    view.fileHeader(fileHeaderTransformer.generateFileHeader(context));
    view.serviceTitle(model.getServiceConfig().getTitle());
    view.serviceDescription(model.getServiceConfig().getDocumentation().getSummary().trim());
    view.domainLayerLocation(productConfig.getDomainLayerLocation());
    view.authScopes(new ArrayList<>()); // Unused in C#
    view.releaseLevel(productConfig.getReleaseLevel());
    String versionSuffix;
    switch (productConfig.getReleaseLevel()) {
      case ALPHA:
        versionSuffix = "-alpha01";
        break;
      case BETA:
        versionSuffix = "-beta01";
        break;
      default:
        versionSuffix = "";
        break;
    }
    view.version("1.0.0" + versionSuffix);
    String tags = "";
    for (String tag : Splitter.on('.').split(productConfig.getPackageName())) {
      if (tag.matches("[vV][\\d.]+")) {
        break;
      }
      tags += ";" + tag;
    }
    view.tags(tags.isEmpty() ? "" : tags.substring(1));
    view.packageMetadata(generatePackageMetadataView(context));
    view.serviceDocs(new ArrayList<>());
    return view.build();
  }

  private PackageMetadataView generatePackageMetadataView(GapicInterfaceContext context) {
    String outputPath =
        pathMapper.getOutputPath(context.getInterface().getFullName(), context.getProductConfig());
    return metadataTransformer
        .generateMetadataView(
            new PackageMetadataNamer(),
            packageMetadataConfig,
            context.getApiModel(),
            CSPROJ_TEMPLATE_FILENAME,
            outputPath,
            TargetLanguage.CSHARP)
        .build();
  }
}
