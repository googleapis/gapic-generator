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
package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.gapic.GapicCodePathMapper;
import com.google.api.codegen.transformer.FileHeaderTransformer;
import com.google.api.codegen.transformer.ModelTypeTable;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.transformer.StandardImportSectionTransformer;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.transformer.StaticLangGapicSamplesTransformer;
import com.google.api.codegen.util.java.JavaTypeTable;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.common.collect.ImmutableMap;

/** A transformer that generates C# standalone samples. */
public class JavaGapicSamplesTransformer extends StaticLangGapicSamplesTransformer {

  private static final String STANDALONE_SAMPLE_TEMPLATE_FILENAME = "java/standalone_sample.snip";

  private static final ImmutableMap<CallingForm, ClientMethodType>
      CALLING_FORM_CLIENT_METHOD_TYPE_MAP =
          ImmutableMap.<CallingForm, ClientMethodType>builder()
              .put(CallingForm.Request, ClientMethodType.RequestObjectMethod)
              .put(CallingForm.RequestPaged, ClientMethodType.PagedRequestObjectMethod)
              .put(CallingForm.Flattened, ClientMethodType.FlattenedMethod)
              .put(CallingForm.FlattenedPaged, ClientMethodType.PagedFlattenedMethod)
              .put(CallingForm.Callable, ClientMethodType.CallableMethod)
              .put(CallingForm.CallableList, ClientMethodType.UnpagedListCallableMethod)
              .put(CallingForm.CallablePaged, ClientMethodType.PagedCallableMethod)
              .put(CallingForm.CallableStreamingBidi, ClientMethodType.CallableMethod)
              .put(CallingForm.CallableStreamingClient, ClientMethodType.CallableMethod)
              .put(CallingForm.CallableStreamingServer, ClientMethodType.CallableMethod)
              .put(CallingForm.LongRunningCallable, ClientMethodType.OperationCallableMethod)
              .put(
                  CallingForm.LongRunningRequestAsync,
                  ClientMethodType.AsyncOperationRequestObjectMethod)
              .build();

  private static final StaticLangApiMethodTransformer apiMethodTransformer =
      new JavaApiMethodTransformer(
          SampleTransformer.newBuilder()
              .sampleType(SampleSpec.SampleType.STANDALONE)
              .sampleImportTransformer(new JavaSampleImportTransformer())
              .build());
  private static final FileHeaderTransformer fileHeaderTransformer =
      new FileHeaderTransformer(new StandardImportSectionTransformer());

  public JavaGapicSamplesTransformer(GapicCodePathMapper pathMapper) {
    super(
        STANDALONE_SAMPLE_TEMPLATE_FILENAME,
        pathMapper,
        fileHeaderTransformer,
        apiMethodTransformer,
        p -> JavaFeatureConfig.create(p),
        p -> new JavaSurfaceNamer(p.getPackageName(), p.getPackageName()),
        p ->
            new ModelTypeTable(
                new JavaTypeTable(JavaSurfaceNamer.getExamplePackageName(p)),
                new JavaModelTypeNameConverter(JavaSurfaceNamer.getExamplePackageName(p))));
  }

  @Override
  protected ClientMethodType fromCallingForm(CallingForm callingForm) {
    return CALLING_FORM_CLIENT_METHOD_TYPE_MAP.get(callingForm);
  }
}
