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
package com.google.api.codegen.discogapic;

import static com.google.api.codegen.common.TargetLanguage.JAVA;

import com.google.api.codegen.common.CodeGenerator;
import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.DiscoApiModel;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicRequestToViewTransformer;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicResourceNameToViewTransformer;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicSchemaToViewTransformer;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicSurfaceTransformer;
import com.google.api.codegen.gapic.ArtifactFlags;
import com.google.api.codegen.gapic.CommonGapicCodePathMapper;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.transformer.ModelToViewTransformer;
import com.google.api.codegen.transformer.java.JavaGapicPackageTransformer;
import com.google.api.codegen.transformer.java.JavaSurfaceTestTransformer;
import com.google.api.codegen.util.CommonRenderingUtil;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/* Factory for DiscoGapicGenerators based on an id. */
public class DiscoGapicGeneratorFactory {

  /** Create the DiscoGapicGenerator based on the given id */
  public static List<CodeGenerator<?>> create(
      TargetLanguage language,
      DiscoApiModel model,
      GapicProductConfig productConfig,
      PackageMetadataConfig packageConfig,
      ArtifactFlags artifactFlags) {

    ArrayList<CodeGenerator<?>> generators = new ArrayList<>();

    // Please keep the following IDs in alphabetical order
    if (language.equals(JAVA)) {
      if (artifactFlags.surfaceGeneratorEnabled()) {
        if (artifactFlags.codeFilesEnabled()) {
          GapicCodePathMapper javaPathMapper =
              CommonGapicCodePathMapper.newBuilder()
                  .setPrefix("src/main/java")
                  .setShouldAppendPackage(true)
                  .build();
          List<ModelToViewTransformer<DiscoApiModel>> transformers =
              Arrays.asList(
                  new JavaDiscoGapicResourceNameToViewTransformer(javaPathMapper),
                  new JavaDiscoGapicSchemaToViewTransformer(javaPathMapper),
                  new JavaDiscoGapicRequestToViewTransformer(javaPathMapper),
                  new JavaDiscoGapicSurfaceTransformer(javaPathMapper));
          DiscoGapicGenerator generator =
              DiscoGapicGenerator.newBuilder()
                  .setDiscoApiModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                  .setModelToViewTransformers(transformers)
                  .build();

          generators.add(generator);
        }

        if (artifactFlags.packagingFilesEnabled()) {
          CodeGenerator metadataGenerator =
              DiscoGapicGenerator.newBuilder()
                  .setDiscoApiModel(model)
                  .setProductConfig(productConfig)
                  .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                  .setModelToViewTransformers(
                      Collections.singletonList(new JavaGapicPackageTransformer<>(packageConfig)))
                  .build();
          generators.add(metadataGenerator);
        }
      }

      if (artifactFlags.testGeneratorEnabled()) {
        GapicCodePathMapper javaTestPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("src/test/java")
                .setShouldAppendPackage(true)
                .build();
        CodeGenerator<?> testGenerator =
            DiscoGapicGenerator.newBuilder()
                .setDiscoApiModel(model)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new CommonRenderingUtil()))
                .setModelToViewTransformers(
                    Arrays.asList(
                        new JavaSurfaceTestTransformer<>(
                            javaTestPathMapper,
                            new JavaDiscoGapicSurfaceTransformer(javaTestPathMapper),
                            "java/http_test.snip")))
                .build();
        generators.add(testGenerator);
      }
      return generators;

    } else {
      throw new UnsupportedOperationException(
          "DiscoGapicGeneratorFactory: unsupported language \"" + language + "\"");
    }
  }
}
