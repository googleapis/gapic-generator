/* Copyright 2017 Google LLC
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
package com.google.api.codegen.configgen.transformer;

import com.google.api.codegen.config.FieldModel;
import com.google.api.codegen.config.InterfaceModel;
import com.google.api.codegen.config.MethodModel;
import com.google.api.codegen.configgen.viewmodel.FieldNamePatternView;
import com.google.api.codegen.configgen.viewmodel.FlatteningGroupView;
import com.google.api.codegen.configgen.viewmodel.FlatteningView;
import com.google.api.codegen.configgen.viewmodel.MethodView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingRequestView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingResponseView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingView;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Generates method view objects from an API interface and collection name map. */
public class MethodTransformer {
  private final InputSpecificMethodTransformer helperTransformer;

  // Used for empty config field values that require a non-null value in the yaml.
  public static String TODO_STRING = "_TODO_";

  public MethodTransformer(InputSpecificMethodTransformer helperTransformer) {
    this.helperTransformer = helperTransformer;
  }

  // Do not apply flattening if the parameter count exceeds the threshold.
  // TODO(shinfan): Investigate a more intelligent way to handle this.
  private static final int FLATTENING_THRESHOLD = 4;

  private static final int REQUEST_OBJECT_METHOD_THRESHOLD = 1;

  public List<MethodView> generateMethods(
      InterfaceModel apiInterface, Map<String, String> collectionNameMap) {
    ImmutableList.Builder<MethodView> methods = ImmutableList.builder();
    for (MethodModel method : apiInterface.getMethods()) {
      MethodView.Builder methodView = MethodView.newBuilder();
      methodView.name(method.getRawName());
      generateField(method, methodView);
      generatePageStreaming(method, methodView);
      generateRetry(method, methodView);
      generateFieldNamePatterns(method, methodView, collectionNameMap);
      methodView.timeoutMillis("60000");
      methods.add(methodView.build());
    }
    return methods.build();
  }

  private void generateField(MethodModel method, MethodView.Builder methodView) {
    List<String> parameterList = new ArrayList<>();
    List<FieldModel> fieldList = new ArrayList<>();
    Iterable<? extends FieldModel> inputFields = method.getMaybeRequiredInputFields();
    for (FieldModel field : inputFields) {
      String fieldName = field.getSimpleName();
      if (field.getOneof() == null
          && !helperTransformer.getPagingParameters().getIgnoredParameters().contains(fieldName)) {
        parameterList.add(fieldName);
        fieldList.add(field);
      }
    }

    List<String> parameters = filteredInputFields(method, fieldList);

    if (parameters.size() > 0 && parameters.size() <= FLATTENING_THRESHOLD) {
      methodView.flattening(generateFlattening(parameters));
    }

    methodView.requiredFields(parameters);
    // use all fields for the following check; if there are ignored fields for flattening
    // purposes, the caller still needs a way to set them (by using the request object method).
    methodView.requestObjectMethod(
        (Iterators.size(inputFields.iterator()) > REQUEST_OBJECT_METHOD_THRESHOLD
                || Iterators.size(inputFields.iterator()) != parameterList.size())
            && !method.getRequestStreaming());
    methodView.resourceNameTreatment(helperTransformer.getResourceNameTreatment(method));
  }

  /** Get the filtered input fields for a model, from a list of candidates. */
  private List<String> filteredInputFields(MethodModel method, List<FieldModel> candidates) {
    List<String> parameterNames = new ArrayList<>();
    List<? extends FieldModel> parametersForResourceNameMethod =
        method.getInputFieldsForResourceNameMethod();
    for (FieldModel field : candidates) {
      if (parametersForResourceNameMethod.contains(field)) {
        parameterNames.add(field.getNameAsParameter());
      }
    }
    return parameterNames;
  }

  private FlatteningView generateFlattening(List<String> parameterList) {
    return FlatteningView.newBuilder()
        .groups(
            ImmutableList.of(FlatteningGroupView.newBuilder().parameters(parameterList).build()))
        .build();
  }

  private void generatePageStreaming(MethodModel method, MethodView.Builder methodView) {
    PageStreamingRequestView request = generatePageStreamingRequest(method);
    if (request == null) {
      return;
    }

    PageStreamingResponseView response = helperTransformer.generatePageStreamingResponse(method);
    if (response == null) {
      return;
    }

    methodView.pageStreaming(
        PageStreamingView.newBuilder().request(request).response(response).build());
  }

  private PageStreamingRequestView generatePageStreamingRequest(MethodModel method) {
    PageStreamingRequestView.Builder requestBuilder = PageStreamingRequestView.newBuilder();

    for (FieldModel field : method.getInputFields()) {
      String fieldName = field.getSimpleName();
      if (fieldName.equals(helperTransformer.getPagingParameters().getNameForPageToken())) {
        requestBuilder.tokenField(fieldName);
      } else if (fieldName.equals(helperTransformer.getPagingParameters().getNameForPageSize())) {
        requestBuilder.pageSizeField(fieldName);
      }
    }

    PageStreamingRequestView request = requestBuilder.build();
    return request.tokenField() == null && request.pageSizeField() == null ? null : request;
  }

  private void generateRetry(MethodModel method, MethodView.Builder methodView) {
    methodView.retryCodesName(
        method.isIdempotent()
            ? RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME
            : RetryTransformer.RETRY_CODES_NON_IDEMPOTENT_NAME);
    methodView.retryParamsName(RetryTransformer.RETRY_PARAMS_DEFAULT_NAME);
  }

  private void generateFieldNamePatterns(
      MethodModel method, MethodView.Builder methodView, Map<String, String> nameMap) {
    ImmutableList.Builder<FieldNamePatternView> fieldNamePatterns = ImmutableList.builder();
    Map<String, String> resourcePatternNameMap = method.getResourcePatternNameMap(nameMap);
    for (String resourcePattern : resourcePatternNameMap.keySet()) {
      fieldNamePatterns.add(
          FieldNamePatternView.newBuilder()
              .pathTemplate(resourcePattern)
              .entityName(resourcePatternNameMap.get(resourcePattern))
              .build());
    }

    methodView.fieldNamePatterns(fieldNamePatterns.build());
  }
}
