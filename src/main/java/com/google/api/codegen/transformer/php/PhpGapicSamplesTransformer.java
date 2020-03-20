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
package com.google.api.codegen.transformer.php;

/**
 * A transformer to generate PHP standalone samples for each method in the GAPIC surface generated
 * from the same ApiModel.
 */
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.DynamicLangGapicSamplesTransformer;
import com.google.api.codegen.transformer.FeatureConfig;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ImportSectionTransformer;
import com.google.api.codegen.transformer.InitCodeTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleManifestTransformer;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.SurfaceNamer;
import com.google.api.codegen.util.php.PhpTypeTable;
import com.google.api.codegen.viewmodel.DynamicLangSampleView;
import com.google.api.codegen.viewmodel.MethodSampleView;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.common.base.Strings;
import java.io.File;
import java.util.function.Function;

public class PhpGapicSamplesTransformer extends DynamicLangGapicSamplesTransformer {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "php/standalone_sample.snip";
  private static final SampleType sampleType = SampleType.STANDALONE;
  private static final ImportSectionTransformer importSectionTransformer =
      new PhpImportSectionTransformer();
  private static final DynamicLangApiMethodTransformer apiMethodTransformer =
      new DynamicLangApiMethodTransformer(
          new PhpApiMethodParamTransformer(),
          SampleTransformer.newBuilder()
              .initCodeTransformer(new InitCodeTransformer(importSectionTransformer, false))
              .sampleType(SampleType.STANDALONE)
              .sampleImportTransformer(new PhpSampleImportTransformer())
              .build());
  private static final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(importSectionTransformer);
  private static final Function<GapicProductConfig, FeatureConfig> newFeatureConfig =
      p -> new PhpFeatureConfig(p);
  private static final Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer =
      product -> new PhpSurfaceNamer(product.getPackageName());
  private static final Function<String, ModelTypeTable> newTypeTable =
      pkg ->
          new ModelTypeTable(
              new PhpTypeTable(pkg + "\\Samples"),
              new PhpModelTypeNameConverter(pkg + "\\Samples"));
  private final GapicCodePathMapper pathMapper;

  public PhpGapicSamplesTransformer(GapicCodePathMapper pathMapper) {
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

  @Override
  protected DynamicLangSampleView.Builder newSampleFileViewBuilder(
      GapicProductConfig productConfig,
      InterfaceContext context,
      String sampleFileName,
      OptionalArrayMethodView method,
      MethodSampleView sample) {

    DynamicLangSampleView.Builder builder =
        super.newSampleFileViewBuilder(productConfig, context, sampleFileName, method, sample);
    String outputPath = builder.outputPath();
    String autoloadPath =
        "__DIR__ . '"
            + Strings.repeat(
                "/..", (int) outputPath.chars().filter(c -> c == File.separatorChar).count())
            + "/vendor/autoload.php'";

    return builder.autoloadPath(autoloadPath);
  }

  public SampleManifestTransformer createManifestTransformer() {
    return new SampleManifestTransformer(
        new PhpSampleMetadataNamer(this, pathMapper),
        p -> new PhpFeatureConfig(p),
        newSurfaceNamer,
        newTypeTable,
        pathMapper);
  }
}
