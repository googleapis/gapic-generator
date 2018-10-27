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

import static com.google.api.codegen.viewmodel.PrintArgView.ArgSegmentView;
import static com.google.api.codegen.viewmodel.PrintArgView.TextSegmentView;
import static com.google.api.codegen.viewmodel.PrintArgView.VariableSegmentView;
import static com.google.common.base.Preconditions.checkArgument;

import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.OutputTransformer;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.codegen.viewmodel.PrintArgView;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.api.tools.framework.model.ProtoFile;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.collect.ImmutableList;
import java.util.ArrayList;
import java.util.List;

public class PythonSamplePrintArgTransformer implements OutputTransformer.PrintArgTransformer {

  @Override
  public PrintArgView generatePrintArg(
      MethodContext context, OutputView.VariableView variableView) {
    if (variableView.type() != null && variableView.type().isEnum()) {
      ImmutableList<ArgSegmentView> segments =
          ImmutableList.of(
              TextSegmentView.of(getEnumTypeClassName(context, variableView.type()) + "("),
              VariableSegmentView.of(variableView),
              TextSegmentView.of(").name"));
      return PrintArgView.newBuilder().segments(segments).build();
    } else {
      return PrintArgView.newBuilder()
          .segments(ImmutableList.<ArgSegmentView>of(VariableSegmentView.of(variableView)))
          .build();
    }
  }

  private static String getEnumTypeClassName(MethodContext context, TypeModel type) {
    checkArgument(
        type instanceof ProtoTypeRef,
        "%s: type %s is not a proto type",
        context.getMethodModel().getSimpleName(),
        type);
    checkArgument(
        ((ProtoTypeRef) type).isEnum(),
        "%s: type %s is not an enum type",
        context.getMethodModel().getSimpleName(),
        type);
    TypeRef protoType = ((ProtoTypeRef) type).getProtoType();
    ProtoElement t = protoType.getEnumType();
    List<String> names = new ArrayList<>();
    while (!(t instanceof ProtoFile)) {
      names.add(t.getSimpleName());
      t = t.getParent();
    }
    names.add("enums");
    StringBuilder builder = new StringBuilder();
    for (int i = names.size() - 1; i >= 1; i--) {
      builder.append(names.get(i));
      builder.append(".");
    }
    builder.append(names.get(0));
    return builder.toString();
  }
}
