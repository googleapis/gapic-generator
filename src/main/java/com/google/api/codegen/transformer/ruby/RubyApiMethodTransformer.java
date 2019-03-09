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
package com.google.api.codegen.transformer;

import com.google.api.codegen.common.TargetLanguage;
import com.google.api.codegen.config.GapicMethodContext;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import java.util.List;

// LangMethodViewGenerator is a bit redundant with quite a few repeated code.
// Trying to adopt a different way in Ruby. If it works, consider refactor existing languages
// to use the same pattern.
public class RubyApiMethodTransformer extends DynamicLangApiMethodTransformer {

  public RubyApiMethodTransformer(ApiMethodParamTransformer apiMethodParamTransformer) {
    super(apiMethodParamTransformer, SampleTransformer.create(SampleType.IN_CODE));
  }

  public RubyApiMethodTransformer(
      ApiMethodParamTransformer apiMethodParamTransformer, SampleTransformer sampleTransformer) {
    super(apiMethodParamTransformer, sampleTransformer);
  }

  // This Method can potentially go back to DynamicLangApiMethodTransformer
  @Override
  public List<OptionalArrayMethodView> generateApiMethods(
      GapicInterfaceContext context, boolean packageHasMultipleServices) {
    // ...
  }

  @Override
  public List<OptionalArrayMethodView> generateOneApiMethod(
      GapicMethodContext context, boolean packageHasMultipleServices) {
    List<CallingForm> callingForms = CallingForm.getCallingForms(context, TargetLanguage.RUBY);
    if (methodContext.getMethodConfig().isPageStreaming()) {
      return generatePagedStreamingMethod(
          methodContext, null, packageHasMultipleServices, callingForms);
    } else if (methodContext.isLongRunningMethodContext()) {
      return clientMethodTransformer.generateLongRunningMethod(
          methodContext, null, packageHasMultipleServices, callingForms);
    } else {
      return clientMethodTransformer.generateRequestMethod(
          methodContext, initContext, packageHasMultipleServices, callingForms);
    }
  }
}
