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
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DefaultFeatureConfig;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.DynamicLangGapicSamplesTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.py.PythonTypeTable;
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
      p -> new PythonSurfaceNamer(p.getPackageName());

  private final PackageMetadataConfig packageConfig;

  public PythonGapicSamplesTransformer(
      GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
    super(
        STANDALONE_SAMPLE_TEMPLATE_FILENAME,
        pathMapper,
        fileHeaderTransformer,
        apiMethodTransformer,
        new DefaultFeatureConfig(),
        newSurfaceNamer,
        p -> new ModelTypeTable(new PythonTypeTable(p), new PythonModelTypeNameConverter(p)));
    this.packageConfig = packageConfig;
  }

  @Override
  protected String getGapicPackageName(GapicProductConfig productConfig) {
    return newSurfaceNamer.apply(productConfig).getGapicPackageName(packageConfig.packageName());
  }
}
