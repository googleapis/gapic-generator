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
import com.google.api.codegen.ProtoFileView;
import com.google.api.codegen.config.GapicInterfaceConfig;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.GrpcElementDocTransformer;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import com.google.api.codegen.viewmodel.GrpcDocView;
import com.google.api.codegen.viewmodel.GrpcElementDocView;
import com.google.api.codegen.viewmodel.ImportSectionView;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.ModuleView;
import com.google.api.codegen.viewmodel.metadata.SimpleModuleView;
import com.google.api.codegen.viewmodel.metadata.TocContentView;
import com.google.api.codegen.viewmodel.metadata.TocModuleView;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.Model;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.common.collect.ImmutableList;
import com.google.protobuf.Api;
import java.util.List;

public class RubyGapicSurfaceDocTransformer implements ModelToViewTransformer {
  private static final String DOC_TEMPLATE_FILENAME = "ruby/message.snip";

  private final GapicCodePathMapper pathMapper;
  private final PackageMetadataConfig packageConfig;
  private final FileHeaderTransformer fileHeaderTransformer = new FileHeaderTransformer(null);
  private final GrpcElementDocTransformer elementDocTransformer = new GrpcElementDocTransformer();

  public RubyGapicSurfaceDocTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    this.pathMapper = pathMapper;
    this.packageConfig = packageConfig;
  }

  @Override
  public List<String> getTemplateFileNames() {
    return ImmutableList.of(DOC_TEMPLATE_FILENAME);
  }

  @Override
  public List<ViewModel> transform(Model model, GapicProductConfig productConfig) {
    ImmutableList.Builder<ViewModel> surfaceDocs = ImmutableList.builder();
    for (ProtoFile file : new ProtoFileView().getElementIterable(model)) {
      surfaceDocs.add(generateDoc(file, productConfig));
    }
    surfaceDocs.add(generateOverview(model, productConfig));
    return surfaceDocs.build();
  }

  private ViewModel generateDoc(ProtoFile file, GapicProductConfig productConfig) {
    ModelTypeTable typeTable =
        new ModelTypeTable(
            new RubyTypeTable(productConfig.getPackageName()),
            new RubyModelTypeNameConverter(productConfig.getPackageName()));
    // Use file path for package name to get file-specific package instead of package for the API.
    SurfaceNamer namer = new RubySurfaceNamer(typeTable.getFullNameFor(file));
    String subPath = pathMapper.getOutputPath(file, productConfig);
    String baseFilename = namer.getProtoFileName(file);
    GrpcDocView.Builder doc = GrpcDocView.newBuilder();
    doc.templateFileName(DOC_TEMPLATE_FILENAME);
    doc.outputPath(subPath + "/doc/" + baseFilename);
    doc.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));
    doc.elementDocs(elementDocTransformer.generateElementDocs(typeTable, namer, file));
    doc.modules(
        generateModuleViews(
            file.getModel(), productConfig, namer, isSourceApiInterfaceFile(file), false));
    return doc.build();
  }

  private ViewModel generateOverview(Model model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());
    GrpcDocView.Builder doc = GrpcDocView.newBuilder();
    doc.templateFileName(DOC_TEMPLATE_FILENAME);
    doc.outputPath(pathMapper.getOutputPath(null, productConfig) + "/doc/overview.rb");
    doc.fileHeader(
        fileHeaderTransformer.generateFileHeader(
            productConfig, ImportSectionView.newBuilder().build(), namer));
    doc.elementDocs(ImmutableList.<GrpcElementDocView>of());
    doc.modules(generateModuleViews(model, productConfig, namer, false, true));
    return doc.build();
  }

  private boolean isSourceApiInterfaceFile(ProtoFile file) {
    List<Api> apis = file.getModel().getServiceConfig().getApisList();
    for (Interface apiInterface : file.getReachableInterfaces()) {
      for (Api api : apis) {
        if (api.getName().equals(apiInterface.getFullName())) {
          return true;
        }
      }
    }
    return false;
  }

  private List<ModuleView> generateModuleViews(
      Model model,
      GapicProductConfig productConfig,
      SurfaceNamer namer,
      boolean hasToc,
      boolean hasOverview) {
    ImmutableList.Builder<ModuleView> moduleViews = ImmutableList.builder();
    for (String moduleName : namer.getApiModules()) {
      if (moduleName.equals(namer.getModuleVersionName()) && hasToc) {
        moduleViews.add(generateTocModuleView(model, productConfig, namer, moduleName));
      } else if (moduleName.equals(namer.getModuleServiceName()) && hasOverview) {
        moduleViews.add(generateOverviewView(model, productConfig));
      } else {
        moduleViews.add(SimpleModuleView.newBuilder().moduleName(moduleName).build());
      }
    }
    return moduleViews.build();
  }

  private TocModuleView generateTocModuleView(
      Model model, GapicProductConfig productConfig, SurfaceNamer namer, String moduleName) {
    RubyPackageMetadataTransformer metadataTransformer =
        new RubyPackageMetadataTransformer(packageConfig);
    RubyPackageMetadataNamer packageNamer =
        new RubyPackageMetadataNamer(productConfig.getPackageName());
    String version = packageConfig.apiVersion();
    ImmutableList.Builder<TocContentView> tocContents = ImmutableList.builder();
    for (Interface apiInterface : new InterfaceView().getElementIterable(model)) {
      GapicInterfaceConfig interfaceConfig = productConfig.getInterfaceConfig(apiInterface);
      tocContents.add(
          metadataTransformer.generateTocContent(
              model, packageNamer, version, namer.getApiWrapperClassName(interfaceConfig)));
    }

    tocContents.add(
        metadataTransformer.generateDataTypeTocContent(
            productConfig.getPackageName(), packageNamer, version));

    return TocModuleView.newBuilder()
        .moduleName(moduleName)
        .fullName(model.getServiceConfig().getTitle())
        .contents(tocContents.build())
        .build();
  }

  private ModuleView generateOverviewView(Model model, GapicProductConfig productConfig) {
    SurfaceNamer namer = new RubySurfaceNamer(productConfig.getPackageName());
    RubyPackageMetadataTransformer metadataTransformer =
        new RubyPackageMetadataTransformer(packageConfig);
    RubyPackageMetadataNamer packageNamer =
        new RubyPackageMetadataNamer(productConfig.getPackageName());
    return metadataTransformer
        .generateReadmeMetadataView(model, productConfig, packageNamer)
        .moduleName(namer.getModuleServiceName())
        .build();
  }
}
