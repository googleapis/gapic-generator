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
package com.google.api.codegen.util;

import static com.google.api.codegen.configgen.mergers.RetryMerger.DEFAULT_RETRY_CODES;
import static com.google.api.codegen.configgen.transformer.RetryTransformer.RETRY_CODES_IDEMPOTENT_NAME;

import com.google.api.AnnotationsProto;
import com.google.api.HttpRule;
import com.google.api.MethodSignature;
import com.google.api.Resource;
import com.google.api.Retry;
import com.google.api.codegen.config.ProtoMethodModel;
import com.google.api.tools.framework.model.Field;
import com.google.api.tools.framework.model.MessageType;
import com.google.api.tools.framework.model.Method;
import com.google.api.tools.framework.model.ProtoElement;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.rpc.Code;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

// Utils for parsing proto annotations.
public class ProtoAnnotations {
  public static final String RESOURCE_ANNOTATION = "google.api.resources";

  /** Return the path, e.g. "shelves/*" for a resource field. Return null if no path found. */
  public static String getResourcePath(ProtoElement element) {
    return element
        .getOptionFields()
        .entrySet()
        .stream()
        .filter(entry -> entry.getKey().getFullName().equals(RESOURCE_ANNOTATION))
        .map(entry -> ((Resource) entry.getValue()).getPath())
        .findFirst()
        .orElse(null);
  }

  /** Return the entity name, e.g. "shelf" for a resource field. */
  public static String getResourceEntityName(Field field) {
    return field.getSimpleName().toLowerCase();
  }

  /* Return a list of method signatures, aka flattenings, specified on a given method. */
  public static List<MethodSignature> getMethodSignatures(ProtoMethodModel method) {
    MethodSignature methodSignature =
        method
            .getProtoMethod()
            .getDescriptor()
            .getMethodAnnotation(AnnotationsProto.methodSignature);
    // Let's only recurse once when we look for additional MethodSignatures.
    List<MethodSignature> additionalSignatures = methodSignature.getAdditionalSignaturesList();
    return ImmutableList.<MethodSignature>builder()
        .add(methodSignature)
        .addAll(additionalSignatures)
        .build();
  }

  public static List<String> getRetryCodes(Method method) {
    Retry retry = method.getDescriptor().getMethodAnnotation(AnnotationsProto.retry);
    HttpRule httpRule = method.getDescriptor().getMethodAnnotation(AnnotationsProto.http);

    // If this is analogous to HTTP GET, then automatically retry on `INTERNAL` and `UNAVAILABLE`.
    if (retry.getCodesCount() == 0 && !Strings.isNullOrEmpty(httpRule.getGet())) {
      return DEFAULT_RETRY_CODES.get(RETRY_CODES_IDEMPOTENT_NAME);
    }

    // If there is no google.api.http annotation, do not retry any errors.
    if (httpRule == null || httpRule.equals(httpRule.getDefaultInstanceForType())) {
      return ImmutableList.of();
    }

    return retry.getCodesList().stream().map(Code::name).collect(Collectors.toList());
  }

  public static List<String> getRequiredFields(Method method) {
    MessageType inputMessage = method.getInputMessage();
    return inputMessage
        .getFields()
        .stream()
        .filter(ProtoAnnotations::isFieldRequired)
        .map(Field::getSimpleName)
        .collect(Collectors.toList());
  }

  /** Returns if a field is required, according to the proto annotations. */
  public static boolean isFieldRequired(Field field) {
    return Optional.ofNullable(
            (Boolean) field.getOptionFields().get(AnnotationsProto.required.getDescriptor()))
        .orElse(false);
  }
}
