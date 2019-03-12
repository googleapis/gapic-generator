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
package com.google.api.codegen.transformer.ruby;

public class RubyGapicSamplesTranformer {} // implements ModelToViewTransformer<ProtoApiModel> {

//   private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME =
// "ruby/standalone_sample.snip";
//   private static final SampleType sampleType = SampleType.STANDALONE;

//   private final RubyImportSectionTransformer importSectionTransformer =
//       new RubyImportSectionTransformer();
//   private final FileHeaderTransformer fileHeaderTransformer =
//       new FileHeaderTransformer(importSectionTransformer);
//   private final DynamicLangApiMethodTransformer apiMethodTransformer =
//       new DynamicLangApiMethodTransformer(
//           new RubyApiMethodParamTransformer(),
//           SampleTransformer.newBuilder()
//               .initCodeTransformer(new InitCodeTransformer(importSectionTransformer))
//               .sampleType(sampleType)
//               .sampleImportTransformer(new RubySampleImportTransformer(importSectionTransformer))
//               .build());
//   private final GapicCodePathMapper pathMapper;
//   private final PackageMetadataConfig packageConfig;

//   public RubyGapicSamplesTranformer(
//       GapicCodePathMapper pathMapper, PackageMetadataConfig packageConfig) {
//     this.pathMapper = pathMapper;
//     this.packageConfig = packageConfig;
//   }

//   @Override
//   public List<String> getTemplateFileNames() {
//     return ImmutableList.of(STANDALONE_SAMPLE_TEMPLATE_FILENAME);
//   }

//   @Override
//   public List<ViewModel> transform(ProtoApiModel apiModel, GapicProductConfig productConfig) {
//     return Streams.stream(apiModel.getInterfaces(productConfig))
//         .filter(i -> productConfig.hasInterfaceConfig())
//         .flatMap(i -> generateSampleFiles(createContext(i, productConfig)).stream())
//         .collect(ImmutableList.toImmutableList());
//   }

//   private List<ViewModel> generateSampleFiles(
//       GapicInterfaceContext context, boolean hasMultipleServices) {
//     SurfaceNamer namer = context.getNamer();
//     SampleFileRegistry generatedSamples = new SampleFileRegistry();
//     String outputPath = GapicCodePathMapper.SAMPLE_DIRECTORY;
//   }
// }
