/* Copyright 2016 Google LLC
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

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.ProtoTypeRef;
import com.google.api.codegen.config.ResourceNameConfig;
import com.google.api.codegen.config.ResourceNameOneofConfig;
import com.google.api.codegen.config.ResourceNameType;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.config.TypeModel;
import com.google.api.codegen.metacode.FieldStructureParser;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeContext.InitCodeOutputType;
import com.google.api.codegen.metacode.InitCodeLineType;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValue;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.util.Scanner;
import com.google.api.codegen.util.SymbolTable;
import com.google.api.codegen.util.testing.TestValueGenerator;
import com.google.api.codegen.viewmodel.FieldSettingView;
import com.google.api.codegen.viewmodel.FormattedInitValueView;
import com.google.api.codegen.viewmodel.InitCodeLineView;
import com.google.api.codegen.viewmodel.InitCodeView;
import com.google.api.codegen.viewmodel.InitValueView;
import com.google.api.codegen.viewmodel.ListInitCodeLineView;
import com.google.api.codegen.viewmodel.MapEntryView;
import com.google.api.codegen.viewmodel.MapInitCodeLineView;
import com.google.api.codegen.viewmodel.OneofConfigView;
import com.google.api.codegen.viewmodel.ReadFileInitCodeLineView;
import com.google.api.codegen.viewmodel.RepeatedResourceNameInitValueView;
import com.google.api.codegen.viewmodel.ResourceNameInitValueView;
import com.google.api.codegen.viewmodel.ResourceNameOneofInitValueView;
import com.google.api.codegen.viewmodel.SimpleInitCodeLineView;
import com.google.api.codegen.viewmodel.SimpleInitValueView;
import com.google.api.codegen.viewmodel.StructureInitCodeLineView;
import com.google.api.codegen.viewmodel.testing.ClientTestAssertView;
import com.google.api.pathtemplate.PathTemplate;
import com.google.api.tools.framework.model.TypeRef;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * InitCodeTransformer generates initialization code for a given method and then transforms it to a
 * view object which can be rendered by a template engine.
 */
public class InitCodeTransformer {
  private static final String FORMAT_SPEC_PLACEHOLDER = "FORMAT_SPEC_PLACEHOLDER";

  // Note: Markdown backticks for code reference should be converted to an idiomatic representation
  // by the language-appropriate CommentReformatter when this String is formatted.
  private static final String UNINITIALIZED_REQUIRED_FIELD_COMMENT = "TODO: Initialize `%s`:";

  private final ImportSectionTransformer importSectionTransformer;

  // Whether the initialization code should include user-facing comments like TODOs. This should be
  // false if the initialization code is being used to generate tests, rather than code samples.
  private final boolean generateUserFacingComments;

  public InitCodeTransformer() {
    this(new StandardImportSectionTransformer(), true);
  }

  public InitCodeTransformer(ImportSectionTransformer importSectionTransformer) {
    this(importSectionTransformer, true);
  }

  public InitCodeTransformer(boolean generateUserFacingComments) {
    this(new StandardImportSectionTransformer(), generateUserFacingComments);
  }

  public InitCodeTransformer(
      ImportSectionTransformer importSectionTransformer, boolean generateUserFacingComments) {
    this.importSectionTransformer = importSectionTransformer;
    this.generateUserFacingComments = generateUserFacingComments;
  }

  /** Generates initialization code from the given MethodContext and InitCodeContext objects. */
  public InitCodeView generateInitCode(
      MethodContext methodContext, InitCodeContext initCodeContext) {
    InitCodeNode rootNode = InitCodeNode.createTree(initCodeContext);
    if (initCodeContext.outputType() == InitCodeOutputType.FieldList) {
      return buildInitCodeViewFlattened(methodContext, initCodeContext, rootNode);
    } else {
      return buildInitCodeViewRequestObject(methodContext, initCodeContext, rootNode);
    }
  }

  public InitCodeContext createRequestInitCodeContext(
      MethodContext context,
      SymbolTable symbolTable,
      Collection<FieldConfig> fieldConfigs,
      InitCodeOutputType outputType,
      TestValueGenerator valueGenerator) {
    return InitCodeContext.newBuilder()
        .initObjectType(context.getMethodModel().getInputType())
        .symbolTable(symbolTable)
        .suggestedName(Name.from("request"))
        .initFieldConfigStrings(context.getMethodConfig().getSampleCodeInitFields())
        .initValueConfigMap(InitCodeTransformer.createCollectionMap(context))
        .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
        .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
        .outputType(outputType)
        .valueGenerator(valueGenerator)
        .build();
  }

  /** Generates assert views for the test of the tested method and its fields. */
  List<ClientTestAssertView> generateRequestAssertViews(
      MethodContext methodContext, InitCodeContext initContext) {
    InitCodeNode rootNode =
        InitCodeNode.createTree(
            InitCodeContext.newBuilder()
                .initObjectType(methodContext.getMethodModel().getInputType())
                .initFields(initContext.initFields())
                .initValueConfigMap(createCollectionMap(methodContext))
                .suggestedName(Name.from("request"))
                .fieldConfigMap(initContext.fieldConfigMap())
                .build());

    List<ClientTestAssertView> assertViews = new ArrayList<>();
    SurfaceNamer namer = methodContext.getNamer();
    // Add request fields checking
    for (InitCodeNode fieldItemTree : rootNode.getChildren().values()) {
      FieldConfig fieldConfig = fieldItemTree.getFieldConfig();

      String getterMethod =
          namer.getFieldGetFunctionName(methodContext.getFeatureConfig(), fieldConfig);

      String expectedValueIdentifier = getVariableName(methodContext, fieldItemTree);
      String expectedTransformFunction = null;
      String actualTransformFunction = null;
      if (methodContext.getFeatureConfig().useResourceNameFormatOption(fieldConfig)) {
        if (fieldConfig.requiresParamTransformationFromAny()) {
          expectedTransformFunction = namer.getToStringMethod();
          actualTransformFunction = namer.getToStringMethod();
        } else if (fieldConfig.requiresParamTransformation()) {
          if (methodContext.getFeatureConfig().useResourceNameConverters(fieldConfig)) {
            expectedTransformFunction = namer.getToStringMethod();
          } else {
            expectedTransformFunction =
                namer.getResourceOneofCreateMethod(methodContext.getTypeTable(), fieldConfig);
          }
        } else if (methodContext.getFeatureConfig().useResourceNameConverters(fieldConfig)) {
          if (fieldConfig.getField().isRepeated()) {
            actualTransformFunction =
                namer.getResourceTypeParseListMethodName(methodContext.getTypeTable(), fieldConfig);
          } else if (fieldConfig.getResourceNameConfig().getResourceNameType()
              == ResourceNameType.ONEOF) {
            actualTransformFunction =
                namer.getResourceTypeParentParseMethod(methodContext.getTypeTable(), fieldConfig);
          } else {
            actualTransformFunction =
                namer.getResourceTypeParseMethodName(methodContext.getTypeTable(), fieldConfig);
          }
        }
      }

      boolean isMap = fieldConfig.getField().isMap();
      boolean isArray = fieldConfig.getField().isRepeated() && !isMap;

      TypeModel fieldType = fieldItemTree.getType();
      String messageTypeName = null;
      if (fieldType.isMessage()) {
        messageTypeName = methodContext.getTypeTable().getFullNameForMessageType(fieldType);
      }

      assertViews.add(
          createAssertView(
              expectedValueIdentifier,
              expectedTransformFunction,
              actualTransformFunction,
              isMap,
              isArray,
              getterMethod,
              messageTypeName));
    }
    return assertViews;
  }

  /**
   * A utility method which creates the InitValueConfig map that contains the collection config
   * data.
   */
  public static ImmutableMap<String, InitValueConfig> createCollectionMap(MethodContext context) {
    ImmutableMap.Builder<String, InitValueConfig> mapBuilder = ImmutableMap.builder();
    Map<String, String> fieldNamePatterns = context.getMethodConfig().getFieldNamePatterns();
    for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
      SingleResourceNameConfig resourceNameConfig =
          context.getSingleResourceNameConfig(fieldNamePattern.getValue());
      String apiWrapperClassName =
          context.getNamer().getApiWrapperClassName(context.getInterfaceConfig());
      InitValueConfig initValueConfig =
          InitValueConfig.create(apiWrapperClassName, resourceNameConfig);
      mapBuilder.put(fieldNamePattern.getKey(), initValueConfig);
    }
    return mapBuilder.build();
  }

  private ClientTestAssertView createAssertView(
      String expected,
      String expectedTransformFunction,
      String actualTransformFunction,
      boolean isMap,
      boolean isArray,
      String actual,
      String messageTypeName) {
    return ClientTestAssertView.newBuilder()
        .expectedValueIdentifier(expected)
        .isMap(isMap)
        .isArray(isArray)
        .expectedValueTransformFunction(expectedTransformFunction)
        .actualValueTransformFunction(actualTransformFunction)
        .actualValueGetter(actual)
        .messageTypeName(messageTypeName)
        .build();
  }

  private InitCodeView buildInitCodeViewFlattened(
      MethodContext context, InitCodeContext initCodeContext, InitCodeNode root) {
    assertNoOverlap(root, initCodeContext.sampleArgStrings());

    // Remove the request object for flattened method
    List<InitCodeNode> orderedItems = root.listInInitializationOrder();
    orderedItems.remove(orderedItems.size() - 1);

    return buildInitCodeView(
        context,
        orderedItems,
        ImmutableList.copyOf(root.getChildren().values()),
        sampleFuncParams(root, initCodeContext.sampleArgStrings()));
  }

  private InitCodeView buildInitCodeViewRequestObject(
      MethodContext context, InitCodeContext initCodeContext, InitCodeNode root) {
    assertNoOverlap(root, initCodeContext.sampleArgStrings());
    return buildInitCodeView(
        context,
        root.listInInitializationOrder(),
        ImmutableList.of(root),
        sampleFuncParams(root, initCodeContext.sampleArgStrings()));
  }

  /**
   * Returns all the nodes to be rendered as sample function parameters.
   *
   * <p>If path is:
   * <li>a normal node, returns that node.
   * <li>a ReadFile node, returns the child node of that node.
   * <li>a resource path, returns the child node whose key equals the entity name in the path.
   */
  private List<InitCodeNode> sampleFuncParams(InitCodeNode root, List<String> paths) {
    List<InitCodeNode> params = new ArrayList<>();
    for (String path : paths) {
      Scanner scanner = new Scanner(path);
      InitCodeNode node = FieldStructureParser.parsePath(root, scanner);
      int token = scanner.lastToken();
      if (token == '%') {
        scanner.scan();
        params.add(node.getChildren().get(scanner.tokenStr()));
      } else if (node.getLineType() == InitCodeLineType.ReadFileInitLine) {
        params.add(node.getChildren().get(InitCodeNode.FILE_NAME_KEY));
      } else {
        params.add(node);
      }
    }
    return params;
  }

  /**
   * Given node `root` and `paths` describing subtrees of `root`, verify that all subtrees are
   * disjoint. i.e., no two subtrees are the same, and no subtrees are themselves part of other
   * subtrees.
   */
  @VisibleForTesting
  static void assertNoOverlap(InitCodeNode root, List<String> paths) {
    // Keep track of the path that adds a node. If we detect collision we can report the two paths
    // that reference the same nodes.
    HashMap<InitCodeNode, String> refFrom = new HashMap<>();

    // Keep track of the resource name entities. Configuring an entity twice or configuring an
    // entity and the parent node at the same time will cause collision. Configuring two different
    // entities will not.
    Multimap<InitCodeNode, String> nodeEntities = HashMultimap.create();

    // Below we'll perform depth-first search, keep a list of nodes we've seen but have not
    // descended into. It doesn't really matter if we search breath- or depth-first; DFS is a little
    // more efficient on average.
    ArrayDeque<InitCodeNode> subNodes = new ArrayDeque<>();

    for (String path : paths) {
      subNodes.add(root.subTree(path));
      String entity = FieldStructureParser.parseEntityName(path);
      while (!subNodes.isEmpty()) {
        InitCodeNode node = subNodes.pollLast();
        String oldPath = refFrom.put(node, path);
        if (oldPath == null) {
          // The node has not been specified before, thus check if entity has been specified
          checkArgument(
              entity == null || nodeEntities.put(node, entity),
              "Entity %s in path %s specified multiple times",
              entity,
              path);
        } else {
          // The node has been specified before. The will be no overlap if and only if:
          // All previous paths are configuring entities
          // This path is configuraing an entity
          // The same entity is never specified before
          checkArgument(
              entity != null && nodeEntities.containsKey(node),
              "SampleInitAttribute %s overlaps with %s",
              oldPath,
              path);
          checkArgument(
              nodeEntities.put(node, entity),
              "Entity %s in path %s specified multiple times",
              entity,
              path);
        }
        subNodes.addAll(node.getChildren().values());
      }
    }
  }

  /**
   * Transform {@code InitCodeNode}s into {@code InitCodeView}.
   *
   * @param orderedItems These nodes are converted into request-initialization code. It contains all
   *     initializations regardless of whether they are parameters to the sample function. The
   *     initialization is "shallow": children nodes are not initialized. If children nodes should
   *     also be initialized, callers must also include them in the list.
   * @param libArguments Used by samples for flattened client lib methods. These nodes contain
   *     values that become arguments to the method.
   * @param sampleFuncParams Subset of {@code orderedItems} containing only items that are function
   *     parameters. Unlike {@code orderedItems}, the {@code sampleFuncParams} are "deep". The init
   *     code for these nodes and their children are commented out so that they don't clobber the
   *     function arguments.
   */
  private InitCodeView buildInitCodeView(
      MethodContext context,
      List<InitCodeNode> orderedItems,
      List<InitCodeNode> libArguments,
      List<InitCodeNode> sampleFuncParams) {
    ImportTypeTable typeTable = context.getTypeTable();
    SurfaceNamer namer = context.getNamer();

    // Initialize the type table with the apiClassName since each sample will be using the
    // apiClass.
    typeTable.getAndSaveNicknameFor(
        namer.getFullyQualifiedApiWrapperClassName(context.getInterfaceConfig()));

    List<FieldSettingView> fieldSettings = getFieldSettings(context, libArguments);
    List<FieldSettingView> optionalFieldSettings =
        fieldSettings.stream().filter(f -> !f.required()).collect(Collectors.toList());
    List<FieldSettingView> requiredFieldSettings =
        fieldSettings.stream().filter(FieldSettingView::required).collect(Collectors.toList());

    List<InitCodeLineView> argDefaultParams = new ArrayList<>();
    List<InitCodeLineView> argDefaultLines = new ArrayList<>();
    for (InitCodeNode param : sampleFuncParams) {
      List<InitCodeNode> paramInits = param.listInInitializationOrder();
      argDefaultLines.addAll(generateSurfaceInitCodeLines(context, paramInits));

      // The param itself is always at the end.
      argDefaultParams.add(argDefaultLines.get(argDefaultLines.size() - 1));

      // Since we're going to write the inits for the params here,
      // remove so we don't init twice.
      orderedItems.removeAll(paramInits);
    }

    return InitCodeView.newBuilder()
        .argDefaultLines(argDefaultLines)
        .argDefaultParams(argDefaultParams)
        .lines(generateSurfaceInitCodeLines(context, orderedItems))
        .topLevelLines(generateSurfaceInitCodeLines(context, libArguments))
        .fieldSettings(fieldSettings)
        .optionalFieldSettings(optionalFieldSettings)
        .requiredFieldSettings(requiredFieldSettings)
        .importSection(importSectionTransformer.generateImportSection(context, orderedItems))
        .topLevelIndexFileImportName(namer.getTopLevelIndexFileImportName())
        .build();
  }

  private List<InitCodeLineView> generateSurfaceInitCodeLines(
      MethodContext context, Iterable<InitCodeNode> specItemNode) {
    boolean isFirstReadFileView = true;
    List<InitCodeLineView> surfaceLines = new ArrayList<>();
    for (InitCodeNode item : specItemNode) {
      surfaceLines.add(
          generateSurfaceInitCodeLine(context, item, surfaceLines.isEmpty(), isFirstReadFileView));
      isFirstReadFileView =
          isFirstReadFileView && item.getLineType() != InitCodeLineType.ReadFileInitLine;
    }
    return surfaceLines;
  }

  private InitCodeLineView generateSurfaceInitCodeLine(
      MethodContext context,
      InitCodeNode specItemNode,
      boolean isFirstItem,
      boolean isFirstReadFileView) {
    switch (specItemNode.getLineType()) {
      case StructureInitLine:
        return generateStructureInitCodeLine(context, specItemNode);
      case ListInitLine:
        return generateListInitCodeLine(context, specItemNode);
      case SimpleInitLine:
        return generateSimpleInitCodeLine(context, specItemNode, isFirstItem);
      case MapInitLine:
        return generateMapInitCodeLine(context, specItemNode);
      case ReadFileInitLine:
        return generateReadFileInitCodeLine(context, specItemNode, isFirstReadFileView);
      default:
        throw new RuntimeException("unhandled line type: " + specItemNode.getLineType());
    }
  }

  private InitCodeLineView generateSimpleInitCodeLine(
      MethodContext context, InitCodeNode item, boolean isFirstItem) {
    SimpleInitCodeLineView.Builder surfaceLine = SimpleInitCodeLineView.newBuilder();
    FieldConfig fieldConfig = item.getFieldConfig();

    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.SimpleInitLine);

    if (context.getFeatureConfig().useResourceNameFormatOptionInSample(context, fieldConfig)) {
      if (!context.isFlattenedMethodContext()) {
        // In a non-flattened context, we always use the resource name type set on the message
        // instead of set on the flattened method
        fieldConfig = fieldConfig.getMessageFieldConfig();
      }
      if (item.getType().isRepeated()) {
        surfaceLine.typeName(namer.getAndSaveResourceTypeName(typeTable, fieldConfig));
      } else {
        surfaceLine.typeName(namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig));
      }
    } else {
      surfaceLine.typeName(typeTable.getAndSaveNicknameFor(item.getType()));
    }

    surfaceLine.identifier(getVariableName(context, item));
    setInitValueAndComments(surfaceLine, context, item, isFirstItem);

    return surfaceLine.build();
  }

  private InitCodeLineView generateStructureInitCodeLine(MethodContext context, InitCodeNode item) {
    StructureInitCodeLineView.Builder surfaceLine = StructureInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.StructureInitLine);
    surfaceLine.identifier(namer.localVarName(item.getIdentifier()));

    String typeName = typeTable.getAndSaveNicknameFor(item.getType());
    surfaceLine.typeName(typeName);
    surfaceLine.typeConstructor(namer.getTypeConstructor(typeName));
    surfaceLine.fieldSettings(getFieldSettings(context, item.getChildren().values()));

    return surfaceLine.build();
  }

  private InitCodeLineView generateListInitCodeLine(MethodContext context, InitCodeNode item) {
    ListInitCodeLineView.Builder surfaceLine = ListInitCodeLineView.newBuilder();
    FieldConfig fieldConfig = item.getFieldConfig();

    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.ListInitLine);
    surfaceLine.identifier(namer.localVarName(item.getIdentifier()));

    if (context.getFeatureConfig().useResourceNameFormatOptionInSample(context, fieldConfig)) {
      surfaceLine.elementTypeName(namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig));
    } else {
      surfaceLine.elementTypeName(
          typeTable.getAndSaveNicknameForElementType(item.getType().makeOptional()));
    }

    List<String> entries = new ArrayList<>();
    List<InitCodeLineView> elements = new ArrayList<>();
    for (InitCodeNode child : item.getChildren().values()) {
      entries.add(namer.localVarName(child.getIdentifier()));
      elements.add(generateSurfaceInitCodeLine(context, child, elements.isEmpty(), false));
    }
    surfaceLine.elementIdentifiers(entries);
    surfaceLine.elements(elements);

    return surfaceLine.build();
  }

  private InitCodeLineView generateMapInitCodeLine(MethodContext context, InitCodeNode item) {
    MapInitCodeLineView.Builder surfaceLine = MapInitCodeLineView.newBuilder();

    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    surfaceLine.lineType(InitCodeLineType.MapInitLine);
    surfaceLine.identifier(namer.localVarName(item.getIdentifier()));

    surfaceLine.keyTypeName(typeTable.getAndSaveNicknameFor(item.getType().getMapKeyType()));
    surfaceLine.valueTypeName(typeTable.getAndSaveNicknameFor(item.getType().getMapValueType()));

    List<MapEntryView> entries = new ArrayList<>();
    for (Map.Entry<String, InitCodeNode> entry : item.getChildren().entrySet()) {
      MapEntryView.Builder mapEntry = MapEntryView.newBuilder();
      mapEntry.key(typeTable.renderPrimitiveValue(item.getType().getMapKeyType(), entry.getKey()));
      mapEntry.valueString(context.getNamer().localVarName(entry.getValue().getIdentifier()));
      mapEntry.value(
          generateSurfaceInitCodeLine(context, entry.getValue(), entries.isEmpty(), false));
      entries.add(mapEntry.build());
    }
    surfaceLine.initEntries(entries);

    return surfaceLine.build();
  }

  // TODO(hzyi): generate necessary imports
  /**
   * @param isFirstReadFileView Used in Java. We need to reuse local variables "path" and "data" if
   *     we have rendered ReadFileViews before so that we don't declare them twice.
   */
  private InitCodeLineView generateReadFileInitCodeLine(
      MethodContext context, InitCodeNode item, boolean isFirstReadFileView) {
    ReadFileInitCodeLineView.Builder surfaceLine = ReadFileInitCodeLineView.newBuilder();
    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    checkState(
        item.getType().isBytesType(),
        "Error setting %s to be read from file. "
            + "Replacing field value with file contents is only allowed for fields of type 'bytes', "
            + "but the type is %s.",
        item.getIdentifier(),
        item.getType());
    typeTable.getAndSaveNicknameFor(item.getType());
    String value = item.getInitValueConfig().getInitialValue().getValue();
    switch (item.getInitValueConfig().getInitialValue().getType()) {
      case Literal:
        // File names are always strings
        value =
            typeTable.renderPrimitiveValue(
                ProtoTypeRef.create(TypeRef.fromPrimitiveName("string")), value);
        break;
      case Variable:
        value = namer.localVarReference(Name.anyLower(value));
        break;
      default:
        throw new IllegalArgumentException("Unhandled init value type");
    }
    return surfaceLine
        .identifier(namer.localVarName(item.getIdentifier()))
        .fileName(SimpleInitValueView.newBuilder().initialValue(value).build())
        .isFirstReadFileView(isFirstReadFileView)
        .build();
  }

  private void setInitValueAndComments(
      SimpleInitCodeLineView.Builder surfaceLine,
      MethodContext context,
      InitCodeNode item,
      boolean isFirstItem) {
    SurfaceNamer namer = context.getNamer();
    ImportTypeTable typeTable = context.getTypeTable();
    InitValueConfig initValueConfig = item.getInitValueConfig();
    FieldConfig fieldConfig = item.getFieldConfig();

    // Output variables
    InitValueView initValue;
    String comment = "";

    if (context.getFeatureConfig().useResourceNameFormatOptionInSample(context, fieldConfig)) {
      if (!context.isFlattenedMethodContext()) {
        ResourceNameConfig messageResNameConfig = fieldConfig.getMessageResourceNameConfig();
        if (messageResNameConfig == null
            || messageResNameConfig.getResourceNameType() != ResourceNameType.ANY) {
          // In a non-flattened context, we always use the resource name type set on the message
          // instead of set on the flattened method, unless the resource name type on message
          // is ANY.
          fieldConfig = fieldConfig.getMessageFieldConfig();
        }
      }
      if (item.getType().isRepeated()) {
        initValue =
            RepeatedResourceNameInitValueView.newBuilder()
                .resourceTypeName(
                    namer.getAndSaveElementResourceTypeName(context.getTypeTable(), fieldConfig))
                .build();
      } else {
        SingleResourceNameConfig singleResourceNameConfig;
        switch (fieldConfig.getResourceNameType()) {
          case ANY:
            // TODO(michaelbausor): handle case where there are no other resource names at all...
            singleResourceNameConfig =
                Iterables.get(context.getProductConfig().getSingleResourceNameConfigs(), 0);
            FieldConfig anyResourceNameFieldConfig =
                fieldConfig.withResourceNameConfig(singleResourceNameConfig);
            initValue =
                createResourceNameInitValueView(context, anyResourceNameFieldConfig, item).build();
            break;
          case FIXED:
            throw new UnsupportedOperationException("entity name invalid");
          case ONEOF:
            ResourceNameOneofConfig oneofConfig =
                (ResourceNameOneofConfig) fieldConfig.getResourceNameConfig();
            singleResourceNameConfig = Iterables.get(oneofConfig.getSingleResourceNameConfigs(), 0);
            FieldConfig singleResourceNameFieldConfig =
                fieldConfig.withResourceNameConfig(singleResourceNameConfig);
            ResourceNameInitValueView initView =
                createResourceNameInitValueView(context, singleResourceNameFieldConfig, item)
                    .build();
            initValue =
                ResourceNameOneofInitValueView.newBuilder()
                    .resourceOneofTypeName(
                        namer.getAndSaveElementResourceTypeName(typeTable, fieldConfig))
                    .specificResourceNameView(initView)
                    .build();
            break;
          case SINGLE:
            initValue = createResourceNameInitValueView(context, fieldConfig, item).build();
            break;
          case NONE:
            // Fall-through
          default:
            throw new UnsupportedOperationException(
                "unexpected entity name type '" + fieldConfig.getResourceNameType() + "'");
        }
      }
    } else if (initValueConfig.hasFormattingConfig() && !item.getType().isRepeated()) {
      if (context.getFeatureConfig().enableStringFormatFunctions()
          || fieldConfig.getResourceNameConfig() == null) {
        FormattedInitValueView.Builder formattedInitValue = FormattedInitValueView.newBuilder();

        formattedInitValue.apiWrapperName(
            context.getNamer().getApiWrapperClassName(context.getInterfaceConfig()));
        formattedInitValue.fullyQualifiedApiWrapperName(
            context.getNamer().getFullyQualifiedApiWrapperClassName(context.getInterfaceConfig()));
        formattedInitValue.formatFunctionName(
            context
                .getNamer()
                .getFormatFunctionName(
                    context.getInterfaceConfig(), initValueConfig.getSingleResourceNameConfig()));

        PathTemplate template = initValueConfig.getSingleResourceNameConfig().getNameTemplate();
        String[] encodeArgs = new String[template.vars().size()];
        Arrays.fill(encodeArgs, FORMAT_SPEC_PLACEHOLDER);
        // Format spec usually contains reserved character, escaped by path template.
        // So we first encode using FORMAT_SPEC_PLACEHOLDER, then do straight string replace.
        formattedInitValue.formatSpec(
            template
                .withoutVars()
                .encode(encodeArgs)
                .replace(FORMAT_SPEC_PLACEHOLDER, context.getNamer().formatSpec()));

        List<String> varList =
            Lists.newArrayList(
                initValueConfig.getSingleResourceNameConfig().getNameTemplate().vars());
        formattedInitValue.formatArgs(getFormatFunctionArgs(context, varList, initValueConfig));

        initValue = formattedInitValue.build();
      } else {
        initValue =
            createResourceNameInitValueView(context, fieldConfig, item)
                .convertToString(true)
                .build();
      }
    } else {
      SimpleInitValueView.Builder simpleInitValue = SimpleInitValueView.newBuilder();

      if (initValueConfig.hasSimpleInitialValue()) {
        String value = initValueConfig.getInitialValue().getValue();
        switch (initValueConfig.getInitialValue().getType()) {
          case Literal:
            if (item.getType().isEnum()) {
              value = context.getTypeTable().getEnumValue(item.getType(), value);
            } else {
              value = context.getTypeTable().renderPrimitiveValue(item.getType(), value);
            }
            break;
          case Random:
            value = context.getNamer().injectRandomStringGeneratorCode(value);
            break;
          case Variable:
            value = context.getNamer().localVarReference(Name.anyLower(value));
            break;
          default:
            throw new IllegalArgumentException("Unhandled init value type");
        }
        simpleInitValue.initialValue(value);
      } else {
        simpleInitValue.initialValue(
            context.getTypeTable().getSnippetZeroValueAndSaveNicknameFor(item.getType()));
        simpleInitValue.isRepeated(item.getType().isRepeated());
        if (isRequired(item.getFieldConfig(), context)) {
          String name = getVariableName(context, item);
          comment = String.format(UNINITIALIZED_REQUIRED_FIELD_COMMENT, name);
        }
      }

      initValue = simpleInitValue.build();
    }
    surfaceLine.initValue(initValue);
    surfaceLine.needsLeadingNewline(!isFirstItem);
    if (generateUserFacingComments) {
      surfaceLine.doc(context.getNamer().getDocLines(comment));
    } else {
      surfaceLine.doc(ImmutableList.of());
    }
  }

  private ResourceNameInitValueView.Builder createResourceNameInitValueView(
      MethodContext context, FieldConfig fieldConfig, InitCodeNode item) {
    String resourceName =
        context.getNamer().getAndSaveElementResourceTypeName(context.getTypeTable(), fieldConfig);
    SingleResourceNameConfig singleResourceNameConfig =
        (SingleResourceNameConfig) fieldConfig.getResourceNameConfig();
    List<String> varList = Lists.newArrayList(singleResourceNameConfig.getNameTemplate().vars());

    return ResourceNameInitValueView.newBuilder()
        .resourceTypeName(resourceName)
        .formatArgs(getFormatFunctionArgs(context, varList, item.getInitValueConfig()));
  }

  private static List<String> getFormatFunctionArgs(
      MethodContext context, List<String> varList, InitValueConfig initValueConfig) {
    List<String> formatFunctionArgs = new ArrayList<>();
    for (String entityName : varList) {
      String entityValue =
          context
              .getTypeTable()
              .renderValueAsString("[" + Name.anyLower(entityName).toUpperUnderscore() + "]");

      if (initValueConfig.getResourceNameBindingValues().containsKey(entityName)) {
        InitValue initValue = initValueConfig.getResourceNameBindingValues().get(entityName);
        switch (initValue.getType()) {
          case Variable:
            entityValue = context.getNamer().localVarReference(Name.anyLower(initValue.getValue()));
            break;
          case Random:
            entityValue = context.getNamer().injectRandomStringGeneratorCode(initValue.getValue());
            break;
          case Literal:
            entityValue =
                context
                    .getTypeTable()
                    .renderPrimitiveValue(
                        ProtoTypeRef.create(TypeRef.fromPrimitiveName("string")),
                        initValue.getValue());
            break;
          default:
            throw new IllegalArgumentException("Unhandled init value type");
        }
      }
      formatFunctionArgs.add(entityValue);
    }
    return formatFunctionArgs;
  }

  private List<FieldSettingView> getFieldSettings(
      MethodContext context, Iterable<InitCodeNode> childItems) {
    SurfaceNamer namer = context.getNamer();
    List<FieldSettingView> allSettings = new ArrayList<>();

    for (InitCodeNode item : childItems) {
      FieldSettingView.Builder fieldSetting = FieldSettingView.newBuilder();
      FieldConfig fieldConfig = item.getFieldConfig();

      if (context.getFeatureConfig().useResourceNameProtoAccessor(fieldConfig)) {
        fieldSetting.fieldSetFunction(
            namer.getResourceNameFieldSetFunctionName(fieldConfig.getMessageFieldConfig()));
      } else {
        fieldSetting.fieldSetFunction(
            namer.getFieldSetFunctionName(item.getType(), Name.anyLower(item.getVarName())));
      }
      fieldSetting.fieldAddFunction(
          namer.getFieldAddFunctionName(item.getType(), Name.anyLower(item.getVarName())));
      fieldSetting.fieldGetFunction(
          namer.getFieldGetFunctionName(item.getType(), Name.anyLower(item.getVarName())));

      fieldSetting.identifier(getVariableName(context, item));
      fieldSetting.initCodeLine(
          generateSurfaceInitCodeLine(context, item, allSettings.isEmpty(), false));
      fieldSetting.fieldName(context.getNamer().publicFieldName(Name.anyLower(item.getVarName())));

      fieldSetting.isMap(item.getType().isMap());
      fieldSetting.isArray(!item.getType().isMap() && item.getType().isRepeated());
      fieldSetting.elementTypeName(context.getTypeTable().getFullNameFor(item.getType()));
      if (item.getOneofConfig() != null) {
        fieldSetting.oneofConfig(
            OneofConfigView.newBuilder()
                .groupName(namer.publicFieldName(item.getOneofConfig().groupName()))
                .variantType(namer.getOneofVariantTypeName(item.getOneofConfig()))
                .build());
      }
      fieldSetting.required(isRequired(fieldConfig, context));

      String formatMethodName = "";
      String transformParamFunctionName = "";

      // If resource name converters should only be used in the sample, we need to convert the
      // resource name to a string before passing it or setting it on the next thing
      boolean needsConversion =
          context.getFeatureConfig().useResourceNameConvertersInSampleOnly(context, fieldConfig);
      // If resource name converters should be used and this is not a flattened method context
      // (i.e. it is for setting fields on a proto object), we need to convert the resource name
      // to a string.
      // For flattened method contexts, if the resource names are used in more than just the sample
      // (i.e. in the flattened method signature), then we don't convert (that will be done in the
      // flattened method implementation when setting fields on the proto object).
      if (context.getFeatureConfig().useResourceNameConverters(fieldConfig)
          && !context.isFlattenedMethodContext()) {
        needsConversion = true;
      }
      if (needsConversion) {
        if (fieldConfig.getField().isRepeated()) {
          // TODO (https://github.com/googleapis/toolkit/issues/1806) support repeated one-ofs
          transformParamFunctionName =
              namer.getResourceTypeFormatListMethodName(context.getTypeTable(), fieldConfig);
        } else {
          formatMethodName = namer.getResourceNameFormatMethodName();
        }
      }
      fieldSetting.transformParamFunctionName(transformParamFunctionName);
      fieldSetting.formatMethodName(formatMethodName);
      allSettings.add(fieldSetting.build());
    }
    return allSettings;
  }

  /** Determines whether a field is required */
  private static boolean isRequired(FieldConfig fieldConfig, MethodContext context) {
    return fieldConfig != null
        && context
            .getMethodConfig()
            .getRequiredFieldConfigs()
            .stream()
            .anyMatch(
                fc -> fc.getField().getSimpleName().equals(fieldConfig.getField().getSimpleName()));
  }

  private static String getVariableName(MethodContext context, InitCodeNode item) {
    if (!context
            .getFeatureConfig()
            .useResourceNameFormatOptionInSample(context, item.getFieldConfig())
        && item.getInitValueConfig().hasFormattingConfig()) {
      return context.getNamer().getFormattedVariableName(item.getIdentifier());
    }
    return context.getNamer().localVarName(item.getIdentifier());
  }
}
