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
package com.google.api.codegen.transformer.nodejs;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.nodejs.NodeJSUtils;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.DynamicLangGapicSamplesTransformer;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleManifestTransformer;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.js.JSTypeTable;
import java.util.function.Function;

/**
 * A transformer to generate NodeJS standalone samples for each method in the GAPIC surface
 * generated from the same ApiModel.
 */
public class NodeJSGapicSamplesTransformer extends DynamicLangGapicSamplesTransformer {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "nodejs/standalone_sample.snip";

  private static final ImportSectionTransformer importSectionTransformer =
      new NodeJSImportSectionTransformer();
  private static final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private static final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new NodeJSApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer, false))
              .sampleImportTransformer(new NodeJSSampleImportTransformer())
              .sampleType(SampleType.STANDALONE)
              .build());
  private static final Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer =
      product -> new NodeJSSurfaceNamer(product.getPackageName(), NodeJSUtils.isGcloud(product));
  private static final Function<String, ModelTypeTable> newTypeTable =
      pkg -> new ModelTypeTable(new JSTypeTable(pkg), new NodeJSModelTypeNameConverter(pkg));
  private final GapicCodePathMapper pathMapper;

  public NodeJSGapicSamplesTransformer(GapicCodePathMapper pathMapper) {
    super(
        STANDALONE_SAMPLE_TEMPLATE_FILENAME,
        pathMapper,
        fileHeaderTransformer,
        apiMethodTransformer,
        new NodeJSFeatureConfig(),
        newSurfaceNamer,
        newTypeTable);
    this.pathMapper = pathMapper;
  }

  public SampleManifestTransformer createManifestTransformer() {
    return new SampleManifestTransformer(
        new NodeJSSampleMetadataNamer(this),
        p -> new NodeJSFeatureConfig(),
        newSurfaceNamer,
        newTypeTable,
        pathMapper);
  }
}
