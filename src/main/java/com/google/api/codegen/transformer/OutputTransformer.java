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
package com.google.api.codegen.transformer;

import com.google.api.codegen.OutputSpec;
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.List;

class OutputTransformer {
  private static final String RESPONSE_PLACEHOLDER = "$resp";

  static List<OutputSpec> defaultOutputSpecs(MethodModel method) {
    if (method.isOutputTypeEmpty()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(
        OutputSpec.newBuilder().addPrint("%s").addPrint(RESPONSE_PLACEHOLDER).build());
  }

  static OutputView toView(OutputSpec config, MethodContext context, SampleValueSet valueSet) {
    Runnable once =
        new Runnable() {
          boolean ran;

          @Override
          public void run() {
            Preconditions.checkArgument(
                !ran,
                "%s:%s: only one field of OutputSpec may be set",
                context.getMethodModel().getSimpleName(),
                valueSet.getId());
            ran = true;
          }
        };

    OutputView.Builder view = OutputView.newBuilder();
    if (config.hasLoop()) {
      once.run();
      throw new UnsupportedOperationException("loop not implemented yet");
    }
    if (config.getPrintCount() > 0) {
      once.run();
      view.print(toView(config.getPrintList(), context, valueSet)).kind(OutputView.Kind.PRINT);
    }
    return view.build();
  }

  private static OutputView.PrintView toView(
      List<String> config, MethodContext context, SampleValueSet valueSet) {
    Preconditions.checkArgument(
        !config.isEmpty(),
        "%s:%s: print spec cannot be empty",
        context.getMethodModel().getSimpleName(),
        valueSet.getId());

    return OutputView.PrintView.newBuilder()
        .printSpec(context.getNamer().getPrintSpec(config.get(0), numSub))
        .printArgs(
            config
                .subList(1, config.size())
                .stream()
                .map(a -> accessor(a, context, valueSet))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private static OutputView.PrintArgView accessor(
      String config, MethodContext context, SampleValueSet valueSet) {
    String[] configElems = config.split("\\.");
    Preconditions.checkArgument(
        configElems.length != 0,
        "%s:%s: field string cannot be empty",
        context.getMethodModel().getSimpleName(),
        valueSet.getId());

    OutputView.PrintArgView.Builder view = OutputView.PrintArgView.newBuilder();
    TypeModel type;
    if (configElems[0].equals(RESPONSE_PLACEHOLDER)) {
      view.variable(context.getNamer().getSampleResponseVarName());
      type = context.getMethodModel().getOutputType();
    } else {
      throw new UnsupportedOperationException("local variable not implemented yet");
    }

    ImmutableList.Builder<String> accessors = ImmutableList.builder();
    for (int i = 1; i < configElems.length; i++) {
      String fieldName = configElems[i];
      FieldModel field =
          Preconditions.checkNotNull(
              type.getField(fieldName),
              "%s:%s: type %s does not have field %s",
              context.getMethodModel().getSimpleName(),
              valueSet.getId(),
              type,
              fieldName);
      Preconditions.checkArgument(
          !field.isRepeated() && !field.isMap(),
          "%s:%s: %s.%s is not scalar",
          context.getMethodModel().getSimpleName(),
          valueSet.getId(),
          type,
          fieldName);
      type = field.getType();
      accessors.add(context.getNamer().getFieldGetFunctionName(field));
    }
    return view.accessors(accessors.build()).build();
  }
}
