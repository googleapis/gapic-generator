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
package com.google.api.codegen.transformer;

import com.google.api.codegen.config.GapicInterfaceContext;
import com.google.api.codegen.config.GapicProductConfig;
import com.google.api.codegen.config.InterfaceContext;
import com.google.api.codegen.config.ProtoApiModel;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.viewmodel.ViewModel;
import com.google.api.codegen.viewmodel.metadata.SampleManifestView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

public class SampleManifestTransformer implements ModelToViewTransformer<ProtoApiModel> {

  private static final String MANIFEST_SCHEMA_VERSION = "3";
  private static final String TEMPLATE_NAME = "metadatagen/sample_manifest.snip";

  private final SampleMetadataNamer metadataNamer;
  private final Function<GapicProductConfig, FeatureConfig> newFeatureConfig;
  private final Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer;
  private final Function<String, ModelTypeTable> newTypeTable;

  public SampleManifestTransformer(
      SampleMetadataNamer metadataNamer,
      Function<GapicProductConfig, FeatureConfig> newFeatureConfig,
      Function<GapicProductConfig, SurfaceNamer> newSurfaceNamer,
      Function<String, ModelTypeTable> newTypeTable) {
    this.metadataNamer = metadataNamer;
    this.newFeatureConfig = newFeatureConfig;
    this.newSurfaceNamer = newSurfaceNamer;
    this.newTypeTable = newTypeTable;
  }

  @Override
  public List<ViewModel> transform(ProtoApiModel model, GapicProductConfig productConfig) {
    SurfaceNamer surfaceNamer = newSurfaceNamer.apply(productConfig);
    ModelTypeTable typeTable = newTypeTable.apply(productConfig.getPackageName());
    FeatureConfig featureConfig = newFeatureConfig.apply(productConfig);

    ImmutableList.Builder<SampleManifestView.SampleEntry> entries = ImmutableList.builder();
    List<InterfaceContext> interfaceContexts =
        Streams.stream(model.getInterfaces(productConfig))
            .filter(iface -> productConfig.hasInterfaceConfig(iface))
            .map(
                iface ->
                    GapicInterfaceContext.create(
                        iface, productConfig, typeTable, surfaceNamer, featureConfig))
            .collect(ImmutableList.toImmutableList());

    List<SampleContext> sampleContexts =
        metadataNamer.getSampleContexts(interfaceContexts, productConfig);
    for (SampleContext context : sampleContexts) {
      String sample = context.uniqueSampleId();
      String path = metadataNamer.getSamplePath(sample);
      String regionTag = context.sampleConfig().regionTag();
      entries.add(SampleManifestView.SampleEntry.create(sample, path, regionTag));
    }
    SampleManifestView.Builder sampleManifestView = SampleManifestView.newBuilder();
    sampleManifestView.environment(metadataNamer.getEnvironment());
    sampleManifestView.bin(metadataNamer.getBin());
    sampleManifestView.basePath(metadataNamer.getBasePath(productConfig));
    sampleManifestView.invocation(metadataNamer.getInvocation());
    sampleManifestView.schemaVersion(MANIFEST_SCHEMA_VERSION);
    sampleManifestView.sampleEntries(entries.build());
    sampleManifestView.outputPath("");
    sampleManifestView.templateFileName(TEMPLATE_NAME);
    return Collections.singletonList(sampleManifestView.build());
  }

  @Override
  public List<String> getTemplateFileNames() {
    return Collections.singletonList(TEMPLATE_NAME);
  }
}
