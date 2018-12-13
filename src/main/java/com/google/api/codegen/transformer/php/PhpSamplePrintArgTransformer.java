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
package com.google.api.codegen.transformer.php;

import static com.google.api.codegen.viewmodel.PrintArgView.ArgSegmentView;
import static com.google.api.codegen.viewmodel.PrintArgView.TextSegmentView;
import static com.google.api.codegen.viewmodel.PrintArgView.VariableSegmentView;

import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.OutputTransformer;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.codegen.viewmodel.PrintArgView;
import com.google.common.collect.ImmutableList;

public class PhpSamplePrintArgTransformer implements OutputTransformer.PrintArgTransformer {

  /**
   * Converts the VariableView to a PrintArgView that knows how to print proto types nicely. To
   * print an enum, we first call SomeEnum::name(x) to convert the enum value to its name. To print
   * a message, we first call `print_r(x, true)` to convert the object to a meaningful string.
   */
  @Override
  public PrintArgView generatePrintArg(
      MethodContext context, OutputView.VariableView variableView) {
    TypeModel type = variableView.type();
    if (type == null || type.isPrimitive()) {
      return PrintArgView.newBuilder()
          .segments(ImmutableList.<ArgSegmentView>of(VariableSegmentView.of(variableView)))
          .build();
    }
    ImmutableList<ArgSegmentView> segments;
    if (type.isMessage()) {
      segments =
          ImmutableList.of(
              TextSegmentView.of("print_r("),
              VariableSegmentView.of(variableView),
              TextSegmentView.of(", true)"));
    } else if (type.isEnum()) {
      segments =
          ImmutableList.of(
              TextSegmentView.of(
                  context.getNamer().getAndSaveTypeName(context.getTypeTable(), type) + "::name("),
              VariableSegmentView.of(variableView),
              TextSegmentView.of(")"));
    } else {
      throw new IllegalArgumentException("unhandled type: " + type.getTypeName());
    }
    return PrintArgView.newBuilder().segments(segments).build();
  }
}
