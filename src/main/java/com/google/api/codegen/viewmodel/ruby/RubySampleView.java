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
package com.google.api.codegen.viewmodel.ruby;

import com.google.api.codegen.SampleValueSet;
import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.SingleResourceNameConfig;
import com.google.api.codegen.metacode.InitCodeContext;
import com.google.api.codegen.metacode.InitCodeNode;
import com.google.api.codegen.metacode.InitValueConfig;
import com.google.api.codegen.transformer.GapicMethodContext;
import com.google.api.codegen.util.Name;
import com.google.api.codegen.viewmodel.ClientMethodType;
import com.google.api.codegen.viewmodel.DynamicLangMethodTypeView;
import com.google.api.codegen.viewmodel.GrpcStreamingView;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class RubySampleView {

  private final GapicMethodContext context;

  private final Collection<InitCodeNode> orderedItems;

  private final Collection<InitCodeNode> argItems;

  private final String methodName;

  public static RubySampleView create(GapicMethodContext context, String methodName) {
    MethodConfig methodConfig = context.getMethodConfig();
    boolean hasRequestStreaming =
        new GrpcStreamingView(methodConfig.getGrpcStreamingType()).isRequestStreaming();
    Iterable<FieldConfig> fieldConfigs = methodConfig.getRequiredFieldConfigs();
    SampleValueSet valueSet =
        SampleValueSet.newBuilder()
            .addAllParameters(methodConfig.getSampleCodeInitFields())
            .setId("[ sample_code_init_fields ]") // only use these samples for initCode
            .setDescription("value set imported from sample_code_init_fields")
            .setTitle("Sample Values")
            .build();
    InitCodeContext initCodeContext =
        InitCodeContext.newBuilder()
            .initObjectType(methodConfig.getMethodModel().getInputType())
            .suggestedName(Name.from("request"))
            .initFieldConfigStrings(valueSet.getParametersList())
            .initValueConfigMap(createCollectionMap(context))
            .initFields(FieldConfig.toFieldTypeIterable(fieldConfigs))
            .outputType(
                hasRequestStreaming
                    ? InitCodeContext.InitCodeOutputType.SingleObject
                    : InitCodeContext.InitCodeOutputType.FieldList)
            .fieldConfigMap(FieldConfig.toFieldConfigMap(fieldConfigs))
            .build();
    InitCodeNode rootNode = InitCodeNode.createTree(initCodeContext);
    List<InitCodeNode> orderedItems = rootNode.listInInitializationOrder();
    if (hasRequestStreaming) {
      return new RubySampleView(context, orderedItems, ImmutableList.of(rootNode), methodName);
    }

    Collection<InitCodeNode> argItems = rootNode.getChildren().values();
    //Remove the request object for flattened method
    return new RubySampleView(
        context, orderedItems.subList(0, orderedItems.size() - 1), argItems, methodName);
  }

  /**
   * A utility method which creates the InitValueConfig map that contains the collection config
   * data.
   */
  private static ImmutableMap<String, InitValueConfig> createCollectionMap(
      GapicMethodContext context) {
    String clientName = context.getInterfaceConfig().getName() + "Client";
    Map<String, String> fieldNamePatterns = context.getMethodConfig().getFieldNamePatterns();
    ImmutableMap.Builder<String, InitValueConfig> collections = ImmutableMap.builder();
    for (Map.Entry<String, String> fieldNamePattern : fieldNamePatterns.entrySet()) {
      SingleResourceNameConfig resourceNameConfig =
          context.getSingleResourceNameConfig(fieldNamePattern.getValue());
      InitValueConfig initValueConfig = InitValueConfig.create(clientName, resourceNameConfig);
      collections.put(fieldNamePattern.getKey(), initValueConfig);
    }
    return collections.build();
  }

  private RubySampleView(
      GapicMethodContext context,
      Collection<InitCodeNode> orderedItems,
      Collection<InitCodeNode> argItems,
      String methodName) {
    this.context = context;
    this.orderedItems = orderedItems;
    this.argItems = argItems;
    this.methodName = methodName;
  }

  public String importFilename() {
    ImmutableList.Builder<String> paths = ImmutableList.builder();
    for (String path : Splitter.on("::").split(getPackageName())) {
      paths.add(Name.upperCamel(path).toLowerUnderscore());
    }

    return Joiner.on("/").join(paths.build());
  }

  public String clientName() {
    return getInterfaceModel().getApiModel().hasMultipleServices()
        ? String.format("%s::%s", getPackageName(), getReducedClientName())
        : getPackageName();
  }

  public String clientVariable() {
    return getClientNameAsName().toLowerUnderscore();
  }

  public String methodName() {
    return methodName;
  }

  public ClientMethodType methodType() {
    return new DynamicLangMethodTypeView(context.getMethodConfig()).type();
  }

  public GrpcStreamingView grpcStreaming() {
    return new GrpcStreamingView(context.getMethodConfig().getGrpcStreamingType());
  }

  public Iterable<RubyInitLineView> lines() {
    String clientName =
        String.format("%s::%s", getPackageName(), getClientNameAsName().toUpperCamel());
    ImmutableList.Builder<RubyInitLineView> lines = ImmutableList.builder();
    for (InitCodeNode item : orderedItems) {
      lines.add(
          isRequired(item.getFieldConfig())
              ? RubyInitLineView.createRequired(item, clientName)
              : RubyInitLineView.createOptional(item, clientName));
    }

    return lines.build();
  }

  public Iterable<RubySampleFieldView> fields() {
    ImmutableList.Builder<RubySampleFieldView> fields = ImmutableList.builder();
    for (InitCodeNode item : argItems) {
      fields.add(
          isRequired(item.getFieldConfig())
              ? RubySampleFieldView.createRequired(item)
              : RubySampleFieldView.createOptional(item));
    }

    return fields.build();
  }

  public boolean hasEmptyReturn() {
    return context.getMethodModel().isOutputTypeEmpty();
  }

  private boolean isRequired(FieldConfig fieldConfig) {
    if (fieldConfig == null) {
      return false;
    }

    String fieldName = fieldConfig.getField().getSimpleName();
    return Iterables.any(
        context.getMethodConfig().getRequiredFields(),
        field -> field.getSimpleName().equals(fieldName));
  }

  private Name getClientNameAsName() {
    return Name.upperCamel(getInterfaceModel().getSimpleName(), "Client");
  }

  private String getReducedClientName() {
    return getInterfaceModel().getSimpleName().replaceAll("(Service)?(V[0-9]+)?$", "");
  }

  private String getPackageName() {
    return context.getProductConfig().getPackageName();
  }

  private InterfaceModel getInterfaceModel() {
    return context.getInterfaceConfig().getInterfaceModel();
  }
}
