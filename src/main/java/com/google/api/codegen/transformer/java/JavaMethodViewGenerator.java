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

package com.google.api.codegen.transformer.java;

import com.google.api.codegen.config.FieldConfig;
import com.google.api.codegen.config.FlatteningConfig;
import com.google.api.codegen.config.MethodConfig;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.config.SampleSpec.SampleType;
import com.google.api.codegen.transformer.ImportTypeTable;
import com.google.api.codegen.transformer.InterfaceContext;
import com.google.api.codegen.transformer.MethodContext;
import com.google.api.codegen.transformer.StaticLangApiMethodTransformer;
import com.google.api.codegen.viewmodel.StaticLangApiMethodView;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Contains the common logic for generating view models for GAPIC surface methods. This is used in
 * the generation of both the client libraries and of the standalone samples for each of the library
 * surface methods. Since in either case the resulting methods have one or more samples, this
 * populates the view models with appropriate sample view models.
 */
public class JavaMethodViewGenerator {

  final StaticLangApiMethodTransformer clientMethodTransformer;

  public JavaMethodViewGenerator(SampleType sampleType) {
    clientMethodTransformer = new StaticLangApiMethodTransformer(sampleType);
  }

  /**
   * Generates the StaticLangApiMethodView for each of the methods in the InterfaceContext.
   *
   * @param context The context containing the methods for which to generate StaticLangApiMethodView
   * @return The list of StaticLangApiMethodView
   */
  public List<StaticLangApiMethodView> generateApiMethods(InterfaceContext context) {
    return context
        .getSupportedMethods()
        .stream()
        .flatMap(method -> generateApiMethod(context, method).stream())
        .collect(Collectors.toList());
  }

  /** Generates the StaticLangApiMethodView for the given method. */
  public List<StaticLangApiMethodView> generateApiMethod(
      InterfaceContext context, MethodModel method) {
    List<StaticLangApiMethodView> apiMethods = new ArrayList<>();

    MethodConfig methodConfig = context.getMethodConfig(method);
    MethodContext requestMethodContext = context.asRequestMethodContext(method);

    if (methodConfig.isPageStreaming()) {
      if (methodConfig.isFlattening()) {
        for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
          MethodContext flattenedMethodContext =
              context.asFlattenedMethodContext(method, flatteningGroup);
          apiMethods.add(
              clientMethodTransformer.generatePagedFlattenedMethod(flattenedMethodContext));
          if (hasAnyResourceNameParameter(flatteningGroup)) {
            apiMethods.add(
                clientMethodTransformer.generatePagedFlattenedMethod(
                    flattenedMethodContext.withResourceNamesInSamplesOnly()));
          }
        }
      }
      apiMethods.add(
          clientMethodTransformer.generatePagedRequestObjectMethod(requestMethodContext));
      apiMethods.add(clientMethodTransformer.generatePagedCallableMethod(requestMethodContext));

      apiMethods.add(
          clientMethodTransformer.generateUnpagedListCallableMethod(requestMethodContext));
    } else if (methodConfig.isGrpcStreaming()) {
      ImportTypeTable typeTable = context.getImportTypeTable();
      switch (methodConfig.getGrpcStreamingType()) {
        case BidiStreaming:
          typeTable.saveNicknameFor("com.google.api.gax.rpc.BidiStreamingCallable");
          break;
        case ClientStreaming:
          typeTable.saveNicknameFor("com.google.api.gax.rpc.ClientStreamingCallable");
          break;
        case ServerStreaming:
          typeTable.saveNicknameFor("com.google.api.gax.rpc.ServerStreamingCallable");
          break;
        default:
          throw new IllegalArgumentException(
              "Invalid streaming type: " + methodConfig.getGrpcStreamingType());
      }
      apiMethods.add(clientMethodTransformer.generateCallableMethod(requestMethodContext));
    } else if (methodConfig.isLongRunningOperation()) {
      context.getImportTypeTable().saveNicknameFor("com.google.api.gax.rpc.OperationCallable");
      if (methodConfig.isFlattening()) {
        for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
          MethodContext flattenedMethodContext =
              context.asFlattenedMethodContext(method, flatteningGroup);
          apiMethods.add(
              clientMethodTransformer.generateAsyncOperationFlattenedMethod(
                  flattenedMethodContext));
          if (hasAnyResourceNameParameter(flatteningGroup)) {
            apiMethods.add(
                clientMethodTransformer.generateAsyncOperationFlattenedMethod(
                    flattenedMethodContext.withResourceNamesInSamplesOnly()));
          }
        }
      }
      apiMethods.add(
          clientMethodTransformer.generateAsyncOperationRequestObjectMethod(requestMethodContext));
      apiMethods.add(clientMethodTransformer.generateOperationCallableMethod(requestMethodContext));
      apiMethods.add(clientMethodTransformer.generateCallableMethod(requestMethodContext));
    } else {
      if (methodConfig.isFlattening()) {
        for (FlatteningConfig flatteningGroup : methodConfig.getFlatteningConfigs()) {
          MethodContext flattenedMethodContext =
              context.asFlattenedMethodContext(method, flatteningGroup);
          apiMethods.add(clientMethodTransformer.generateFlattenedMethod(flattenedMethodContext));

          if (hasAnyResourceNameParameter(flatteningGroup)) {
            apiMethods.add(
                clientMethodTransformer.generateFlattenedMethod(
                    flattenedMethodContext.withResourceNamesInSamplesOnly()));
          }
        }
      }
      apiMethods.add(clientMethodTransformer.generateRequestObjectMethod(requestMethodContext));

      apiMethods.add(clientMethodTransformer.generateCallableMethod(requestMethodContext));
    }

    return apiMethods;
  }

  private boolean hasAnyResourceNameParameter(FlatteningConfig flatteningGroup) {
    return flatteningGroup
        .getFlattenedFieldConfigs()
        .values()
        .stream()
        .anyMatch(FieldConfig::useResourceNameType);
  }
}
