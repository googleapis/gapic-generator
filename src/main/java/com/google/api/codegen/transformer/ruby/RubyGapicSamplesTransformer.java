/* Copyright 2019 Google LLC
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
package com.google.api.codegen.transformer.ruby;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.DynamicLangGapicSamplesTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleManifestTransformer;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.ruby.RubyTypeTable;
import java.util.function.Function;

/**
 * A transformer to generate Ruby standalone samples for each method in the GAPIC surface generated
 * from the same ApiModel.
 */
public class RubyGapicSamplesTransformer extends DynamicLangGapicSamplesTransformer {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "ruby/standalone_sample.snip";

  private static final RubyImportSectionTransformer importSectionTransformer =
      new RubyImportSectionTransformer();
  private static final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private static final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new RubyApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer, false))
              .sampleType(SampleSpec.SampleType.STANDALONE)
              .build());
  private static final Function<GapicProductConfig, FeatureConfig> newFeatureConfig =
      p -> new RubyFeatureConfig();
  private static final Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer =
      product -> new RubySurfaceNamer(product.getPackageName());
  private static final Function<String, ModelTypeTable> newTypeTable =
      pkg -> new ModelTypeTable(new RubyTypeTable(pkg), new RubyModelTypeNameConverter(pkg));
  private final GapicCodePathMapper pathMapper;

  public RubyGapicSamplesTransformer(GapicCodePathMapper pathMapper) {
    super(
        STANDALONE_SAMPLE_TEMPLATE_FILENAME,
        pathMapper,
        fileHeaderTransformer,
        apiMethodTransformer,
        newFeatureConfig,
        newSurfaceNamer,
        newTypeTable);
    this.pathMapper = pathMapper;
  }

  public SampleManifestTransformer createManifestTransformer() {
    return new SampleManifestTransformer(
        new RubySampleMetadataNamer(this, pathMapper),
        p -> new RubyFeatureConfig(),
        newSurfaceNamer,
        newTypeTable,
        pathMapper);
  }
}
