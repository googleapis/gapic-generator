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

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.DynamicLangGapicSamplesTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleManifestTransformer;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.py.PythonTypeTable;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import java.util.function.Function;

/**
 * A transformer to generate Python standalone samples for each method in the GAPIC surface
 * generated from the same ApiModel.
 */
public class PythonGapicSamplesTransformer extends DynamicLangGapicSamplesTransformer {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "py/standalone_sample.snip";
  private static final SampleType sampleType = SampleType.STANDALONE;

  private static final PythonImportSectionTransformer importSectionTransformer =
      new PythonImportSectionTransformer();
  private static final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private static final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new PythonApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer, false))
              .sampleType(sampleType)
              .sampleImportTransformer(new PythonSampleImportTransformer())
              .build());
  private static final Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer =
      product -> new PythonSurfaceNamer(product.getPackageName());
  private static final Function<String, ModelTypeTable> newTypeTable =
      pkg -> new ModelTypeTable(new PythonTypeTable(pkg), new PythonModelTypeNameConverter(pkg));

  private final PackageMetadataConfig packageConfig;
  private final GapicCodePathMapper pathMapper;

  public PythonGapicSamplesTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    super(
        STANDALONE_SAMPLE_TEMPLATE_FILENAME,
        pathMapper,
        fileHeaderTransformer,
        apiMethodTransformer,
        new DefaultFeatureConfig(),
        newSurfaceNamer,
        newTypeTable);
    this.packageConfig = packageConfig;
    this.pathMapper = pathMapper;
  }

  @Override
  protected DynamicLangSampleView.Builder newSampleFileViewBuilder(
      GapicProductConfig productConfig,
      InterfaceContext context,
      String sampleFileName,
      OptionalArrayMethodView method,
      MethodSampleView sample) {
    return super.newSampleFileViewBuilder(productConfig, context, sampleFileName, method, sample)
        .gapicPackageName(
            newSurfaceNamer.apply(productConfig).getGapicPackageName(packageConfig.packageName()));
  }

  public SampleManifestTransformer createManifestTransformer() {
    return new SampleManifestTransformer(
        new PythonSampleMetadataNamer(this, pathMapper),
        p -> new DefaultFeatureConfig(),
        newSurfaceNamer,
        newTypeTable,
        pathMapper);
  }
}
