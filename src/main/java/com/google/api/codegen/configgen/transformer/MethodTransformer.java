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

import com.google.api.codegen.configgen.CollectionPattern;
import com.google.api.codegen.configgen.viewmodel.FieldNamePatternView;
import com.google.api.codegen.configgen.viewmodel.FlatteningGroupView;
import com.google.api.codegen.configgen.viewmodel.FlatteningView;
import com.google.api.codegen.configgen.viewmodel.MethodView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingRequestView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingResponseView;
import com.google.api.codegen.configgen.viewmodel.PageStreamingView;
import com.google.api.tools.framework.aspects.http.model.HttpAttribute;
import com.google.api.tools.framework.aspects.http.model.MethodKind;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.Interface;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.common.collect.ImmutableList;
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
      Interface apiInterface, Map<String, String> collectionNameMap) {
    ImmutableList.Builder<MethodView> methods = ImmutableList.builder();
    for (Method method : apiInterface.getMethods()) {
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

  private void generateField(Method method, MethodView.Builder methodView) {
    List<String> parameterList = new ArrayList<>();
    MessageType message = method.getInputMessage();
    for (Field field : message.getFields()) {
      String fieldName = field.getSimpleName();
      if (!IGNORED_FIELDS.contains(fieldName)) {
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
        (message.getFields().size() > REQUEST_OBJECT_METHOD_THRESHOLD
                || message.getFields().size() != parameterList.size())
            && !method.getRequestStreaming());
  }

  private FlatteningView generateFlattening(List<String> parameterList) {
    return FlatteningView.newBuilder()
        .groups(
            ImmutableList.of(FlatteningGroupView.newBuilder().parameters(parameterList).build()))
        .build();
  }

  private void generatePageStreaming(Method method, MethodView.Builder methodView) {
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

  private PageStreamingRequestView generatePageStreamingRequest(Method method) {
    PageStreamingRequestView.Builder requestBuilder = PageStreamingRequestView.newBuilder();

    for (Field field : method.getInputMessage().getFields()) {
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

  private PageStreamingResponseView generatePageStreamingResponse(Method method) {
    boolean hasTokenField = false;
    String resourcesField = null;
    for (Field field : method.getOutputMessage().getFields()) {
      String fieldName = field.getSimpleName();
      if (fieldName.equals(PARAMETER_NEXT_PAGE_TOKEN)) {
        hasTokenField = true;
      } else if (field.getType().isRepeated()) {
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

  private void generateRetry(Method method, MethodView.Builder methodView) {
    methodView.retryCodesName(
        isIdempotent(method)
            ? RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME
            : RetryTransformer.RETRY_CODES_NON_IDEMPOTENT_NAME);
    methodView.retryParamsName(RetryTransformer.RETRY_PARAMS_DEFAULT_NAME);
  }

  /**
   * Returns true if the method is idempotent according to the http method kind (GET, PUT, DELETE).
   */
  private boolean isIdempotent(Method method) {
    HttpAttribute httpAttr = method.getAttribute(HttpAttribute.KEY);
    if (httpAttr == null) {
      return false;
    }
    MethodKind methodKind = httpAttr.getMethodKind();
    return methodKind.isIdempotent();
  }

  private void generateFieldNamePatterns(
      Method method, MethodView.Builder methodView, Map<String, String> nameMap) {
    ImmutableList.Builder<FieldNamePatternView> fieldNamePatterns = ImmutableList.builder();
    for (CollectionPattern collectionPattern :
        CollectionPattern.getCollectionPatternsFromMethod(method)) {
      String resourceNameString = collectionPattern.getTemplatizedResourcePath();
      fieldNamePatterns.add(
          FieldNamePatternView.newBuilder()
              .pathTemplate(collectionPattern.getFieldPath())
              .entityName(nameMap.get(resourceNameString))
              .build());
    }

    methodView.fieldNamePatterns(fieldNamePatterns.build());
  }
}
