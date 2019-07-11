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

import static com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;

import com.google.api.codegen.OutputSpec;
import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.LongRunningConfig;
import com.google.api.codegen.config.MethodContext;
import com.google.api.codegen.config.OutputContext;
import com.google.api.codegen.config.SampleConfig;
import com.google.api.codegen.config.SampleContext;
import com.google.api.codegen.config.SampleSpec;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.samplegen.v1p2.ResponseStatementProto;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.Scanner;
import com.google.api.codegen.viewmodel.CallingForm;
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
import javax.annotation.Nullable;

public class OutputTransformer {

  private static final String RESPONSE_PLACEHOLDER = "$resp";

  static List<OutputSpec> defaultOutputSpecs(MethodContext methodContext) {
    if (methodContext.getMethodModel().isOutputTypeEmpty()) {
      return Collections.emptyList();
    }
    // We also need to check for LROs whose return values are empty
    LongRunningConfig lroConfig = methodContext.getLongRunningConfig();
    if (lroConfig != null && lroConfig.getReturnType().isEmptyType()) {
      return Collections.emptyList();
    }
    return Collections.singletonList(
        OutputSpec.newBuilder().addPrint("%s").addPrint(RESPONSE_PLACEHOLDER).build());
  }

  static List<ResponseStatementProto> defaultResponseStatements(MethodContext methodContext) {
    return fromOutputSpecs(defaultOutputSpecs(methodContext));
  }

  // Helper function for backward compatibility during sample config migration.
  private static List<ResponseStatementProto> fromOutputSpecs(List<OutputSpec> oldConfigs) {
    return oldConfigs
        .stream()
        .map(OutputTransformer::fromOutputSpec)
        .collect(ImmutableList.toImmutableList());
  }

  private static ResponseStatementProto fromOutputSpec(OutputSpec oldConfig) {
    ResponseStatementProto.Builder builder = ResponseStatementProto.newBuilder();
    builder
        .setDefine(oldConfig.getDefine())
        .addAllComment(oldConfig.getCommentList())
        .addAllPrint(oldConfig.getPrintList());
    if (oldConfig.hasWriteFile()) {
      builder
          .getWriteFileBuilder()
          .addAllFileName(oldConfig.getWriteFile().getFileNameList())
          .setContents(oldConfig.getWriteFile().getContents());
    }
    if (oldConfig.hasLoop()) {
      builder
          .getLoopBuilder()
          .setCollection(oldConfig.getLoop().getCollection())
          .setVariable(oldConfig.getLoop().getVariable())
          .setMap(oldConfig.getLoop().getMap())
          .setKey(oldConfig.getLoop().getKey())
          .setValue(oldConfig.getLoop().getValue())
          .addAllBody(fromOutputSpecs(oldConfig.getLoop().getBodyList()));
    }
    return builder.build();
  }

  // Entry point for generating output views using sample config.
  ImmutableList<OutputView> toViews(
      List<ResponseStatementProto> configs,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    return configs
        .stream()
        .map(s -> toView(s, methodContext, sampleContext, outputContext))
        .collect(ImmutableList.toImmutableList());
  }

  // Entry point for generating output views using gapic config. To be deprecated.
  ImmutableList<OutputView> toViews(
      List<OutputSpec> configs,
      MethodContext context,
      SampleValueSet valueSet,
      CallingForm form,
      OutputContext outputContext) {

    SampleConfig sampleConfig =
        SampleConfig.newBuilder()
            .id(valueSet.getId())
            .type(SampleSpec.SampleType.STANDALONE)
            .build();
    SampleContext sampleContext =
        SampleContext.newBuilder()
            .uniqueSampleId(valueSet.getId())
            .sampleType(SampleSpec.SampleType.STANDALONE)
            .sampleConfig(sampleConfig)
            .initCodeOutputType(InitCodeOutputType.FieldList)
            .callingForm(form)
            .build();
    return toViews(fromOutputSpecs(configs), context, sampleContext, outputContext);
  }

  private OutputView toView(
      ResponseStatementProto config,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    Runnable once =
        new Runnable() {
          boolean ran;

          @Override
          public void run() {
            Preconditions.checkArgument(
                !ran,
                "%s:%s: only one field of OutputSpec may be set",
                methodContext.getMethodModel().getSimpleName(),
                sampleContext.sampleConfig().id());
            ran = true;
          }
        };

    OutputView view = null;
    if (config.hasLoop()) {
      once.run();
      view = loopView(config.getLoop(), methodContext, sampleContext, outputContext);
    }
    if (config.getPrintCount() > 0) {
      once.run();
      view = printView(config.getPrintList(), methodContext, sampleContext, outputContext);
    }
    if (!config.getDefine().isEmpty()) {
      once.run();
      view =
          defineView(new Scanner(config.getDefine()), methodContext, sampleContext, outputContext);
    }
    if (config.getCommentCount() > 0) {
      once.run();
      view = commentView(config.getCommentList(), methodContext);
    }
    if (config.hasWriteFile()) {
      once.run();
      view = writeFileView(config.getWriteFile(), methodContext, sampleContext, outputContext);
    }

    return Preconditions.checkNotNull(
        view,
        "%s:%s: one field of OutputSpec must be set",
        methodContext.getMethodModel().getSimpleName(),
        sampleContext.sampleConfig().id());
  }

  private OutputView.PrintView printView(
      List<String> config,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    Preconditions.checkArgument(
        !config.isEmpty(),
        "%s:%s: print spec cannot be empty",
        methodContext.getMethodModel().getSimpleName(),
        sampleContext.sampleConfig().id());
    outputContext.printSpecs().add(config);
    OutputView.StringInterpolationView interpolatedString =
        stringInterpolationView(methodContext, sampleContext, outputContext, config);
    return OutputView.PrintView.newBuilder().interpolatedString(interpolatedString).build();
  }

  private OutputView.WriteFileView writeFileView(
      ResponseStatementProto.WriteFileStatement config,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    OutputView.StringInterpolationView fileName =
        stringInterpolationView(
            methodContext, sampleContext, outputContext, config.getFileNameList());
    OutputView.VariableView contents =
        accessor(
            new Scanner(config.getContents()),
            methodContext,
            sampleContext,
            outputContext.scopeTable());
    Preconditions.checkArgument(
        contents.type().isStringType() || contents.type().isBytesType(),
        "Output to file: expected 'string' or 'bytes', found %s",
        contents.type().getTypeName());
    outputContext.fileOutputTypes().add(contents.type());
    return OutputView.WriteFileView.newBuilder()
        .fileName(fileName)
        .contents(contents)
        .isFirst(!outputContext.hasMultipleFileOutputs())
        .build();
  }

  private OutputView.StringInterpolationView stringInterpolationView(
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext,
      List<String> configs) {
    String format = configs.get(0);
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (String path : configs.subList(1, configs.size())) {
      OutputView.VariableView variable =
          accessor(new Scanner(path), methodContext, sampleContext, outputContext.scopeTable());
      TypeModel type = variable.type();
      // TODO: resource names are left out. We don't need to do anything for
      // resource names, but should include them as well for completeness
      if (type != null) {
        outputContext.stringFormattedVariableTypes().add(type);
      }
      String formattedArg =
          methodContext
              .getNamer()
              .getFormattedPrintArgName(
                  methodContext.getTypeTable(), type, variable.variable(), variable.accessors());
      builder.add(formattedArg);
    }
    ImmutableList<String> args = builder.build();
    ImmutableList<String> stringWithInterpolatedArgs =
        methodContext.getNamer().getInterpolatedFormatAndArgs(format, args);
    return OutputView.StringInterpolationView.newBuilder()
        .format(stringWithInterpolatedArgs.get(0))
        .args(stringWithInterpolatedArgs.subList(1, stringWithInterpolatedArgs.size()))
        .build();
  }

  private OutputView loopView(
      ResponseStatementProto.LoopStatement loop,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    if (!loop.getCollection().isEmpty() && loop.getMap().isEmpty()) {
      Preconditions.checkArgument(
          !loop.getVariable().isEmpty(),
          "Bad format: `variable` must be specified if `collection` is specified.");
      Preconditions.checkArgument(
          loop.getKey().isEmpty() && loop.getValue().isEmpty(),
          "Bad format: neither `key` nor `value` can be specified if `collection` is specified.");
      return arrayLoopView(
          loop, methodContext, sampleContext, outputContext.createWithNewChildScope());
    } else if (!loop.getMap().isEmpty() && loop.getCollection().isEmpty()) {
      Preconditions.checkArgument(
          loop.getVariable().isEmpty(),
          "Bad format: `variable` can't be specified if `map` is specified.");
      Preconditions.checkArgument(
          !loop.getKey().isEmpty() || !loop.getValue().isEmpty(),
          "Bad format: at least one of `key` and `value` must be specified if `collection` is"
              + " specified.");
      return mapLoopView(
          loop, methodContext, sampleContext, outputContext.createWithNewChildScope());
    } else {
      throw new IllegalArgumentException(
          "Bad format: exactly one of `map` and `collection` should be specified in `loop`.");
    }
  }

  private OutputView.ArrayLoopView arrayLoopView(
      ResponseStatementProto.LoopStatement loop,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    ScopeTable scope = outputContext.scopeTable();
    String loopVariable = loop.getVariable();
    assertIdentifierNotUsed(loopVariable, methodContext, sampleContext);
    OutputView.VariableView accessor =
        accessorNewVariable(
            new Scanner(loop.getCollection()),
            methodContext,
            sampleContext,
            scope,
            loopVariable,
            true);
    return OutputView.ArrayLoopView.newBuilder()
        .variableType(scope.getTypeName(loopVariable))
        .variableName(methodContext.getNamer().localVarName(Name.from(loopVariable)))
        .collection(accessor)
        .body(
            loop.getBodyList()
                .stream()
                .map(body -> toView(body, methodContext, sampleContext, outputContext))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private OutputView.MapLoopView mapLoopView(
      ResponseStatementProto.LoopStatement loop,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    outputContext.mapSpecs().add(loop);
    ScopeTable scope = outputContext.scopeTable();
    String key = loop.getKey();
    String value = loop.getValue();

    OutputView.VariableView mapVar =
        accessor(new Scanner(loop.getMap()), methodContext, sampleContext, scope);
    TypeModel keyType = mapVar.type().getMapKeyType();
    TypeModel valueType = mapVar.type().getMapValueType();
    String keyTypeName = methodContext.getTypeTable().getNicknameFor(keyType);
    String valueTypeName = methodContext.getTypeTable().getNicknameFor(valueType);

    if (!key.isEmpty()) {
      assertIdentifierNotUsed(key, methodContext, sampleContext);
      scope.put(key, keyType, keyTypeName);
    }
    if (!value.isEmpty()) {
      assertIdentifierNotUsed(value, methodContext, sampleContext);
      scope.put(value, valueType, valueTypeName);
    }
    return OutputView.MapLoopView.newBuilder()
        .keyVariableName(methodContext.getNamer().localVarName(Name.anyLower(key)))
        .keyType(keyTypeName)
        .valueVariableName(methodContext.getNamer().localVarName(Name.anyLower(value)))
        .valueType(valueTypeName)
        .map(mapVar)
        .body(
            loop.getBodyList()
                .stream()
                .map(body -> toView(body, methodContext, sampleContext, outputContext))
                .collect(ImmutableList.toImmutableList()))
        .build();
  }

  private OutputView.DefineView defineView(
      Scanner definition,
      MethodContext methodContext,
      SampleContext sampleContext,
      OutputContext outputContext) {
    Preconditions.checkArgument(
        definition.scan() == Scanner.IDENT,
        "%s:%s: expected identifier: %s",
        methodContext.getMethodModel().getSimpleName(),
        sampleContext.sampleConfig().id(),
        definition.input());
    String identifier = definition.tokenStr();
    assertIdentifierNotUsed(identifier, methodContext, sampleContext);
    Preconditions.checkArgument(
        definition.scan() == '=',
        "%s:%s invalid definition, expecting '=': %s",
        methodContext.getMethodModel().getSimpleName(),
        sampleContext.sampleConfig().id(),
        definition.input());
    OutputView.VariableView reference =
        accessorNewVariable(
            definition,
            methodContext,
            sampleContext,
            outputContext.scopeTable(),
            identifier,
            false);
    return OutputView.DefineView.newBuilder()
        .variableTypeName(outputContext.scopeTable().getTypeName(identifier))
        .variableName(methodContext.getNamer().localVarName(Name.from(identifier)))
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
      MethodContext methodContext,
      SampleContext sampleContext,
      ScopeTable localVars) {
    return accessorNewVariable(config, methodContext, sampleContext, localVars, null, false);
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
      MethodContext methodContext,
      SampleContext sampleContext,
      ScopeTable localVars,
      @Nullable String newVar,
      boolean scalarTypeForCollection) {

    OutputView.VariableView.Builder view = OutputView.VariableView.newBuilder();

    Preconditions.checkArgument(
        config.scan() == Scanner.IDENT,
        "%s:%s: expected identifier: %s",
        methodContext.getMethodModel().getSimpleName(),
        sampleContext.sampleConfig().id(),
        config.input());
    String baseIdentifier = config.tokenStr();

    TypeModel type = null;
    String typeName = null;
    if (baseIdentifier.equals(RESPONSE_PLACEHOLDER)) {
      view.variable(
          methodContext
              .getNamer()
              .getSampleResponseVarName(methodContext, sampleContext.callingForm()));
      boolean pageStreaming = methodContext.getMethodConfig().getPageStreaming() != null;
      boolean pageStreamingAndUseResourceName =
          pageStreaming
              && methodContext
                  .getFeatureConfig()
                  .useResourceNameFormatOption(
                      methodContext.getMethodConfig().getPageStreaming().getResourcesFieldConfig());

      // Compute the resource name format of output type and store that in typeName
      if (pageStreamingAndUseResourceName) {
        typeName =
            methodContext
                .getNamer()
                .getAndSaveElementResourceTypeName(
                    methodContext.getTypeTable(),
                    methodContext.getMethodConfig().getPageStreaming().getResourcesFieldConfig());
      } else if (pageStreaming) {
        type =
            methodContext
                .getMethodConfig()
                .getPageStreaming()
                .getResourcesFieldConfig()
                .getField()
                .getType()
                .makeOptional();
      } else if (methodContext.isLongRunningMethodContext()) {
        type = methodContext.getLongRunningConfig().getReturnType();
      } else {
        type = methodContext.getMethodModel().getOutputType();
      }
    } else {
      // Referencing the value of a local variable
      view.variable(methodContext.getNamer().localVarName(Name.from(baseIdentifier)));
      type = localVars.getTypeModel(baseIdentifier);
      if (type == null) {
        typeName =
            Preconditions.checkNotNull(
                localVars.getTypeName(baseIdentifier),
                "%s:%s: variable not defined: %s",
                methodContext.getMethodModel().getSimpleName(),
                sampleContext.sampleConfig().id(),
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
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id());
        Preconditions.checkArgument(
            type.isMessage(),
            "%s:%s: %s is not a message",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());
        Preconditions.checkArgument(
            !type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not scalar",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());

        Preconditions.checkArgument(
            config.scan() == Scanner.IDENT,
            "%s:%s: expected identifier: %s",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());

        String fieldName = config.tokenStr();
        FieldModel field =
            Preconditions.checkNotNull(
                type.getField(fieldName),
                "%s:%s: type %s does not have field %s",
                methodContext.getMethodModel().getSimpleName(),
                sampleContext.sampleConfig().id(),
                type,
                fieldName);

        type = field.getType();
        accessors.add(methodContext.getNamer().getFieldAccessorName(field));
      } else if (token == '[') {
        Preconditions.checkArgument(
            type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not a repeated field",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());
        Preconditions.checkArgument(
            config.scan() == Scanner.INT,
            "%s:%s: expected int in index expression: %s",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());

        type = type.makeOptional();
        int index = Integer.parseInt(config.tokenStr());
        accessors.add(methodContext.getNamer().getIndexAccessorName(index));

        Preconditions.checkArgument(
            config.scan() == ']',
            "%s:%s: expected ']': %s",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());
      } else if (token == '{') {
        // TODO: honor https://github.com/googleapis/gapic-generator/issues/2600
        Preconditions.checkArgument(
            type.isMap(),
            "%s:%s: %s is not a map field",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());
        TypeModel keyType = type.getMapKeyType();
        int keyToken = config.scan();
        if (keyType.isStringType()) {
          Preconditions.checkArgument(
              keyToken == Scanner.STRING,
              "%s:%s: expected string type for map key: %s",
              methodContext.getMethodModel().getSimpleName(),
              sampleContext.sampleConfig().id(),
              config.input());
        } else if (keyType.isBooleanType()) {
          // `true` and `false` are the only valid literals here
          Preconditions.checkArgument(
              keyToken == Scanner.IDENT,
              "%s:%s: expected boolean type for map key: %s",
              methodContext.getMethodModel().getSimpleName(),
              sampleContext.sampleConfig().id(),
              config.input());
        } else {
          // Protobuf map keys can only be strings, booleans or integers
          Preconditions.checkArgument(
              keyToken == Scanner.INT,
              "%s:%s: expected integral type for map key: %s",
              methodContext.getMethodModel().getSimpleName(),
              sampleContext.sampleConfig().id(),
              config.input());
        }
        keyType.validateValue(config.tokenStr());
        accessors.add(methodContext.getNamer().getMapKeyAccessorName(keyType, config.tokenStr()));
        type = type.getMapValueType();
        Preconditions.checkArgument(
            config.scan() == '}',
            "%s:%s: expected '}': %s",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());
      } else {
        throw new IllegalArgumentException(
            String.format(
                "%s:%s: unexpected character: %c (%d)",
                methodContext.getMethodModel().getSimpleName(),
                sampleContext.sampleConfig().id(),
                token,
                token));
      }
    }

    if (newVar != null) {
      assertIdentifierNotUsed(newVar, methodContext, sampleContext);
      if (scalarTypeForCollection) {
        Preconditions.checkArgument(
            type != null,
            "%s:%s: a resource name can never be a repeated field",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id());

        Preconditions.checkArgument(
            type.isRepeated() && !type.isMap(),
            "%s:%s: %s is not a repeated field",
            methodContext.getMethodModel().getSimpleName(),
            sampleContext.sampleConfig().id(),
            config.input());
        type = type.makeOptional(); // "optional" is how protobuf defines singular fields
      }
      if (type == null && typeName == null) {
        throw new IllegalStateException(
            String.format(
                "%s:%s: type and typeName can't be null at the same time",
                methodContext.getMethodModel().getSimpleName(), sampleContext.sampleConfig().id()));
      }
      typeName = type == null ? typeName : methodContext.getTypeTable().getNicknameFor(type);
      if (!localVars.put(newVar, type, typeName)) {
        throw new IllegalStateException(
            String.format(
                "%s:%s: duplicate variable declaration not allowed: %s",
                methodContext.getMethodModel().getSimpleName(),
                sampleContext.sampleConfig().id(),
                newVar));
      }
    }

    return view.accessors(accessors.build()).type(type).build();
  }

  private static void assertIdentifierNotUsed(
      String identifier, MethodContext methodContext, SampleContext sampleContext) {
    Preconditions.checkArgument(
        !methodContext
            .getNamer()
            .getSampleUsedVarNames(methodContext, sampleContext.callingForm())
            .contains(identifier),
        "%s: %s cannot define variable \"%s\": it is already used by the sample template for"
            + " calling form \"%s\".",
        methodContext.getMethodModel().getSimpleName(),
        sampleContext.sampleConfig().id(),
        identifier,
        sampleContext.callingForm());
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
  // TODO(hzyi): factor it out to a top-level class
  public static class ScopeTable {
    private final Set<String> sample;
    // Store all types used in the sample scope. Java and C# need to know this
    // to correctly import those types. This is basically doing what `ImportTypeTable`
    // does, when we factor `ScopeTable` out to a top-level class we should consider
    // reusing `ImportTypeTable` as well.
    // We still use null to represent resource names since `SampleImportTransformer`
    // knows how to import the correct resource name types for all cases we need now.
    // Consider making a wrapper type that can refer to either a `TypeModel` and
    // `ResourceNameConfig` to make the code cleaner.
    private final Set<TypeModel> allTypes;

    @Nullable private final ScopeTable parent;
    private final Map<String, TypeModel> types = new HashMap<>();
    private final Map<String, String> typeNames = new HashMap<>();

    public ScopeTable() {
      sample = new HashSet<>();
      allTypes = new HashSet<>();
      parent = null;
    }

    ScopeTable(ScopeTable parent) {
      Preconditions.checkNotNull(parent);
      sample = parent.sample;
      allTypes = parent.allTypes;
      this.parent = parent;
    }

    /** Gets the type of the variable. Returns null if the variable is not found. */
    public TypeModel getTypeModel(String name) {
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
    public String getTypeName(String name) {
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
      allTypes.add(type);
      typeNames.put(name, typeName);
      if (type != null) {
        types.put(name, type);
      }
      return true;
    }

    public Set<TypeModel> allTypes() {
      Set<TypeModel> types = new HashSet<>(); // ImmutableSet does not allow null elements
      types.addAll(allTypes);
      return types;
    }

    public ScopeTable newChild() {
      return new ScopeTable(this);
    }
  }
}
