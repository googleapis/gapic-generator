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
package com.google.api.codegen.discogapic;

import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.PackageMetadataConfig;
import com.google.api.codegen.discogapic.transformer.DocumentToViewTransformer;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicRequestToViewTransformer;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicResourceNameToViewTransformer;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicSchemaToViewTransformer;
import com.google.api.codegen.discogapic.transformer.java.JavaDiscoGapicSurfaceTransformer;
import com.google.api.codegen.discovery.Document;
import com.google.api.codegen.gapic.CommonGapicCodePathMapper;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.gapic.GapicGeneratorConfig;
import com.google.api.codegen.rendering.CommonSnippetSetRunner;
import com.google.api.codegen.util.java.JavaRenderingUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.NotImplementedException;

public class MainDiscoGapicProviderFactory implements DiscoGapicProviderFactory {

  public static final String JAVA = "java";

  /** Create the DiscoGapicProvider based on the given id */
  public static List<DiscoGapicProvider> defaultCreate(
      Document document,
      GapicProductConfig productConfig,
      GapicGeneratorConfig generatorConfig,
      PackageMetadataConfig packageConfig) {

    ArrayList<DiscoGapicProvider> providers = new ArrayList<>();
    String id = generatorConfig.id();

    // Please keep the following IDs in alphabetical order
    if (id.equals(JAVA)) {
      if (generatorConfig.enableSurfaceGenerator()) {
        GapicCodePathMapper javaPathMapper =
            CommonGapicCodePathMapper.newBuilder()
                .setPrefix("src/main/java")
                .setShouldAppendPackage(true)
                .build();
        List<DocumentToViewTransformer> transformers =
            Arrays.asList(
                new JavaDiscoGapicResourceNameToViewTransformer(javaPathMapper, packageConfig),
                new JavaDiscoGapicSchemaToViewTransformer(javaPathMapper, packageConfig),
                new JavaDiscoGapicRequestToViewTransformer(javaPathMapper, packageConfig),
                new JavaDiscoGapicSurfaceTransformer(javaPathMapper, packageConfig));
        DiscoGapicProvider provider =
            DiscoGapicProvider.newBuilder()
                .setDocument(document)
                .setProductConfig(productConfig)
                .setSnippetSetRunner(new CommonSnippetSetRunner(new JavaRenderingUtil()))
                .setDocumentToViewTransformers(transformers)
                .build();

        providers.add(provider);
      }
      return providers;

    } else {
      throw new NotImplementedException("DiscoGapicProviderFactory: invalid id \"" + id + "\"");
    }
  }

  /** Create the DiscoGapicProvider based on the given id */
  @Override
  public List<DiscoGapicProvider> create(
      Document document,
      GapicProductConfig productConfig,
      GapicGeneratorConfig generatorConfig,
      PackageMetadataConfig packageConfig) {
    return defaultCreate(document, productConfig, generatorConfig, packageConfig);
  }
}
