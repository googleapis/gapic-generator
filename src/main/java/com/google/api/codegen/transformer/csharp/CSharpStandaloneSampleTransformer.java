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
package com.google.api.codegen.transformer.csharp;

import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.StaticLangGapicSamplesTransformer;
import com.google.api.codegen.util.csharp.CSharpAliasMode;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.common.collect.ImmutableMap;

/** A transformer that generates C# standalone samples. */
public class CSharpStandaloneSampleTransformer extends StaticLangGapicSamplesTransformer {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "csharp/standalone_sample.snip";
  private static final ImmutableMap<CallingForm, ClientMethodType>
      CALLING_FORM_CLIENT_METHOD_TYPE_MAP =
          ImmutableMap.<CallingForm, ClientMethodType>builder()
              .put(CallingForm.Request, ClientMethodType.RequestObjectMethod)
              .put(CallingForm.RequestAsync, ClientMethodType.AsyncRequestObjectMethod)
              .put(CallingForm.RequestAsyncPagedAll, ClientMethodType.AsyncPagedRequestObjectMethod)
              .put(
                  CallingForm.RequestAsyncPagedPageSize,
                  ClientMethodType.AsyncPagedRequestObjectMethod)
              .put(CallingForm.RequestPaged, ClientMethodType.PagedRequestObjectMethod)
              .put(CallingForm.RequestPagedAll, ClientMethodType.PagedRequestObjectMethod)
              .put(CallingForm.RequestPagedPageSize, ClientMethodType.PagedRequestObjectMethod)
              .put(CallingForm.RequestStreamingBidi, ClientMethodType.RequestObjectMethod)
              .put(CallingForm.RequestStreamingServer, ClientMethodType.RequestObjectMethod)
              .put(CallingForm.Flattened, ClientMethodType.FlattenedMethod)
              .put(CallingForm.FlattenedPaged, ClientMethodType.PagedFlattenedMethod)
              .put(CallingForm.FlattenedPagedAll, ClientMethodType.PagedFlattenedMethod)
              .put(CallingForm.FlattenedPagedPageSize, ClientMethodType.PagedFlattenedMethod)
              .put(CallingForm.FlattenedAsync, ClientMethodType.FlattenedAsyncCallSettingsMethod)
              .put(CallingForm.FlattenedAsyncPaged, ClientMethodType.PagedFlattenedAsyncMethod)
              .put(CallingForm.FlattenedAsyncPagedAll, ClientMethodType.PagedFlattenedAsyncMethod)
              .put(
                  CallingForm.FlattenedAsyncPagedPageSize,
                  ClientMethodType.PagedFlattenedAsyncMethod)
              .put(CallingForm.FlattenedStreamingBidi, ClientMethodType.FlattenedMethod)
              .put(CallingForm.FlattenedStreamingServer, ClientMethodType.FlattenedMethod)
              .put(
                  CallingForm.LongRunningFlattenedPollUntilComplete,
                  ClientMethodType.OperationFlattenedMethod)
              .put(
                  CallingForm.LongRunningFlattenedAsyncPollUntilComplete,
                  ClientMethodType.OperationFlattenedMethod)
              .put(
                  CallingForm.LongRunningRequestPollUntilComplete,
                  ClientMethodType.OperationRequestObjectMethod)
              .put(
                  CallingForm.LongRunningRequestAsyncPollUntilComplete,
                  ClientMethodType.OperationRequestObjectMethod)
              .build();

  private static final CSharpAliasMode ALIAS_MODE = CSharpAliasMode.Off;
  private static final CSharpCommonTransformer csharpCommonTransformer =
      new CSharpCommonTransformer();

  private static final StaticLangApiMethodTransformer csharpApiMethodTransformer =
      new CSharpApiMethodTransformer(
          SampleTransformer.newBuilder()
              .sampleType(SampleSpec.SampleType.STANDALONE)
              .sampleImportTransformer(new CSharpSampleImportTransformer())
              .build());
  private static final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());

  public CSharpStandaloneSampleTransformer(GapicCodePathMapper pathMapper) {
    super(
        STANDALONE_SAMPLE_TEMPLATE_FILENAME,
        pathMapper,
        fileHeaderTransformer,
        csharpApiMethodTransformer,
        product -> new CSharpFeatureConfig(),
        product -> new CSharpSurfaceNamer(product.getPackageName(), ALIAS_MODE),
        pkg -> csharpCommonTransformer.createTypeTable(pkg + ".Samples", ALIAS_MODE));
  }

  @Override
  protected ClientMethodType fromCallingForm(CallingForm callingForm) {
    return CALLING_FORM_CLIENT_METHOD_TYPE_MAP.get(callingForm);
  }
}
