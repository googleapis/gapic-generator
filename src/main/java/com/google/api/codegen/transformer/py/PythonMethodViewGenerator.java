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

import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.transformer.DynamicLangApiMethodTransformer;
import com.google.api.codegen.transformer.GapicInterfaceContext;
import com.google.api.codegen.viewmodel.OptionalArrayMethodView;
import com.google.common.collect.ImmutableList;
import java.util.List;

/**
 * Contains the common logic for generating view models for GAPIC surface methods. This is used in
 * the generation of both the client libraries and of the standalone samples for each of the library
 * surface methods. Since in either case the resulting methods have one or more samples, this
 * populates the view models with appropriate sample view models.
 */
public class PythonMethodViewGenerator {
  final DynamicLangApiMethodTransformer clientMethodTransformer;

  public PythonMethodViewGenerator(DynamicLangApiMethodTransformer transformer) {
    clientMethodTransformer = transformer;
  }

  public List<OptionalArrayMethodView> generateApiMethods(GapicInterfaceContext context) {
    // TODO(vchudnov-g): Move the signature selection logic (effectively, the ClientMethodType choice)
    // here from the .snip files where it currently resides.
    ImmutableList.Builder<OptionalArrayMethodView> apiMethodsAndSamples = ImmutableList.builder();

    for (MethodModel method : context.getSupportedMethods()) {
      OptionalArrayMethodView methodView =
          clientMethodTransformer.generateMethod(context.asDynamicMethodContext(method));
      apiMethodsAndSamples.add(methodView);
    }

    return apiMethodsAndSamples.build();
  }
}
