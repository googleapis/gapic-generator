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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

  static List<OutputView> toViews(
      List<OutputSpec> configs, MethodContext context, SampleValueSet valueSet) {
    ScopeTable localVars = new ScopeTable();
    return configs
        .stream()
        .map(s -> OutputTransformer.toView(s, context, valueSet, localVars))
        .collect(ImmutableList.toImmutableList());
  }

  private static OutputView toView(
      OutputSpec config, MethodContext context, SampleValueSet valueSet, ScopeTable localVars) {
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
    if (!config.getDefine().isEmpty()) {
      once.run();
      view = defineView(config.getDefine(), context, valueSet, localVars);
    }

    return Preconditions.checkNotNull(
        view,
        "%s:%s: one field of OutputSpec must be set",
        context.getMethodModel().getSimpleName(),
        valueSet.getId());
  }

  private static OutputView.PrintView printView(
      List<String> config, MethodContext context, SampleValueSet valueSet, ScopeTable localVars) {
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
                .map(a -> accessor(a, context, valueSet, localVars))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private static OutputView.LoopView loopView(
      OutputSpec.LoopStatement loop,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars) {

    ScopeTable scope = localVars.newChild();
    OutputView.VariableView accessor =
        accessorNewVariable(loop.getCollection(), context, valueSet, scope, loop.getVariable());
    OutputView.LoopView ret =
        OutputView.LoopView.newBuilder()
            .variableType(
                context
                    .getNamer()
                    .getAndSaveTypeName(context.getTypeTable(), scope.get(loop.getVariable())))
            .variableName(context.getNamer().localVarName(Name.from(loop.getVariable())))
            .collection(accessor)
            .body(
                loop.getBodyList()
                    .stream()
                    .map(body -> toView(body, context, valueSet, scope))
                    .collect(ImmutableList.toImmutableList()))
            .build();
    return ret;
  }

  private static OutputView.DefineView defineView(
      String definition, MethodContext context, SampleValueSet valueSet, ScopeTable localVars) {
    int p = identifierEnd(definition, 0);
    String identifier = definition.substring(0, p);
    Preconditions.checkArgument(
        definition.startsWith("=", p),
        "%s:%s invalid definition, expecting '=': %s",
        context.getMethodModel().getSimpleName(),
        valueSet.getId(),
        definition);
    OutputView.VariableView reference =
        accessorNewVariable(definition.substring(p + 1), context, valueSet, localVars, identifier);
    return OutputView.DefineView.newBuilder()
        .variableType(
            context
                .getNamer()
                .getAndSaveTypeName(context.getTypeTable(), localVars.get(identifier)))
        .variableName(context.getNamer().localVarName(Name.from(identifier)))
        .reference(reference)
        .build();
  }

  private static OutputView.VariableView accessor(
      String config, MethodContext context, SampleValueSet valueSet, ScopeTable localVars) {
    return accessorNewVariable(config, context, valueSet, localVars, null);
  }

  /**
   * Parses config and returns accessor the config describes.
   *
   * <p>The config is type-checked. For example, indexing into a scalar field is not allowed. If
   * config refers to a local variable, the variable is looked up in {@code localVars}. If {@code
   * newVar} is not null, it is registered into {@code localVars}.
   *
   * <pre><code>
   * Syntax:
   * accessor:
   *   identifier
   *   accessor '[' number ']'
   *   accessor '.' identifier
   * </code></pre>
   */
  private static OutputView.VariableView accessorNewVariable(
      String config,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars,
      @Nullable String newVar) {

    OutputView.VariableView.Builder view = OutputView.VariableView.newBuilder();

    int cursor = 0;
    int end = identifierEnd(config, cursor);

    String baseIdentifier = config.substring(cursor, end);
    TypeModel type;
    if (baseIdentifier.equals(RESPONSE_PLACEHOLDER)) {
      view.variable(context.getNamer().getSampleResponseVarName(context));

      if (context.getMethodConfig().getPageStreaming() != null) {
        type =
            context
                .getMethodConfig()
                .getPageStreaming()
                .getResourcesFieldConfig()
                .getField()
                .getType()
                .makeOptional();
      } else {
        type = context.getMethodModel().getOutputType();
      }
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
            type.isMessage(),
            "%s:%s: %s is not a message",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.substring(0, end));
        Preconditions.checkArgument(
            !type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not scalar",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.substring(0, end));

        cursor = end + 1;
        end = identifierEnd(config, cursor);

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
      if (!localVars.put(newVar, type)) {
        throw new IllegalStateException(
            String.format(
                "%s:%s: duplicated variable declaration not allowed: %s",
                context.getMethodModel().getSimpleName(), valueSet.getId(), newVar));
      }
    }

    return view.accessors(accessors.build()).build();
  }

  /** Returns the largest p such that s.substring(startsFrom, p) is an identifier. */
  private static int identifierEnd(String s, int startFrom) {
    for (int p = startFrom; p < s.length(); p++) {
      char c = s.charAt(p);
      if (!Character.isLetterOrDigit(c) && c != '_' && c != '$') {
        Preconditions.checkArgument(
            p != startFrom, "not an identifier: %s", s.substring(startFrom));
        return p;
      }
    }
    return s.length();
  }

  /**
   * Tracks the variables that were defined for this sample and the subset that is currently in
   * scope. We do this by maintaining two scopes: sample and local.
   *
   * <p>Sample keeps track of all variable declared by the output specs. We need this because
   * variables are function-scoped in many dynamic languages, and we should error if the spec
   * declares a variable with the same name twice.
   *
   * <p>Local keeps track of variables in the current block. We need this because variables are
   * block-scoped in many static languages, and we should error if the spec uses a variable not in
   * the nested blocks currently in scope.
   */
  private static class ScopeTable {
    private final Set<String> sample;
    @Nullable private final ScopeTable parent;
    private final Map<String, TypeModel> scope = new HashMap<>();

    private ScopeTable() {
      sample = new HashSet<>();
      parent = null;
    }

    private ScopeTable(ScopeTable parent) {
      Preconditions.checkNotNull(parent);
      sample = parent.sample;
      this.parent = parent;
    }

    /** Gets the type of the variable. Returns null if the variable is not found. */
    private TypeModel get(String name) {
      ScopeTable table = this;
      while (table != null) {
        TypeModel type = table.scope.get(name);
        if (type != null) {
          return type;
        }
        table = table.parent;
      }
      return null;
    }

    /**
     * Associates a variable with a type in the current scope. Returns whether the insertion was
     * successful.
     */
    private boolean put(String name, TypeModel type) {
      if (!sample.add(name)) {
        return false;
      }
      scope.put(name, type);
      return true;
    }

    private ScopeTable newChild() {
      return new ScopeTable(this);
    }
  }
}
