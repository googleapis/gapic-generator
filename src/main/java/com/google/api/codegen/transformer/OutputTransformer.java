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
import com.google.api.codegen.util.Scanner;
import com.google.api.codegen.viewmodel.CallingForm;
import com.google.api.codegen.viewmodel.ImportFileView;
import com.google.api.codegen.viewmodel.OutputView;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public class OutputTransformer {
  private static final String RESPONSE_PLACEHOLDER = "$resp";

  private final OutputImportTransformer importTransformer;

  public OutputTransformer() {
    this.importTransformer = new OutputImportTransformer() {};
  }

  public OutputTransformer(OutputImportTransformer importTransformer) {
    this.importTransformer = importTransformer;
  }

  /**
   * Tranformer that takes the a list of {@code OutputView(s)} and generate imports needed by the
   * output part. The returned list of {@code ImportFileView(s)} will be passed to {@code
   * MethodSampleView}.
   */
  public static interface OutputImportTransformer {

    public default ImmutableList<ImportFileView> generateOutputImports(
        MethodContext context, List<OutputView> outputViews) {
      return ImmutableList.<ImportFileView>of();
    }
  }

  public OutputImportTransformer getOutputImportTransformer() {
    return this.importTransformer;
  }

  static List<OutputSpec> defaultOutputSpecs(MethodModel method) {
    if (method.isOutputTypeEmpty()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(
        OutputSpec.newBuilder().addPrint("%s").addPrint(RESPONSE_PLACEHOLDER).build());
  }

  ImmutableList<OutputView> toViews(
      List<OutputSpec> configs, MethodContext context, SampleValueSet valueSet, CallingForm form) {
    ScopeTable localVars = new ScopeTable();
    return configs
        .stream()
        .map(s -> toView(s, context, valueSet, localVars, form))
        .collect(ImmutableList.toImmutableList());
  }

  private OutputView toView(
      OutputSpec config,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars,
      CallingForm form) {
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
      view = loopView(config.getLoop(), context, valueSet, localVars, form);
    }
    if (config.getPrintCount() > 0) {
      once.run();
      view = printView(config.getPrintList(), context, valueSet, localVars, form);
    }
    if (!config.getDefine().isEmpty()) {
      once.run();
      view = defineView(new Scanner(config.getDefine()), context, valueSet, localVars, form);
    }
    if (config.getCommentCount() > 0) {
      once.run();
      view = commentView(config.getCommentList(), context);
    }

    return Preconditions.checkNotNull(
        view,
        "%s:%s: one field of OutputSpec must be set",
        context.getMethodModel().getSimpleName(),
        valueSet.getId());
  }

  private OutputView.PrintView printView(
      List<String> config,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars,
      CallingForm form) {
    Preconditions.checkArgument(
        !config.isEmpty(),
        "%s:%s: print spec cannot be empty",
        context.getMethodModel().getSimpleName(),
        valueSet.getId());
    String format = config.get(0);
    ImmutableList.Builder<OutputView.PrintArgView> argsBuilder = ImmutableList.builder();
    for (String path : config.subList(1, config.size())) {
      OutputView.VariableView variable =
          accessor(new Scanner(path), context, valueSet, localVars, form);
      TypeModel type = variable.type();
      String formattedArg =
          context
              .getNamer()
              .getFormattedPrintArgName(type, variable.variable(), variable.accessors());
      OutputView.PrintArgView arg =
          OutputView.PrintArgView.newBuilder().type(type).formattedName(formattedArg).build();
      argsBuilder.add(arg);
    }
    ImmutableList<OutputView.PrintArgView> args = argsBuilder.build();
    List<String> specs =
        context
            .getNamer()
            .getPrintSpecs(
                format, args.stream().map(arg -> arg.formattedName()).collect(Collectors.toList()));
    return OutputView.PrintView.newBuilder().format(specs.get(0)).args(args).build();
  }

  private OutputView.LoopView loopView(
      OutputSpec.LoopStatement loop,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars,
      CallingForm form) {

    ScopeTable scope = localVars.newChild();
    String loopVariable = loop.getVariable();
    assertIdentifierNotUsed(
        loopVariable, context.getMethodModel().getSimpleName(), valueSet.getId(), context, form);
    OutputView.VariableView accessor =
        accessorNewVariable(
            new Scanner(loop.getCollection()), context, valueSet, scope, loopVariable, true, form);
    OutputView.LoopView ret =
        OutputView.LoopView.newBuilder()
            .variableType(scope.getTypeName(loopVariable))
            .variableName(context.getNamer().localVarName(Name.from(loopVariable)))
            .collection(accessor)
            .body(
                loop.getBodyList()
                    .stream()
                    .map(body -> toView(body, context, valueSet, scope, form))
                    .collect(ImmutableList.toImmutableList()))
            .build();
    return ret;
  }

  private OutputView.DefineView defineView(
      Scanner definition,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars,
      CallingForm form) {
    Preconditions.checkArgument(
        definition.scan() == Scanner.IDENT,
        "%s:%s: expected identifier: %s",
        context.getMethodModel().getSimpleName(),
        valueSet.getId(),
        definition.input());
    String identifier = definition.tokenStr();
    assertIdentifierNotUsed(
        identifier, context.getMethodModel().getSimpleName(), valueSet.getId(), context, form);
    Preconditions.checkArgument(
        definition.scan() == '=',
        "%s:%s invalid definition, expecting '=': %s",
        context.getMethodModel().getSimpleName(),
        valueSet.getId(),
        definition.input());
    OutputView.VariableView reference =
        accessorNewVariable(definition, context, valueSet, localVars, identifier, false, form);
    return OutputView.DefineView.newBuilder()
        .variableType(localVars.getTypeName(identifier))
        .variableName(context.getNamer().localVarName(Name.from(identifier)))
        .reference(reference)
        .build();
  }

  private OutputView.CommentView commentView(List<String> configs, MethodContext context) {
    String comment = configs.get(0);
    Object[] args =
        configs
            .subList(1, configs.size())
            .stream()
            .map(c -> context.getNamer().localVarName(Name.anyLower(c)))
            .toArray(Object[]::new);
    String formattedComment = String.format(comment, args);
    ImmutableList<String> lines = ImmutableList.copyOf(formattedComment.split("\\n", -1));
    return OutputView.CommentView.newBuilder().lines(lines).build();
  }

  private static OutputView.VariableView accessor(
      Scanner config,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars,
      CallingForm form) {
    return accessorNewVariable(config, context, valueSet, localVars, null, false, form);
  }

  /**
   * Parses config and returns accessor the config describes.
   *
   * <p>The config is type-checked. For example, indexing into a scalar field is not allowed. If
   * config refers to a local variable, the variable is looked up in {@code localVars}. If {@code
   * newVar} is not null, it is registered into {@code localVars}. If {@code
   * scalarTypeForCollection} is true, the config must resolve to a collection type, and the type of
   * the elements is registered instead.
   *
   * <pre><code>
   * Syntax:
   * accessor:
   *   identifier
   *   accessor '[' number ']'
   *   accessor '.' identifier
   * </code></pre>
   */
  @VisibleForTesting
  static OutputView.VariableView accessorNewVariable(
      Scanner config,
      MethodContext context,
      SampleValueSet valueSet,
      ScopeTable localVars,
      @Nullable String newVar,
      boolean scalarTypeForCollection,
      CallingForm form) {

    OutputView.VariableView.Builder view = OutputView.VariableView.newBuilder();

    Preconditions.checkArgument(
        config.scan() == Scanner.IDENT,
        "%s:%s: expected identifier: %s",
        context.getMethodModel().getSimpleName(),
        valueSet.getId(),
        config.input());
    String baseIdentifier = config.tokenStr();

    TypeModel type = null;
    String typeName = null;
    if (baseIdentifier.equals(RESPONSE_PLACEHOLDER)) {
      view.variable(context.getNamer().getSampleResponseVarName(context, form));
      boolean pageStreaming = context.getMethodConfig().getPageStreaming() != null;
      boolean pageStreamingAndUseResourceName =
          pageStreaming
              && context
                  .getFeatureConfig()
                  .useResourceNameFormatOption(
                      context.getMethodConfig().getPageStreaming().getResourcesFieldConfig());

      // Compute the resource name format of output type and store that in typeName
      if (pageStreamingAndUseResourceName) {
        typeName =
            context
                .getNamer()
                .getAndSaveElementResourceTypeName(
                    context.getTypeTable(),
                    context.getMethodConfig().getPageStreaming().getResourcesFieldConfig());
      } else if (pageStreaming) {
        type =
            context
                .getMethodConfig()
                .getPageStreaming()
                .getResourcesFieldConfig()
                .getField()
                .getType()
                .makeOptional();
      } else if (context.isLongRunningMethodContext()) {
        type = context.getLongRunningConfig().getReturnType();
      } else {
        type = context.getMethodModel().getOutputType();
      }
    } else {
      // Referencing the value of a local variable
      view.variable(context.getNamer().localVarName(Name.from(baseIdentifier)));
      type = localVars.getTypeModel(baseIdentifier);
      if (type == null) {
        typeName =
            Preconditions.checkNotNull(
                localVars.getTypeName(baseIdentifier),
                "%s:%s: variable not defined: %s",
                context.getMethodModel().getSimpleName(),
                valueSet.getId(),
                baseIdentifier);
      }
    }

    int token;
    // The accessors include not only the field names but also language-specific
    // syntax. e.g., `->field()` in PHP and `.field()` in Java.
    ImmutableList.Builder<String> accessors = ImmutableList.builder();
    while ((token = config.scan()) != Scanner.EOF) {
      if (token == '.') {
        // TODO(hzyi): add support for accessing fields of resource name types
        Preconditions.checkArgument(
            type != null,
            "%s:%s: accessing a field of a resource name is not currently supported",
            context.getMethodModel().getSimpleName(),
            valueSet.getId());
        Preconditions.checkArgument(
            type.isMessage(),
            "%s:%s: %s is not a message",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());
        Preconditions.checkArgument(
            !type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not scalar",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());

        Preconditions.checkArgument(
            config.scan() == Scanner.IDENT,
            "%s:%s: expected identifier: %s",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());

        String fieldName = config.tokenStr();
        FieldModel field =
            Preconditions.checkNotNull(
                type.getField(fieldName),
                "%s:%s: type %s does not have field %s",
                context.getMethodModel().getSimpleName(),
                valueSet.getId(),
                type,
                fieldName);

        type = field.getType();
        accessors.add(context.getNamer().getFieldAccessorName(field));
      } else if (token == '[') {
        Preconditions.checkArgument(
            type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not a repeated field",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());
        Preconditions.checkArgument(
            config.scan() == Scanner.INT,
            "%s:%s: expected int in index expression: %s",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());

        type = type.makeOptional();
        int index = Integer.parseInt(config.tokenStr());
        accessors.add(context.getNamer().getIndexAccessorName(index));

        Preconditions.checkArgument(
            config.scan() == ']',
            "%s:%s: expected ']': %s",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());
      } else if (token == '{') {
        // TODO: honor https://github.com/googleapis/gapic-generator/issues/2600
        Preconditions.checkArgument(
            type.isMap(),
            "%s:%s: %s is not a map field",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());
        TypeModel keyType = type.getMapKeyType();
        int keyToken = config.scan();
        if (keyType.isStringType()) {
          Preconditions.checkArgument(
              keyToken == Scanner.STRING,
              "%s:%s: expected string type for map key: %s",
              context.getMethodModel().getSimpleName(),
              valueSet.getId(),
              config.input());
        } else if (keyType.isBooleanType()) {
          // `true` and `false` are the only valid literals here
          Preconditions.checkArgument(
              keyToken == Scanner.IDENT,
              "%s:%s: expected boolean type for map key: %s",
              context.getMethodModel().getSimpleName(),
              valueSet.getId(),
              config.input());
        } else {
          // Protobuf map keys can only be strings, booleans or integers
          Preconditions.checkArgument(
              keyToken == Scanner.INT,
              "%s:%s: expected integral type for map key: %s",
              context.getMethodModel().getSimpleName(),
              valueSet.getId(),
              config.input());
        }
        keyType.validateValue(config.tokenStr());
        accessors.add(context.getNamer().getMapKeyAccessorName(keyType, config.tokenStr()));
        type = type.getMapValueType();
        Preconditions.checkArgument(
            config.scan() == '}',
            "%s:%s: expected '}': %s",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());
      } else {
        throw new IllegalArgumentException(
            String.format(
                "%s:%s: unexpected character: %c (%d)",
                context.getMethodModel().getSimpleName(), valueSet.getId(), token, token));
      }
    }

    if (newVar != null) {
      assertIdentifierNotUsed(
          newVar, context.getMethodModel().getSimpleName(), valueSet.getId(), context, form);
      if (scalarTypeForCollection) {
        Preconditions.checkArgument(
            type != null,
            "%s:%s: a resource name can never be a repeated field",
            context.getMethodModel().getSimpleName(),
            valueSet.getId());

        Preconditions.checkArgument(
            type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not a repeated field",
            context.getMethodModel().getSimpleName(),
            valueSet.getId(),
            config.input());
        type = type.makeOptional(); // "optional" is how protobuf defines singular fields
      }
      if (type == null && typeName == null) {
        throw new IllegalStateException(
            String.format(
                "%s:%s: type and typeName can't be null at the same time",
                context.getMethodModel().getSimpleName(), valueSet.getId()));
      }
      typeName = type == null ? typeName : context.getTypeTable().getNicknameFor(type);
      if (!localVars.put(newVar, type, typeName)) {
        throw new IllegalStateException(
            String.format(
                "%s:%s: duplicate variable declaration not allowed: %s",
                context.getMethodModel().getSimpleName(), valueSet.getId(), newVar));
      }
    }

    return view.accessors(accessors.build()).type(type).build();
  }

  private static void assertIdentifierNotUsed(
      String identifier,
      String methodName,
      String valueSetId,
      MethodContext context,
      CallingForm form) {
    Preconditions.checkArgument(
        !context.getNamer().getSampleUsedVarNames(context, form).contains(identifier),
        "%s: %s cannot define variable \"%s\": it is used by the sample template for calling form \"%s\".",
        methodName,
        valueSetId,
        identifier,
        form);
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
  @VisibleForTesting
  static class ScopeTable {
    private final Set<String> sample;
    @Nullable private final ScopeTable parent;
    private final Map<String, TypeModel> types = new HashMap<>();
    private final Map<String, String> typeNames = new HashMap<>();

    ScopeTable() {
      sample = new HashSet<>();
      parent = null;
    }

    ScopeTable(ScopeTable parent) {
      Preconditions.checkNotNull(parent);
      sample = parent.sample;
      this.parent = parent;
    }

    /** Gets the type of the variable. Returns null if the variable is not found. */
    TypeModel getTypeModel(String name) {
      ScopeTable table = this;
      while (table != null) {
        TypeModel type = table.types.get(name);
        if (type != null) {
          return type;
        }
        table = table.parent;
      }
      return null;
    }

    /**
     * Gets the type name of the variable. Returns null if the variable is not found. This is mostly
     * used for resource name since they do not have a {@code TypeModel}.
     */
    String getTypeName(String name) {
      ScopeTable table = this;
      while (table != null) {
        String typeName = table.typeNames.get(name);
        if (typeName != null) {
          return typeName;
        }
        table = table.parent;
      }
      return null;
    }

    /**
     * Associates a variable with a type in the current scope. Returns whether the insertion was
     * successful.
     *
     * <p>{@code type} could be left null if {@code typeName} is not associated with a {@code
     * TypeModel}, like when {@code typeName} is a resource name.
     */
    boolean put(String name, @Nullable TypeModel type, String typeName) {
      if (!sample.add(name)) {
        return false;
      }
      typeNames.put(name, typeName);
      if (type != null) {
        types.put(name, type);
      }
      return true;
    }

    private ScopeTable newChild() {
      return new ScopeTable(this);
    }
  }
}
