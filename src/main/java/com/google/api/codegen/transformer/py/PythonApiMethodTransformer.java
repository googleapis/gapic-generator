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

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.SampleTransformer;
import com.google.api.codegen.viewmodel.CallingForm;
import java.util.List;

/**
 * Contains the common logic for generating view models for GAPIC surface methods. This is used in
 * the generation of both the client libraries and of the standalone samples for each of the library
 * surface methods. Since in either case the resulting methods have one or more samples, this
 * populates the view models with appropriate sample view models.
 */
public class PythonApiMethodTransformer extends DynamicLangApiMethodTransformer {

  public PythonApiMethodTransformer(SampleTransformer sampleTransformer) {
    super(new PythonApiMethodParamTransformer(), sampleTransformer);
  }

  public List<CallingForm> getCallingForms(MethodContext context) {
    return CallingForm.getCallingForms(context, TargetLanguage.PYTHON);
  }
}
