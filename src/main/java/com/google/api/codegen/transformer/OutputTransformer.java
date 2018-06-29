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
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.api.codegen.viewmodel.OutputView.VariableView;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

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
    return toView(config, context, valueSet, new HashMap<>());
  }

  private static OutputView toView(
      OutputSpec config,
      MethodContext context,
      SampleValueSet valueSet,
      Map<String, TypeModel> localVars) {
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

    OutputView view = null;
    if (config.hasLoop()) {
      once.run();
      view = loopView(config.getLoop(), context, valueSet, localVars);
    }
    if (config.getPrintCount() > 0) {
      once.run();
      view = printView(config.getPrintList(), context, valueSet, localVars);
    }

    return Preconditions.checkNotNull(
        view,
        "%s:%s: one field of OutputSpec must be set",
        context.getMethodModel().getSimpleName(),
        valueSet.getId());
  }

  private static OutputView.PrintView printView(
      List<String> config,
      MethodContext context,
      SampleValueSet valueSet,
      Map<String, TypeModel> localVars) {
    Preconditions.checkArgument(
        !config.isEmpty(),
        "%s:%s: print spec cannot be empty",
        context.getMethodModel().getSimpleName(),
        valueSet.getId());

    return OutputView.PrintView.newBuilder()
        .format(context.getNamer().getPrintSpec(config.get(0)))
        .args(
            config
                .subList(1, config.size())
                .stream()
                .map(a -> accessor(a, context, valueSet, localVars, null))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  // accessor:
  //   identifier
  //   accessor '[' number ']'
  //   accessor '.' identifier

  /**
   * Parses config and returns accessor the config describes.
   *
   * <p>The config is type-checked. For example, indexing into a scalar field is not allowed. If
   * config refers to a local variable, the variable is looked up in {@code localVars}. If {@code
   * newVar} is not null, it is registered into {@code localVars}.
   */
  private static OutputView.VariableView accessor(
      String config,
      MethodContext context,
      SampleValueSet valueSet,
      Map<String, TypeModel> localVars,
      @Nullable String newVar) {

    OutputView.VariableView.Builder view = OutputView.VariableView.newBuilder();

    int cursor = 0;
    int end = identifier(config, cursor);

    String baseIdentifier = config.substring(cursor, end);
    TypeModel type;
    if (baseIdentifier.equals(RESPONSE_PLACEHOLDER)) {
      view.variable(context.getNamer().getSampleResponseVarName());
      type = context.getMethodModel().getOutputType();
    } else {
      view.variable(context.getNamer().localVarName(Name.from(baseIdentifier)));
      type =
          Preconditions.checkNotNull(
              localVars.get(baseIdentifier),
              "%s:%s: variable not defined: %s",
              context.getMethodModel().getSimpleName(),
              valueSet.getId(),
              baseIdentifier);
    }

    ImmutableList.Builder<String> accessors = ImmutableList.builder();
    while (end < config.length()) {
      if (config.charAt(end) == '.') {
        Preconditions.checkArgument(
            !type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not scalar",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.substring(0, end));

        cursor = end + 1;
        end = identifier(config, cursor);

        String fieldName = config.substring(cursor, end);
        FieldModel field =
            Preconditions.checkNotNull(
                type.getField(fieldName),
                "%s:%s: type %s does not have field %s",
                context.getMethodModel().getSimpleName(),
                valueSet.getId(),
                type,
                fieldName);

        type = field.getType();
        accessors.add(context.getNamer().getFieldGetFunctionName(field));
      } else if (config.charAt(end) == '[') {
        Preconditions.checkArgument(
            type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not a repeated field",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.substring(0, end));
        throw new UnsupportedOperationException("array indexing not supported yet");
      } else {
        throw new IllegalArgumentException(
            String.format("unexpected character: %c (%s)", config.charAt(end), config));
      }
    }

    if (newVar != null) {
      if (localVars.putIfAbsent(newVar, type) != null) {
        throw new IllegalStateException(
            String.format(
                "%s:%s: duplicated variable declaration not allowed: %s",
                context.getMethodModel().getSimpleName(), valueSet.getId(), newVar));
      }
    }

    return view.accessors(accessors.build()).build();
  }

  /** Returns the largest p such that s.substring(startsFrom, p) is an identifier. */
  private static int identifier(String s, int startFrom) {
    for (int p = startFrom; p < s.length(); p++) {
      char c = s.charAt(p);
      if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
        return p;
      }
    }
    return s.length();
  }

  private static OutputView.LoopView loopView(
      OutputSpec.LoopStatement loop,
      MethodContext context,
      SampleValueSet valueSet,
      Map<String, TypeModel> localVars) {

    OutputView.VariableView accessor =
        accessor(loop.getCollection(), context, valueSet, localVars, loop.getVariable());
    OutputView.LoopView ret =
        OutputView.LoopView.newBuilder()
            .variableType(
                context
                    .getNamer()
                    .getAndSaveTypeName(context.getTypeTable(), localVars.get(loop.getVariable())))
            .variableName(context.getNamer().localVarName(Name.from(loop.getVariable())))
            .collection(accessor)
            .body(
                loop.getBodyList()
                    .stream()
                    .map(body -> toView(body, context, valueSet, localVars))
                    .collect(ImmutableList.toImmutableList()))
            .build();

    // The variable is only visible within the loop, delete it from the table before we return.
    localVars.remove(loop.getVariable());
    return ret;
  }
}
