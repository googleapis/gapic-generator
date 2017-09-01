/* Copyright 2017 Google Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
  private static final String PARAMETER_PAGE_TOKEN = "page_token";
  private static final String PARAMETER_PAGE_SIZE = "page_size";
  private static final String PARAMETER_NEXT_PAGE_TOKEN = "next_page_token";
  private static final ImmutableList<String> IGNORED_FIELDS =
      ImmutableList.of(PARAMETER_PAGE_TOKEN, PARAMETER_PAGE_SIZE);

  // Do not apply flattening if the parameter count exceeds the threshold.
  // TODO(shinfan): Investigate a more intelligent way to handle this.
  private static final int FLATTENING_THRESHOLD = 4;

  private static final int REQUEST_OBJECT_METHOD_THRESHOLD = 1;

  public List<MethodView> generateMethods(
      InterfaceModel apiInterface, Map<String, String> collectionNameMap) {
    ImmutableList.Builder<MethodView> methods = ImmutableList.builder();
    for (MethodModel method : apiInterface.getMethods()) {
      MethodView.Builder methodView = MethodView.newBuilder();
      methodView.name(method.getSimpleName());
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
    Iterable<FieldModel> inputFields = method.getInputFields();
    for (FieldModel field : inputFields) {
      String fieldName = field.getSimpleName();
      if (field.getOneof() == null && !IGNORED_FIELDS.contains(fieldName)) {
        parameterList.add(fieldName);
      }
    }

    if (parameterList.size() > 0 && parameterList.size() <= FLATTENING_THRESHOLD) {
      methodView.flattening(generateFlattening(parameterList));
    }

    methodView.requiredFields(parameterList);
    // use all fields for the following check; if there are ignored fields for flattening
    // purposes, the caller still needs a way to set them (by using the request object method).
    methodView.requestObjectMethod(
        (Iterators.size(inputFields.iterator()) > REQUEST_OBJECT_METHOD_THRESHOLD
                || Iterators.size(inputFields.iterator()) != parameterList.size())
            && !method.getRequestStreaming());
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

    PageStreamingResponseView response = generatePageStreamingResponse(method);
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
      if (fieldName.equals(PARAMETER_PAGE_TOKEN)) {
        requestBuilder.tokenField(fieldName);
      } else if (fieldName.equals(PARAMETER_PAGE_SIZE)) {
        requestBuilder.pageSizeField(fieldName);
      }
    }

    PageStreamingRequestView request = requestBuilder.build();
    return request.tokenField() == null && request.pageSizeField() == null ? null : request;
  }

  private PageStreamingResponseView generatePageStreamingResponse(MethodModel method) {
    boolean hasTokenField = false;
    String resourcesField = null;
    for (FieldModel field : method.getOutputFields()) {
      String fieldName = field.getSimpleName();
      if (fieldName.equals(PARAMETER_NEXT_PAGE_TOKEN)) {
        hasTokenField = true;
      } else if (field.isRepeated()) {
        if (resourcesField == null) {
          resourcesField = fieldName;
        } else {
          // TODO(shinfan): Add a warning system that is used when heuristic decision cannot be made.
          System.err.printf(
              "Warning: Page Streaming resource field could not be heuristically"
                  + " determined for method %s\n",
              method.getSimpleName());
          break;
        }
      }
    }

    if (!hasTokenField || resourcesField == null) {
      return null;
    }

    return PageStreamingResponseView.newBuilder()
        .tokenField(PARAMETER_NEXT_PAGE_TOKEN)
        .resourcesField(resourcesField)
        .build();
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
